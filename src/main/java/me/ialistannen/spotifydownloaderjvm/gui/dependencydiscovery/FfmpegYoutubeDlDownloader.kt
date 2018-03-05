package me.ialistannen.spotifydownloaderjvm.gui.dependencydiscovery

import io.reactivex.Maybe
import io.reactivex.Single
import me.ialistannen.spotifydownloaderjvm.gui.util.findExecutable
import me.ialistannen.spotifydownloaderjvm.gui.util.getInitialFolder
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipFile

class FfmpegYoutubeDlDownloader {

    companion object {
        private const val FFMPEG_URL = "https://ffmpeg.zeranoe.com/builds/win64/static/" +
                "ffmpeg-latest-win64-static.zip"
        private const val YOUTUBE_DL_URL = "https://yt-dl.org/downloads/2018.03.03/youtube-dl.exe"
    }

    /**
     * Downloads the dependencies.
     *
     * @param targetDir the directory to download them to
     * @return returns nothing if it succeeded, throws an error otherwise
     */
    fun download(targetDir: Path): Single<out Any> {
        return downloadFfmpeg(targetDir)
                .flatMap { downloadYoutubeDl(targetDir) }
                .toSingle()
    }

    private fun downloadYoutubeDl(targetDirectory: Path): Maybe<Path> {
        return downloadFile(
                targetDirectory.resolve("youtube-dl.exe"),
                URL(YOUTUBE_DL_URL)
        )
    }

    private fun downloadFfmpeg(targetDirectory: Path): Maybe<Path> {
        return downloadFile(
                Files.createTempFile("downloadTemp", "SpotDown"),
                URL(FFMPEG_URL)
        ).map {
            val zipFile = ZipFile(it.toFile())

            zipFile.use {
                for (entry in it.entries()) {
                    val entryFile = targetDirectory.resolve(entry.name)
                    if (entry.isDirectory) {
                        Files.createDirectories(entryFile)
                    } else {
                        it.getInputStream(entry).use { inStream ->
                            Files.newOutputStream(entryFile, StandardOpenOption.CREATE).use {
                                inStream.copyTo(it)
                            }
                        }
                    }
                }
            }

            Files.deleteIfExists(it)

            targetDirectory
        }
    }

    private fun downloadFile(path: Path, url: URL): Maybe<Path> {
        return Maybe.create {
            try {
                val connection = url.openConnection()
                connection.addRequestProperty("User-Agent", "Mozilla/5.0")
                connection.getInputStream().use { inStream ->
                    Files.newOutputStream(path, StandardOpenOption.CREATE).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                it.onSuccess(path)
            } catch (e: Exception) {
                it.onError(e)
            }
        }
    }

    /**
     * Checks whether it needs to download the dependencies.
     *
     * @return a single indicating whether they should be downloaded or not
     */
    fun needsToDownloadDependencies(): Single<Boolean> {
        val initialFolder = getInitialFolder(FfmpegYoutubeDlDownloader::class.java)
        return findExecutable("ffmpeg", initialFolder)
                .map { true }
                .toSingle(false)
                .flatMap { foundFfprobe ->
                    findExecutable("ffprobe", initialFolder)
                            .map { foundFfprobe }
                            .toSingle(false)
                }.flatMap { foundOthers ->
                    findExecutable("youtubedl", initialFolder)
                            .map { foundOthers }
                            .toSingle(false)
                }
    }
}