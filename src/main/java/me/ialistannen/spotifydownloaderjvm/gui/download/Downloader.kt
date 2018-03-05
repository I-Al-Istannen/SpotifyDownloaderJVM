package me.ialistannen.spotifydownloaderjvm.gui.download

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import javafx.beans.property.SimpleDoubleProperty
import me.ialistannen.spotifydownloaderjvm.glue.TrackDownloader
import me.ialistannen.spotifydownloaderjvm.glue.VideoNotFoundException
import me.ialistannen.spotifydownloaderjvm.gui.model.DownloadingTrack
import me.ialistannen.spotifydownloaderjvm.gui.model.Status
import me.ialistannen.spotifydownloaderjvm.gui.util.getStackTraceString
import me.ialistannen.spotifydownloaderjvm.spotify.SpotifyTrackFetcher
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Downloads songs and talks with the GUI.
 *
 * **Can not be reused, as it keeps internal state!**
 */
class Downloader(
        private val trackDownloader: TrackDownloader,
        private val spotifyTrackFetcher: SpotifyTrackFetcher,
        private val outputFolder: Path,
        parallelism: Int
) {

    private val executor: ExecutorService = Executors.newFixedThreadPool(parallelism, {
        val thread = Thread(it)
        thread.isDaemon = true

        thread
    })
    private val disposable: CompositeDisposable = CompositeDisposable()

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
                .observeOn(JavaFxScheduler.platform())
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
                            it.id,
                            SimpleDoubleProperty(-1.0)
                    )
                }
                .toList()
                .observeOn(JavaFxScheduler.platform())
                .subscribe(
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
     */
    fun startDownload(tracks: Iterable<DownloadingTrack>) {
        tracks.forEach { track ->
            trackDownloader.downloadTrack(track.id.value, outputFolder)
                    .subscribeOn(ExecutorScheduler(executor))
                    .subscribe(
                            {
                                when (it) {
                                    is TrackDownloader.DownloadProgress.Progress -> {
                                        if (it.progress >= 0.5) {
                                            track.status.set(Status.PROCESSING)
                                        } else {
                                            track.status.set(Status.DOWNLOADING)
                                        }
                                        track.progress.set(it.progress)
                                    }
                                }
                            },
                            {
                                if (it is VideoNotFoundException) {
                                    track.status.set(Status.NOT_FOUND)
                                } else {
                                    track.status.set(Status.ERROR)
                                }
                                track.error.set(it.getStackTraceString())
                            },
                            {
                                track.status.set(Status.FINISHED)
                            }
                    ).addTo(disposable)
        }
    }

    /**
     * Stops all downloads.
     */
    fun stopDownload() {
        disposable.dispose()
        executor.shutdownNow()
    }
}