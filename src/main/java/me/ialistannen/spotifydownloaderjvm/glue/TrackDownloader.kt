package me.ialistannen.spotifydownloaderjvm.glue

import io.reactivex.Observable
import me.ialistannen.spotifydownloaderjvm.downloading.Downloader
import me.ialistannen.spotifydownloaderjvm.downloading.YoutubeDlDownloader
import me.ialistannen.spotifydownloaderjvm.metadata.MetadataInjector
import me.ialistannen.spotifydownloaderjvm.metadata.Mp3gicMetadataInjector
import me.ialistannen.spotifydownloaderjvm.normalization.FfmpegNormalizer
import me.ialistannen.spotifydownloaderjvm.normalization.Normalizer
import me.ialistannen.spotifydownloaderjvm.searching.TrackUrlSearcher
import me.ialistannen.spotifydownloaderjvm.searching.YoutubeTrackSearcher
import me.ialistannen.spotifydownloaderjvm.spotify.SpotifyMetadataFetcher
import me.ialistannen.spotifydownloaderjvm.spotify.createSpotifyApiFromClientCredentials
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.roundToInt

class TrackDownloader(
        private val spotifyMetadataFetcher: SpotifyMetadataFetcher,
        private val trackUrlSearcher: TrackUrlSearcher,
        private val downloader: Downloader,
        private val normalizer: Normalizer,
        private val metadataInjector: MetadataInjector
) {

    /**
     * Downloads a single track.
     *
     * @param trackId the ID of the track
     * @param targetFolder the dolder to download to
     */
    fun downloadTrackTest(trackId: String, targetFolder: Path): Observable<Double> {
        return spotifyMetadataFetcher.getTrackMetadata(trackId)
                .map {
                    val url = trackUrlSearcher.findTrackUrl(it)
                            ?: throw DownloadException("No data found")
                    url to it
                }
                .flatMap { pair ->
                    val file = targetFolder.resolve(pair.second.title.sanitize() + ".mp3")
                    println("File: $file")
                    downloader.download(pair.first, file).map {
                        it / 2
                    }.concatWith(
                            normalizer.normalize(file).map { it / 2 + 0.5 }
                    ).concatWith(
                            Observable.fromCallable {
                                metadataInjector.inject(file, pair.second)
                                1.0
                            }
                    )
                }
    }

    private fun String.sanitize(): String {
        return replace(Regex("[^a-zA-Z0-9 \\-]"), "")
                .replace(" ", "_")
    }
}

class DownloadException(message: String) : RuntimeException(message)

fun main(args: Array<String>) {
    val trackDownloader = TrackDownloader(
            SpotifyMetadataFetcher(createSpotifyApiFromClientCredentials(
                    "***REMOVED***",
                    "***REMOVED***"
            )),
            YoutubeTrackSearcher(),
            YoutubeDlDownloader(),
            FfmpegNormalizer(
                    Paths.get("/bin/ffmpeg"),
                    Paths.get("/bin/ffprobe")
            ),
            Mp3gicMetadataInjector()
    )

    trackDownloader.downloadTrackTest(
            "48W1cLE8KD0n4SWtj3YLtz",
            Paths.get("/tmp")
    ).subscribe(
            {
                println("Currently at: ${(it * 100).roundToInt()}")
            },
            {
                it.printStackTrace()
            },
            {
                println("Done!")
            }
    )
}