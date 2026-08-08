//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.build.logic.convention.plugins

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import edu.stanford.spezi.build.logic.convention.model.PluginId
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * Packages the pinned study bundle as application assets.
 *
 * Registers a [MHCFetchStudyBundle] per variant, wired into that variant's asset sources, and makes
 * the test tasks depend on them so a test can read the unpacked bundle.
 */
class MHCStudyBundleConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val bundleName = gradleProperty(BUNDLE_NAME_PROPERTY)
        val pinFile = rootProject.layout.projectDirectory.file(gradleProperty(PIN_FILE_PROPERTY))

        // Applied independently of where this sits among the module's plugins.
        pluginManager.withPlugin(PluginId.ANDROID_APPLICATION.id) {
            extensions.configure<ApplicationAndroidComponentsExtension> {
                onVariants { variant ->
                    val fetch = tasks.register<MHCFetchStudyBundle>(
                        "fetch${variant.name.replaceFirstChar(Char::titlecase)}StudyBundle"
                    ) {
                        group = "build setup"
                        description =
                            "Unpacks the pinned study bundle into the ${variant.name} assets."
                        pin.set(pinFile)
                        localArchive.set(
                            providers.gradleProperty(MHCFetchStudyBundle.LOCAL_ARCHIVE_PROPERTY)
                        )
                    }
                    variant.sources.assets?.addGeneratedSourceDirectory(
                        fetch,
                        MHCFetchStudyBundle::outputDirectory
                    )
                    variant.buildConfigFields?.put(
                        ASSET_PATH_FIELD,
                        provider { BuildConfigField("String", "\"$bundleName\"", ASSET_PATH_DOC) }
                    )
                }
            }

            tasks.withType<Test>().configureEach {
                dependsOn(tasks.withType<MHCFetchStudyBundle>())
            }
        }
    }

    private fun Project.gradleProperty(name: String): String =
        requireNotNull(providers.gradleProperty(name).orNull) { "Missing gradle property: $name" }

    private companion object {
        const val BUNDLE_NAME_PROPERTY = "myHeartCounts.studyBundle.name"
        const val PIN_FILE_PROPERTY = "myHeartCounts.studyBundle.pinFile"
        const val ASSET_PATH_FIELD = "STUDY_BUNDLE_ASSET_PATH"
        const val ASSET_PATH_DOC = "Path of the study bundle within the app's assets."
    }
}

/**
 * Unpacks the study bundle named by a pin file into a directory the app packages as assets.
 *
 * The archive is downloaded from the URL the pin names and rejected unless it hashes to the pinned
 * digest. Setting [localArchive] uses an archive already on disk instead, which is what makes an
 * offline build possible; the digest is verified either way.
 */
@DisableCachingByDefault(because = "Unpacking a pinned archive is cheaper than a build cache round trip")
abstract class MHCFetchStudyBundle : DefaultTask() {

    /**
     * The pin naming the archive to unpack: a `bundle` object holding a `url` and a `sha256`.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val pin: RegularFileProperty

    /**
     * Path to an archive to use instead of downloading the pinned one.
     */
    @get:Input
    @get:Optional
    abstract val localArchive: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun fetch() {
        val descriptor = JsonSlurper().parse(pin.get().asFile)
        val bundle = requireNotNull((descriptor as? Map<*, *>)?.get(BUNDLE_KEY) as? Map<*, *>) {
            "Malformed study bundle pin: ${pin.get().asFile}"
        }
        val expectedDigest = bundle.required(DIGEST_KEY).lowercase()

        val archive = localArchive.orNull?.let(::File) ?: download(bundle.required(URL_KEY))
        val digest = archive.sha256()
        check(digest == expectedDigest) {
            "Study bundle digest mismatch for ${archive.name}.\n" +
                "  expected: $expectedDigest\n" +
                "  actual:   $digest\n" +
                "Either the pin in ${pin.get().asFile.name} is stale or the archive was tampered with."
        }
        unzip(archive, outputDirectory.get().asFile)
    }

    private fun Map<*, *>.required(key: String): String =
        requireNotNull(this[key] as? String) { "Study bundle pin is missing '$BUNDLE_KEY.$key'" }

    private fun download(url: String): File {
        val destination = File(temporaryDir, "study-bundle.zip")
        logger.lifecycle("Downloading study bundle from $url")
        runCatching {
            URI(url).toURL().openStream().use { source ->
                destination.outputStream().use(source::copyTo)
            }
        }.onFailure { failure ->
            throw GradleException(
                "Unable to download the study bundle from $url. " +
                    "Build offline with -P$LOCAL_ARCHIVE_PROPERTY=<path to a local archive>.",
                failure
            )
        }
        return destination
    }

    private fun unzip(archive: File, destination: File) {
        destination.deleteRecursively()
        destination.mkdirs()
        ZipFile(archive).use { zip ->
            zip.entries().asSequence().filterNot { it.isDirectory }.forEach { entry ->
                val target = destination.resolve(entry.name).normalize()
                require(target.startsWith(destination)) { "Entry escapes the bundle: ${entry.name}" }
                target.parentFile.mkdirs()
                zip.getInputStream(entry).use { source ->
                    target.outputStream().use(source::copyTo)
                }
            }
        }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { stream ->
            val buffer = ByteArray(DIGEST_BUFFER_BYTES)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        /**
         * Gradle property naming a local archive to use instead of downloading the pinned one.
         */
        const val LOCAL_ARCHIVE_PROPERTY = "studyBundleArchive"

        private const val BUNDLE_KEY = "bundle"
        private const val URL_KEY = "url"
        private const val DIGEST_KEY = "sha256"
        private const val DIGEST_BUFFER_BYTES = 8192
    }
}
