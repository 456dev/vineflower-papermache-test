package io.papermc.mache

import dotGradleDirectory
import io.papermc.mache.constants.*
import io.papermc.mache.lib.data.LibrariesList
import io.papermc.mache.lib.data.api.MinecraftDownload
import io.papermc.mache.lib.data.api.MinecraftManifest
import io.papermc.mache.lib.data.api.MinecraftVersionManifest
import io.papermc.mache.lib.json
import io.papermc.mache.util.*
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.resources.TextResource
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import kotlin.io.path.exists
import kotlin.io.path.useLines
import kotlin.io.path.writeText

object ConfigureVersionProject {

    fun configure(target: Project, mache: MacheExtension) {
        return target.configure0(mache)
    }

    private fun Project.configure0(mache: MacheExtension) {
        val mcManifestFile: RegularFile = rootProject.layout.dotGradleDirectory.file(MC_MANIFEST)
        val mcManifest = json.decodeFromString<MinecraftManifest>(
            resources.text.fromFile(mcManifestFile).asString()
        )

        val mcVersionManifestFile: RegularFile = layout.dotGradleDirectory.file(MC_VERSION)
        val mcVersion = mcManifest.versions.first { it.id == mache.minecraftVersion.get() }
        download.download(
            mcVersion.url,
            mcVersionManifestFile,
            Hash(mcVersion.sha1, HashingAlgorithm.SHA1)
        )

        val manifestResource: TextResource = resources.text.fromFile(mcVersionManifestFile)
        val mcVersionManifest =
            json.decodeFromString<MinecraftVersionManifest>(manifestResource.asString())

        // must be configured now before the value of the property is read later
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(mcVersionManifest.javaVersion.majorVersion))
            }
        }
        tasks.withType(JavaCompile::class).configureEach {
            options.release.set(mcVersionManifest.javaVersion.majorVersion)
        }

        val mcIsServer = mache.minecraftJarType.getOrElse("SERVER").uppercase() == "SERVER"

        val downloadInputJarFile = layout.dotGradleDirectory.file(DOWNLOAD_INPUT_JAR)
        val inputMappingsFile = layout.dotGradleDirectory.file(INPUT_MAPPINGS)
        downloadInputFiles(
            download,
            mcVersionManifest,
            downloadInputJarFile,
            inputMappingsFile,
            mcIsServer
        )

        val inputHash =
            downloadInputJarFile.convertToPath().hashFile(HashingAlgorithm.SHA256).asHexString()

        if (mcIsServer) {
            val librariesFile = layout.dotGradleDirectory.file(INPUT_LIBRARIES_LIST)
            val libraries = determineLibraries(downloadInputJarFile, inputHash, librariesFile)

            dependencies {
                for (library in libraries) {
                    "minecraft"(library)
                }
            }
        } else {
            dependencies {
                for (library in mcVersionManifest.libraries) {
                    "minecraft"(library.name)
                }
            }
        }

    }

    private fun Project.downloadInputFiles(
        download: DownloadService,
        manifest: MinecraftVersionManifest,
        inputJar: Any,
        inputMappings: Any,
        mcIsServer: Boolean,
    ) {
        val jarManifest: MinecraftDownload
        val mappingsManifest: MinecraftDownload
        if (mcIsServer) {
            jarManifest = manifest.downloads.server
            mappingsManifest = manifest.downloads.serverMappings
        } else {
            jarManifest = manifest.downloads.client
            mappingsManifest = manifest.downloads.clientMappings
        }


        runBlocking {
            awaitAll(
                download.downloadAsync(
                    jarManifest.url,
                    inputJar,
                    Hash(jarManifest.sha1, HashingAlgorithm.SHA1),
                ),
                download.downloadAsync(
                    mappingsManifest.url,
                    inputMappings,
                    Hash(mappingsManifest.sha1, HashingAlgorithm.SHA1),
                ) {
                    log("Downloading %s jar".format(if (mcIsServer) "server" else "client"))
                },
            )
        }
    }

    private fun Project.determineLibraries(
        jar: Any,
        inputHash: String,
        libraries: Any
    ): List<String> {
        val librariesJson = libraries.convertToPath()
        val libs = if (librariesJson.exists()) {
            json.decodeFromString<LibrariesList>(resources.text.fromFile(libraries).asString())
        } else {
            null
        }

        val inputJar = jar.convertToPath()
        if (libs != null) {
            if (inputHash == libs.sha256) {
                return libs.libraries
            }
        }

        val result = inputJar.useZip { root ->
            val librariesList = root.resolve("META-INF").resolve("libraries.list")

            return@useZip librariesList.useLines { lines ->
                return@useLines lines.map { line ->
                    val parts = line.split(whitespace)
                    if (parts.size != 3) {
                        throw Exception("libraries.list file is invalid")
                    }
                    return@map parts[1]
                }.toList()
            }
        }

        val resultList = json.encodeToString(LibrariesList(inputHash, result))
        librariesJson.writeText(resultList)
        return result
    }

    private fun Project.log(msg: String) {
        println("$path > $msg")
    }
}
