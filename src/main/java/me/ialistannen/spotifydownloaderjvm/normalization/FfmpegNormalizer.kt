package me.ialistannen.spotifydownloaderjvm.normalization

import io.reactivex.Observable
import java.nio.file.Path
import java.nio.file.Paths

class FfmpegNormalizer(
        private val ffmpegPath: Path,
        private val ffprobePath: Path
) : Normalizer {

    override fun normalize(file: Path): Observable<Double> {
        return Observable.create {
            FfmpegWrapper(ffmpegPath, ffprobePath).applyLoudNorm(file).subscribe { progress ->
                if (!it.isDisposed) {
                    it.onNext(progress)
                }
            }

            it.onComplete()
        }
    }

}

fun main(args: Array<String>) {
    val normalizer = FfmpegNormalizer(Paths.get("/bin/ffmpeg"), Paths.get("/bin/ffprobe"))
    normalizer.normalize(Paths.get("/tmp/hm.mp3")).subscribe {
        println(it)
    }
}