package me.ialistannen.spotifydownloaderjvm.gui.download

import io.reactivex.schedulers.Schedulers
import javafx.beans.property.SimpleDoubleProperty
import me.ialistannen.spotifydownloaderjvm.glue.TrackDownloader
import me.ialistannen.spotifydownloaderjvm.gui.model.DownloadingTrack
import me.ialistannen.spotifydownloaderjvm.gui.model.Status
import me.ialistannen.spotifydownloaderjvm.spotify.SpotifyTrackFetcher

/**
 * Downloads songs and talks with the GUI,
 */
class Downloader(
        private val trackDownloader: TrackDownloader,
        private val spotifyTrackFetcher: SpotifyTrackFetcher
) {

    /**
     * Passes the tracks and name of the playlist to the given [DownloadScreenController].
     *
     * @param playlistLink the link to the playlist
     * @param downloadScreenController the [DownloadScreenController] to populate
     */
    fun passPlaylistTracks(playlistLink: String, downloadScreenController: DownloadScreenController) {
        val errorHandler = buildErrorHandler()

        spotifyTrackFetcher.getPlaylistNameFromLink(playlistLink)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { downloadScreenController.setPlaylistName(it) },
                        errorHandler
                )

        spotifyTrackFetcher.getPlaylistTracksFromLink(playlistLink)
                .map { it.track }
                .map {
                    DownloadingTrack.newInstance(
                            Status.QUEUED,
                            it.name,
                            it.artists[0].name,
                            SimpleDoubleProperty(-1.0)
                    )
                }
                .toList().subscribe(
                        {
                            downloadScreenController.setTracks(it)
                        },
                        errorHandler
                )
    }

    private fun buildErrorHandler(): (Throwable) -> Unit {
        return { throwable -> throwable.printStackTrace() }
    }

    /**
     * Starts the download of the given tracks.
     *
     * @param tracks the tracks to download
     * @param parallelism the parallelism level
     */
    fun startDownload(tracks: Iterable<DownloadingTrack>, parallelism: Int) {

    }
}