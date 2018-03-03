package me.ialistannen.spotifydownloaderjvm.downloading

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLException
import com.sapher.youtubedl.YoutubeDLRequest
import io.reactivex.Observable
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.roundToInt

class YoutubeDlDownloader : Downloader {

    override fun canDownloader(url: String): Boolean {
        return "youtube" in url
    }

    override fun download(url: String, path: Path): Observable<Double> {
        return Observable.create {
            val dlRequest = YoutubeDLRequest(url).apply {
                setOption("extract-audio")
                setOption("audio-format", "mp3")
                setOption("output", makeOutputPath(path))
            }

            try {
                val dlResponse = YoutubeDL.execute(dlRequest, { progress, eta ->
                    if (!it.isDisposed) {
                        it.onNext(progress.toDouble())
                    }
                })

                if (dlResponse.exitCode != 0) {
                    it.onError(YoutubeDLException("Non-zero exit code: '${dlResponse.exitCode}'"))
                }

            } catch (e: YoutubeDLException) {
                if (!it.isDisposed) {
                    it.onError(e)
                }
            }

            if (!it.isDisposed) {
                it.onComplete()
            }
        }
    }

    private fun makeOutputPath(path: Path): String {
        val pathString = path.toAbsolutePath().toString()
                .replace(".mp3", "")

        return "$pathString.%(ext)s"
    }

}

fun main(args: Array<String>) {
    val youtubeDlDownloader = YoutubeDlDownloader()
    youtubeDlDownloader.download("" +
            "https://www.youtube.com/watch?v=DYRtvMIWXzE",
            Paths.get("/tmp/hm.mp3")
    ).subscribe {
        println((it * 100).roundToInt())
    }
}