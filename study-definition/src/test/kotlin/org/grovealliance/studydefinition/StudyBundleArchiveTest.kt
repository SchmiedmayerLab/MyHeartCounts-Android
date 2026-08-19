//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition

import com.github.luben.zstd.ZstdOutputStream
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File

class StudyBundleArchiveTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `unpack extracts the archive the Swift exporter produces`() {
        val bundleDir = File(temporaryFolder.root, "mhcStudyBundle.${StudyBundle.FILE_EXTENSION}")

        archiveFixture().use { StudyBundle.unpack(it, bundleDir) }

        val definition = File(bundleDir, "definition.json")
        assertThat(definition.exists()).isTrue()
        val root = Json.parseToJsonElement(definition.readText()).jsonObject
        assertThat(root["schemaVersion"]?.jsonPrimitive?.content).isEqualTo("0.14.0")
        assertThat(bundleDir.walkTopDown().count { it.isFile }).isEqualTo(FIXTURE_FILE_COUNT)
        assertThat(File(bundleDir, "consent/Consent+en-US.md").readText()).contains("consent")
    }

    @Test
    fun `unpack replaces previous bundle contents`() {
        val bundleDir = File(temporaryFolder.root, "mhcStudyBundle.${StudyBundle.FILE_EXTENSION}")
        File(bundleDir, "stale.txt").apply { parentFile?.mkdirs() }.writeText("stale")

        archiveFixture().use { StudyBundle.unpack(it, bundleDir) }

        assertThat(File(bundleDir, "stale.txt").exists()).isFalse()
    }

    @Test
    fun `unpack rejects a directory without the bundle extension`() {
        assertThrows(IllegalArgumentException::class.java) {
            archiveFixture().use { StudyBundle.unpack(it, File(temporaryFolder.root, "bundle")) }
        }
    }

    @Test
    fun `unpack rejects entries escaping the bundle directory`() {
        val bundleDir = File(temporaryFolder.root, "evil.${StudyBundle.FILE_EXTENSION}")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            maliciousArchive().inputStream().use { StudyBundle.unpack(it, bundleDir) }
        }

        assertThat(exception).hasMessageThat().contains("escapes the bundle")
        assertThat(File(temporaryFolder.root, "escaped.txt").exists()).isFalse()
    }

    private fun archiveFixture() =
        requireNotNull(javaClass.classLoader?.getResourceAsStream(FIXTURE_PATH)) {
            "Missing test fixture $FIXTURE_PATH"
        }

    /**
     * A zstd-compressed tar holding a single file whose path climbs out of the bundle directory.
     */
    private fun maliciousArchive(): ByteArray {
        val contents = "escaped".toByteArray()
        val header = ByteArray(BLOCK_SIZE)
        val name = "../escaped.txt".toByteArray()
        name.copyInto(header)
        "%011o ".format(contents.size).toByteArray().copyInto(header, destinationOffset = 124)
        header[156] = '0'.code.toByte()
        val tar = ByteArrayOutputStream().apply {
            write(header)
            write(contents.copyOf(BLOCK_SIZE))
            write(ByteArray(BLOCK_SIZE * 2))
        }
        return ByteArrayOutputStream().also { compressed ->
            ZstdOutputStream(compressed).use { it.write(tar.toByteArray()) }
        }.toByteArray()
    }

    private companion object {
        // Exported by the exportStudyBundleTestFixture task into the test resources.
        const val FIXTURE_PATH = "mhcStudyBundle.studybundle.tar.zst"
        const val FIXTURE_FILE_COUNT = 61
        const val BLOCK_SIZE = 512
    }
}
