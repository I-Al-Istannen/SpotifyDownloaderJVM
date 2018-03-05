package me.ialistannen.spotifydownloaderjvm.downloading

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLException
import com.sapher.youtubedl.YoutubeDLRequest
import io.reactivex.Observable
import java.nio.file.Path
import java.nio.file.Paths

class YoutubeDlDownloader(private val ffmpegDirectory: Path) : Downloader {

    override fun canDownloader(url: String): Boolean {
        return "youtube" in url
    }

    override fun download(url: String, path: Path): Observable<Double> {
        return Observable.create {
            val dlRequest = YoutubeDLRequest(url).apply {
                setOption("extract-audio")
                setOption("audio-format", "mp3")
                setOption("output", makeOutputPath(path))
                setOption("ffmpeg-location", ffmpegDirectory.toAbsolutePath().toString())
                directory = getCurrentDirectory()
            }

            try {
                val dlResponse = YoutubeDL.execute(dlRequest, { progress, _ ->
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

}