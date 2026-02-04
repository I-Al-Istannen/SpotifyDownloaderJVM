package me.ialistannen.spotifydownloaderjvm.gui.main

import io.reactivex.Observable
import io.reactivex.rxjavafx.observables.JavaFxObservable
import io.reactivex.rxjavafx.observers.JavaFxObserver
import javafx.beans.value.ObservableValue
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.layout.Pane
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import me.ialistannen.spotifydownloaderjvm.downloading.YtDlpDownloader
import me.ialistannen.spotifydownloaderjvm.glue.TrackDownloader
import me.ialistannen.spotifydownloaderjvm.gui.MainApplication
import me.ialistannen.spotifydownloaderjvm.gui.dependencydiscovery.FfmpegFinder
import me.ialistannen.spotifydownloaderjvm.gui.download.DownloadScreenController
import me.ialistannen.spotifydownloaderjvm.gui.download.Downloader
import me.ialistannen.spotifydownloaderjvm.metadata.Mp3gicMetadataInjector
import me.ialistannen.spotifydownloaderjvm.normalization.FfmpegNormalizer
import me.ialistannen.spotifydownloaderjvm.searching.YoutubeTrackSearcher
import me.ialistannen.spotifydownloaderjvm.spotify.SpotifyMetadataFetcher
import me.ialistannen.spotifydownloaderjvm.spotify.SpotifyTrackFetcher
import me.ialistannen.spotifydownloaderjvm.spotify.createSpotifyApiFromClientCredentials
import me.ialistannen.spotifydownloaderjvm.spotify.isValidPlaylistLink
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.roundToInt


class MainScreenController {
    @FXML
    private lateinit var playlistUrl: TextField

    @FXML
    private lateinit var parallelismSlider: Slider

    @FXML
    private lateinit var outputPath: Label

    @FXML
    private lateinit var downloadButton: Button

    @FXML
    private lateinit var clientId: TextField

    @FXML
    private lateinit var clientSecret: TextField

    @FXML
    private lateinit var ffmpegPath: TextField

    @FXML
    private lateinit var ffprobePath: TextField

    private var outputFile: File? = null
    private var ffmpeg: File? = null
    private var ffprobe: File? = null

    @FXML
    private fun initialize() {
        setupDownloadButton()
        setupFfmpegSettings()

        parallelismSlider.max = 4.0 // you do not want to be blocked by youtube, do you?
    }

    private fun setupFfmpegSettings() {
        val ffmpegFinder = FfmpegFinder()
        ffmpegFinder.findFfmpeg().subscribe(
                { ffmpegPath.text = it.toAbsolutePath().toString() },
                { it.printStackTrace() }
        )
        ffmpegFinder.findFfprobe().subscribe(
                { ffprobePath.text = it.toAbsolutePath().toString() },
                { it.printStackTrace() }
        )

        ffmpegPath.textProperty().observeToPath { ffmpeg = it.toFile() }
        ffprobePath.textProperty().observeToPath { ffprobe = it.toFile() }
    }

    private fun ObservableValue<String>.observeToPath(action: (Path) -> Unit) {
        JavaFxObservable.valuesOf(this)
                .filter { it.isNotBlank() }
                .map { Paths.get(it) }
                .filter { Files.exists(it) }
                .forEach { action.invoke(it) }
    }

    private fun setupDownloadButton() {
        val disableDownload = Observable.combineLatest(
                listOf<Observable<Boolean>>(
                        JavaFxObservable.valuesOf(playlistUrl.textProperty().isEmpty, true),
                        JavaFxObservable.valuesOf(clientSecret.textProperty().isEmpty, true),
                        JavaFxObservable.valuesOf(clientId.textProperty().isEmpty, true),
                        JavaFxObservable.valuesOf(playlistUrl.textProperty(), "")
                                .map { !it.isValidPlaylistLink() }
                )
        ) { array -> array.filterIsInstance<Boolean>().reduce { one, two -> one || two } }
        downloadButton.disableProperty().bind(JavaFxObserver.toBinding(disableDownload))
    }

    /**
     * Sets the client id.
     *
     * @param clientId the client id
     */
    fun setClientId(clientId: String) {
        this.clientId.text = clientId
    }

    /**
     * Sets the client secret
     *
     * @param clientSecret the client secret
     */
    fun setClientSecret(clientSecret: String) {
        this.clientSecret.text = clientSecret
    }

    @FXML
    fun onDownload() {
        if (outputFile == null || ffmpeg == null || ffprobe == null) {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error, files not found"
            alert.headerText = "You need to set the output folder and the paths for ffmpeg"
            alert.show()
            return
        }

        val parallelism = parallelismSlider.value.roundToInt()
        val playlistUrl = this.playlistUrl.text

        val fxmlLoader = FXMLLoader(javaClass.getResource("/fxml/DownloadScreen.fxml"))
        val pane: Pane = fxmlLoader.load()
        val controller: DownloadScreenController = fxmlLoader.getController()

        val spotifyApi = createSpotifyApiFromClientCredentials(
                clientSecret.text,
                clientId.text
        )
        val downloader = Downloader(
                TrackDownloader(
                        SpotifyMetadataFetcher(spotifyApi),
                        YoutubeTrackSearcher(),
                        YtDlpDownloader(ffmpeg!!.toPath().parent),
                        FfmpegNormalizer(
                                ffmpeg!!.toPath(), ffprobe!!.toPath()
                        ),
                        Mp3gicMetadataInjector()
                ),
                SpotifyTrackFetcher(spotifyApi),
                outputFile!!.toPath(),
                parallelism
        )
        downloader.passPlaylistTracks(playlistUrl, controller)
        controller.downloader = downloader

        val newStage = Stage()
        newStage.initModality(Modality.WINDOW_MODAL)
        newStage.initOwner(MainApplication.stage)
        newStage.scene = Scene(pane)
        newStage.sizeToScene()
        newStage.show()
    }

    @FXML
    fun onPickOutputPath() {
        val chooser = DirectoryChooser()
        chooser.title = "Pick an output directory"
        val file = chooser.showDialog(playlistUrl.scene.window) ?: return

        outputPath.text = file.name

        this.outputFile = file
    }

    @FXML
    fun onPickFfmpegPath() {
        pickFile("Pick the FFMPEG outputFile to use")?.let {
            ffmpeg = it
            ffmpegPath.text = it.absolutePath
        }
    }

    @FXML
    fun onPickFfprobePath() {
        pickFile("Pick the FFPROBE outputFile to use")?.let {
            ffprobe = it
            ffprobePath.text = it.absolutePath
        }
    }

    private fun pickFile(title: String): File? {
        val chooser = FileChooser()
        chooser.title = title
        return chooser.showOpenDialog(playlistUrl.scene.window)
    }
}
