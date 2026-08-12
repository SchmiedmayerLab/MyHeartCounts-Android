//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.build.logic.convention.plugins

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import edu.stanford.spezi.build.logic.convention.model.PluginId
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.process.ExecOperations
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

/**
 * Packages the study bundle exported from the `MyHeartCounts-StudyDefinitions` submodule as application assets.
 *
 * Registers an [MHCExportStudyBundle] per variant, wired into that variant's asset sources, plus an
 * aggregate `exportStudyBundle` task for refreshing the assets before development.
 */
class MHCStudyBundleConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val bundleName = gradleProperty(BUNDLE_NAME_PROPERTY)
        val packageDirectory = rootProject.layout.projectDirectory.dir(gradleProperty(PACKAGE_PATH_PROPERTY))

        // Applied independently of where this sits among the module's plugins.
        pluginManager.withPlugin(PluginId.ANDROID_APPLICATION.id) {
            tasks.register("exportStudyBundle") {
                group = BUILD_SETUP_GROUP
                description = "Exports the study bundle into the assets of every application variant."
                dependsOn(tasks.withType<MHCExportStudyBundle>())
            }

            extensions.configure<ApplicationAndroidComponentsExtension> {
                onVariants { variant ->
                    val export = tasks.register<MHCExportStudyBundle>(
                        "export${variant.name.replaceFirstChar(Char::titlecase)}StudyBundle"
                    ) {
                        group = BUILD_SETUP_GROUP
                        description = "Exports the study bundle into the ${variant.name} assets."
                        this.bundleName.set(bundleName)
                        this.packageDirectory.set(packageDirectory)
                        rootDirectory.set(rootProject.layout.projectDirectory)
                        exporterSources.from(
                            packageDirectory.file("Package.swift"),
                            packageDirectory.dir("Sources")
                        )
                        toolchain.set(
                            providers.gradleProperty(TOOLCHAIN_PROPERTY)
                                .orElse(providers.gradleProperty(TOOLCHAIN_DEFAULT_PROPERTY))
                        )
                        swiftImage.set(providers.gradleProperty(SWIFT_IMAGE_PROPERTY))
                        scratchDirectory.set(layout.buildDirectory.dir("studyBundle/${variant.name}"))
                    }
                    variant.sources.assets?.addGeneratedSourceDirectory(
                        export,
                        MHCExportStudyBundle::outputDirectory
                    )
                    variant.buildConfigFields?.put(
                        ASSET_PATH_FIELD,
                        provider { BuildConfigField("String", "\"$bundleName\"", ASSET_PATH_DOC) }
                    )
                }
            }
        }
    }

    private fun Project.gradleProperty(name: String): String =
        requireNotNull(providers.gradleProperty(name).orNull) { "Missing gradle property: $name" }

    private companion object {
        const val BUILD_SETUP_GROUP = "build setup"
        const val BUNDLE_NAME_PROPERTY = "myHeartCounts.studyBundle.name"
        const val PACKAGE_PATH_PROPERTY = "myHeartCounts.studyBundle.packagePath"
        const val SWIFT_IMAGE_PROPERTY = "myHeartCounts.studyBundle.swiftImage"
        const val TOOLCHAIN_DEFAULT_PROPERTY = "myHeartCounts.studyBundle.toolchain"
        const val TOOLCHAIN_PROPERTY = "studyBundleToolchain"
        const val ASSET_PATH_FIELD = "STUDY_BUNDLE_ASSET_PATH"
        const val ASSET_PATH_DOC = "Path of the study bundle within the app's assets."
    }
}

/**
 * Exports the study bundle from the `MyHeartCounts-StudyDefinitions` submodule into a directory the
 * app packages as assets.
 *
 * The bundle is produced by the same Swift exporter the iOS app ships, so both platforms package the
 * bundle the study definitions describe at the commit the submodule pins — nothing generated from
 * them is committed here.
 *
 * The exporter runs on a Swift toolchain found on `PATH`, or in a container built from
 * [swiftImage] when there is none, which is what lets a machine without Xcode and a Linux CI runner
 * export the same bundle. Select one explicitly with `-PstudyBundleToolchain=swift|docker`.
 */
@CacheableTask
abstract class MHCExportStudyBundle : DefaultTask() {

    /**
     * The sources the exported bundle is a function of: the package manifest and everything it builds.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val exporterSources: ConfigurableFileCollection

    /**
     * Name of the bundle the exporter writes, used to verify the export landed where the app expects it.
     */
    @get:Input
    abstract val bundleName: Property<String>

    /**
     * Container image supplying the Swift toolchain when the export does not run on the host.
     */
    @get:Input
    abstract val swiftImage: Property<String>

    /**
     * Which toolchain to export with: `auto`, `swift` or `docker`.
     */
    @get:Internal
    abstract val toolchain: Property<String>

    @get:Internal
    abstract val packageDirectory: DirectoryProperty

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Internal
    abstract val scratchDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun export() {
        val packageDirectory = packageDirectory.get().asFile
        checkOutSubmodule(packageDirectory)

        val outputDirectory = outputDirectory.get().asFile
        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()
        val scratchDirectory = scratchDirectory.get().asFile.also { it.mkdirs() }

        when (val toolchain = resolveToolchain()) {
            Toolchain.SWIFT -> exportWithSwift(packageDirectory, scratchDirectory, outputDirectory)
            Toolchain.DOCKER -> exportWithDocker(packageDirectory, scratchDirectory, outputDirectory)
            else -> error("Unhandled toolchain: $toolchain")
        }

        check(outputDirectory.resolve(bundleName.get()).isDirectory) {
            "The exporter did not write '${bundleName.get()}' into $outputDirectory. " +
                "Either the submodule renamed the bundle or $BUNDLE_NAME_PROPERTY is stale."
        }
    }

    /**
     * Populates the submodule when the checkout left it empty, so a fresh clone and a CI runner that
     * checked out without submodules both export from the pinned commit rather than failing.
     */
    private fun checkOutSubmodule(packageDirectory: File) {
        if (packageDirectory.resolve("Package.swift").isFile) {
            return
        }
        logger.lifecycle("Checking out the study definitions submodule at $packageDirectory")
        execOperations.exec {
            commandLine("git", "submodule", "update", "--init", "--", packageDirectory.absolutePath)
            workingDir = rootDirectory.get().asFile
        }
        check(packageDirectory.resolve("Package.swift").isFile) {
            "No Swift package at $packageDirectory. Run 'git submodule update --init' and try again."
        }
    }

    private fun exportWithSwift(packageDirectory: File, scratchDirectory: File, outputDirectory: File) {
        execOperations.exec {
            commandLine(
                "swift", "run",
                "--package-path", packageDirectory.absolutePath,
                "--scratch-path", scratchDirectory.absolutePath,
                EXPORTER_PRODUCT, "export", "--format", "package", outputDirectory.absolutePath
            )
        }
    }

    private fun exportWithDocker(packageDirectory: File, scratchDirectory: File, outputDirectory: File) {
        logger.lifecycle("Exporting the study bundle in ${swiftImage.get()}")
        execOperations.exec {
            commandLine(
                buildList {
                    add("docker")
                    add("run")
                    add("--rm")
                    // Bind mounts inherit the container's user, so claim them for whoever runs the build.
                    posixOwner(outputDirectory)?.let { owner ->
                        add("--user")
                        add(owner)
                    }
                    add("--env")
                    add("HOME=/tmp")
                    addAll(mount(packageDirectory, CONTAINER_PACKAGE_PATH))
                    addAll(mount(scratchDirectory, CONTAINER_SCRATCH_PATH))
                    addAll(mount(outputDirectory, CONTAINER_OUTPUT_PATH))
                    add(swiftImage.get())
                    addAll(
                        listOf(
                            "swift", "run",
                            "--package-path", CONTAINER_PACKAGE_PATH,
                            "--scratch-path", CONTAINER_SCRATCH_PATH,
                            EXPORTER_PRODUCT, "export", "--format", "package", CONTAINER_OUTPUT_PATH
                        )
                    )
                }
            )
        }
    }

    private fun resolveToolchain(): Toolchain {
        val requested = toolchain.getOrElse(Toolchain.AUTO.name.lowercase())
        return when (requested.lowercase()) {
            Toolchain.SWIFT.name.lowercase() -> Toolchain.SWIFT
            Toolchain.DOCKER.name.lowercase() -> Toolchain.DOCKER
            Toolchain.AUTO.name.lowercase() -> if (isOnPath("swift")) Toolchain.SWIFT else Toolchain.DOCKER
            else -> error(
                "Unknown toolchain '$requested'. Choose one of: " +
                    Toolchain.entries.joinToString { it.name.lowercase() }
            )
        }
    }

    private fun isOnPath(executable: String) = System.getenv("PATH")
        .orEmpty()
        .splitToSequence(File.pathSeparatorChar)
        .any { File(it, executable).canExecute() }

    private fun mount(directory: File, containerPath: String) =
        listOf("--volume", "${directory.absolutePath}:$containerPath")

    private fun posixOwner(directory: File): String? = runCatching {
        val attributes = Files.readAttributes(directory.toPath(), "unix:uid,unix:gid")
        "${attributes["uid"]}:${attributes["gid"]}"
    }.getOrNull()

    private enum class Toolchain { AUTO, SWIFT, DOCKER }

    private companion object {
        const val EXPORTER_PRODUCT = "MHCStudyDefinitionExporterCLI"
        const val CONTAINER_PACKAGE_PATH = "/package"
        const val CONTAINER_SCRATCH_PATH = "/scratch"
        const val CONTAINER_OUTPUT_PATH = "/output"
        const val BUNDLE_NAME_PROPERTY = "myHeartCounts.studyBundle.name"
    }
}
