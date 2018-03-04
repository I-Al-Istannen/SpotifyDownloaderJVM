package me.ialistannen.spotifydownloaderjvm.glue

import io.reactivex.Observable
import me.ialistannen.spotifydownloaderjvm.downloading.Downloader
import me.ialistannen.spotifydownloaderjvm.metadata.Metadata
import me.ialistannen.spotifydownloaderjvm.metadata.MetadataInjector
import me.ialistannen.spotifydownloaderjvm.normalization.Normalizer
import me.ialistannen.spotifydownloaderjvm.searching.TrackUrlSearcher
import me.ialistannen.spotifydownloaderjvm.spotify.SpotifyMetadataFetcher
import java.nio.file.Path

typealias Progress = TrackDownloader.DownloadProgress.Progress

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
     * @param targetFolder the folder to download to
     */
    fun downloadTrack(trackId: String, targetFolder: Path): Observable<DownloadProgress> {
        return spotifyMetadataFetcher.getTrackMetadata(trackId)
                .map {
                    val url = trackUrlSearcher.findTrackUrl(it)
                            ?: throw DownloadException("No data found")
                    url to it
                }
                .flatMap { pair ->
                    val metadata = pair.second
                    val fileName = "${metadata.title.sanitize()}-${metadata.artists[0].sanitize()}.mp3"

                    val file = targetFolder.resolve(fileName)
                    Observable
                            .just<DownloadProgress>(DownloadProgress.DownloadMetadata(metadata))
                            .concatWith(
                                    downloader.download(pair.first, file).map {
                                        Progress(it / 2)
                                    }
                            )
                            .concatWith(
                                    normalizer.normalize(file).map { Progress(it / 2 + 0.5) }
                            ).concatWith(
                                    Observable.fromCallable {
                                        metadataInjector.inject(file, metadata)
                                        Progress(1.0)
                                    }
                            )
                }
    }

    private fun String.sanitize(): String {
        return replace(Regex("[^a-zA-Z0-9 \\-_.]"), "")
                .replace(" ", "_")
    }

    /**
     * Contains information about the progress of the download as well as eventual metadata.
     */
    sealed class DownloadProgress {
        data class Progress(val progress: Double) : DownloadProgress()
        data class DownloadMetadata(val metadata: Metadata) : DownloadProgress()
    }
}

class DownloadException(message: String) : RuntimeException(message)