package me.ialistannen.spotifydownloaderjvm.downloading

import io.reactivex.Observable
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

const val YT_DLP_EXECUTABLE = "yt-dlp"

class YtDlpDownloader(private val ffmpegDirectory: Path) : Downloader {

    override fun canDownloader(url: String): Boolean {
        return "youtube" in url
    }

    override fun download(url: String, path: Path): Observable<Double> {
        return Observable.create { emitter ->
            val options = mutableMapOf<String, String?>()
            options["extract-audio"] = null
            options["audio-format"] = "mp3"
            options["output"] = makeOutputPath(path)
            options["ffmpeg-location"] = ffmpegDirectory.toAbsolutePath().toString()

            val command = buildCommand(url, options)
            val directory = getCurrentDirectory()

            try {
                val response = execute(command, directory) { progress ->
                    if (!emitter.isDisposed) {
                        emitter.onNext(progress)
                    }
                }

                if (response.exitCode != 0) {
                    emitter.onError(YtDlpException("Non-zero exit code: '${response.exitCode}'\n${response.err}"))
                } else if (!emitter.isDisposed) {
                    emitter.onComplete()
                }
            } catch (e: YtDlpException) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
            }
        }
    }

    private fun buildCommand(url: String, options: Map<String, String?>): List<String> {
        val args = mutableListOf(YT_DLP_EXECUTABLE, url)
        options.forEach { (key, value) ->
            args.add("--$key")
            if (value != null) {
                args.add(value)
            }
        }
        return args
    }

    private fun getCurrentDirectory(): String =
        Paths.get(javaClass.protectionDomain.codeSource.location.toURI())
            .parent
            .toAbsolutePath()
            .toString()

    private fun makeOutputPath(path: Path): String {
        val pathString = path.toAbsolutePath().toString()
            .replace(".mp3", "")

        return "$pathString.%(ext)s"
    }

    private fun execute(
        command: List<String>,
        directory: String?,
        progressCallback: (Double) -> Unit
    ): YtDlpResponse {
        val outBuffer = StringBuilder()
        val errBuffer = StringBuilder()
        val startTime = System.nanoTime()

        val processBuilder = ProcessBuilder(command)
        if (directory != null) {
            processBuilder.directory(File(directory))
        }

        val process = processBuilder.start()

        val stdoutThread = Thread {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lineSequence().forEach { line ->
                    outBuffer.appendLine(line)
                    parseProgress(line)?.let { progressCallback(it) }
                }
            }
        }

        val stderrThread = Thread {
            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                reader.lineSequence().forEach { line ->
                    errBuffer.appendLine(line)
                }
            }
        }

        stdoutThread.start()
        stderrThread.start()

        val exitCode: Int = try {
            stdoutThread.join()
            stderrThread.join()
            process.waitFor()
        } catch (e: InterruptedException) {
            throw YtDlpException("Process interrupted", e)
        }

        val elapsedTime = ((System.nanoTime() - startTime) / 1000000L).toInt()
        return YtDlpResponse(
            command.joinToString(" "),
            directory,
            exitCode,
            elapsedTime,
            outBuffer.toString(),
            errBuffer.toString()
        )
    }

    private fun parseProgress(line: String): Double? {
        val pattern = Pattern.compile("\\[download\\]\\s+(\\d+\\.?\\d*)%")
        val matcher = pattern.matcher(line)
        return if (matcher.find()) {
            matcher.group(1).toDoubleOrNull()?.div(100.0)
        } else {
            null
        }
    }

    class YtDlpException(message: String, cause: Throwable? = null) : Exception(message, cause)

    private data class YtDlpResponse(
        val command: String,
        val directory: String?,
        val exitCode: Int,
        val elapsedTime: Int,
        val out: String,
        val err: String
    )
}
