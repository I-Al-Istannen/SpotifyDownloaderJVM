package me.ialistannen.spotifydownloaderjvm.downloading

import com.sapher.youtubedl.DownloadProgressCallback
import com.sapher.youtubedl.YoutubeDLException
import com.sapher.youtubedl.YoutubeDLRequest
import com.sapher.youtubedl.YoutubeDLResponse
import com.sapher.youtubedl.utils.StreamGobbler
import com.sapher.youtubedl.utils.StreamProcessExtractor
import io.reactivex.Observable
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class YoutubeDlDownloader(private val ffmpegDirectory: Path) : Downloader {

    override fun canDownloader(url: String): Boolean {
        return "youtube" in url
    }

    override fun download(url: String, path: Path): Observable<Double> {
        return Observable.create {
            val dlRequest = SpaceAwareYoutubeDlRequest(url).apply {
                setOption("extract-audio")
                setOption("audio-format", "mp3")
                setOption("output", makeOutputPath(path))
                setOption("ffmpeg-location", ffmpegDirectory.toAbsolutePath().toString())
                directory = getCurrentDirectory()
            }

            try {
                val dlResponse = execute(dlRequest, DownloadProgressCallback { progress, _ ->
                    if (!it.isDisposed) {
                        it.onNext(progress.toDouble() / 100)
                    }
                })

                if (dlResponse.exitCode != 0) {
                    it.onError(YoutubeDLException("Non-zero exit code: '${dlResponse.exitCode}'"))
                }

                if (!it.isDisposed) {
                    it.onComplete()
                }
            } catch (e: YoutubeDLException) {
                if (!it.isDisposed) {
                    it.onError(e)
                }
            }
        }
    }

    private fun getCurrentDirectory() =
        Paths.get(javaClass.protectionDomain.codeSource.location.toURI())
            .parent
            .toAbsolutePath()
            .toString()

    private fun makeOutputPath(path: Path): String {
        val pathString = path.toAbsolutePath().toString()
            .replace(".mp3", "")

        return "$pathString.%(ext)s"
    }


    private class SpaceAwareYoutubeDlRequest(url: String) : YoutubeDLRequest(url) {

        fun buildNicerOptions(): List<String> {
            val options = option.entries
                .flatMap {
                    val res = mutableListOf(
                        "--" + it.key
                    )
                    if (it.value != null) {
                        res.add(it.value)
                    }
                    res
                }
                .toMutableList()
            options.add(0, url)

            return options
        }
    }

    @Throws(YoutubeDLException::class)
    private fun execute(
        request: SpaceAwareYoutubeDlRequest,
        callback: DownloadProgressCallback?
    ): YoutubeDLResponse {
        val command = listOf("youtube-dl") + request.buildNicerOptions()
        val directory = request.directory
        val options = request.option

        val outBuffer = StringBuffer()
        val errBuffer = StringBuffer()
        val startTime = System.nanoTime()

        val processBuilder = ProcessBuilder(command)
        if (directory != null) {
            processBuilder.directory(File(directory))
        }

        val process: Process
        process = try {
            processBuilder.start()
        } catch (e: IOException) {
            throw YoutubeDLException(e)
        }

        process.inputStream.use { input ->
            process.errorStream.use { error ->
                val stdOutProcessor = StreamProcessExtractor(outBuffer, input, callback)
                val stdErrProcessor = StreamGobbler(errBuffer, error)

                val exitCode: Int
                exitCode = try {
                    stdOutProcessor.join()
                    stdErrProcessor.join()
                    process.waitFor()
                } catch (e: InterruptedException) {
                    throw YoutubeDLException(e)
                }

                val out = outBuffer.toString()
                val err = errBuffer.toString()

                return if (exitCode > 0) {
                    throw YoutubeDLException(err)
                } else {
                    val elapsedTime = ((System.nanoTime() - startTime) / 1000000L).toInt()
                    YoutubeDLResponse(
                        command.joinToString(" "),
                        options,
                        directory,
                        exitCode,
                        elapsedTime,
                        out,
                        err
                    )
                }
            }
        }
    }

}