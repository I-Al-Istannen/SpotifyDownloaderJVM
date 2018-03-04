package me.ialistannen.spotifydownloaderjvm.gui.main

import com.jfoenix.controls.JFXSlider
import com.jfoenix.controls.JFXTextField
import com.jfoenix.validation.base.ValidatorBase
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
import javafx.scene.control.TextInputControl
import javafx.scene.layout.Pane
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import me.ialistannen.spotifydownloaderjvm.downloading.YoutubeDlDownloader
import me.ialistannen.spotifydownloaderjvm.glue.TrackDownloader
import me.ialistannen.spotifydownloaderjvm.gui.MainApplication
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
    private lateinit var playlistUrl: JFXTextField

    @FXML
    private lateinit var parallelismSlider: JFXSlider

    @FXML
    private lateinit var outputPath: Label

    @FXML
    private lateinit var downloadButton: Button

    @FXML
    private lateinit var clientId: JFXTextField

    @FXML
    private lateinit var clientSecret: JFXTextField

    @FXML
    private lateinit var ffmpegPath: JFXTextField

    @FXML
    private lateinit var ffprobePath: JFXTextField

    private var file: File? = null
    private var ffmpeg: File? = null
    private var ffprobe: File? = null

    @FXML
    private fun initialize() {
        setupPlaylistUrl()
        setupDownloadButton()

        ffmpegPath.textProperty().observeToPath { ffmpeg = it.toFile() }
        ffprobePath.textProperty().observeToPath { ffprobe = it.toFile() }

        clientSecret.text = "***REMOVED***"
        clientId.text = "***REMOVED***"
    }

    private fun ObservableValue<String>.observeToPath(action: (Path) -> Unit) {
        JavaFxObservable.valuesOf(this)
                .filter { it.isNotBlank() }
                .map { Paths.get(it) }
                .filter { Files.exists(it) }
                .forEach { action.invoke(it) }
    }

    private fun setupPlaylistUrl() {
        playlistUrl.validators.add(object : ValidatorBase() {
            override fun eval() {
                val textField = srcControl.value as TextInputControl
                hasErrors.set(!textField.text.isValidPlaylistLink())
            }
        })
    }

    private fun setupDownloadButton() {
        val disableDownload = Observable.combineLatest(
                listOf<Observable<Boolean>>(
                        JavaFxObservable.valuesOf(playlistUrl.textProperty().isEmpty, true),
                        JavaFxObservable.valuesOf(clientSecret.textProperty().isEmpty, true),
                        JavaFxObservable.valuesOf(clientId.textProperty().isEmpty, true),
                        JavaFxObservable.valuesOf(playlistUrl.textProperty(), "")
                                .map { !playlistUrl.validate() }
                ),
                { array -> array.filterIsInstance<Boolean>().reduce({ one, two -> one || two }) }
        )
        downloadButton.disableProperty().bind(JavaFxObserver.toBinding(disableDownload))
    }

    @FXML
    fun onDownload() {
        if (file == null || ffmpeg == null || ffprobe == null) {
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
                        YoutubeDlDownloader(),
                        FfmpegNormalizer(
                                ffmpeg!!.toPath(), ffprobe!!.toPath()
                        ),
                        Mp3gicMetadataInjector()
                ),
                SpotifyTrackFetcher(spotifyApi)
        )
        downloader.passPlaylistTracks(playlistUrl, controller)
        controller.parallelism = parallelism
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

        this.file = file
    }

    @FXML
    fun onPickFfmpegPath() {
        pickFile("Pick the FFMPEG file to use")?.let {
            ffmpeg = it
            ffmpegPath.text = it.absolutePath
        }
    }

    @FXML
    fun onPickFfprobePath() {
        pickFile("Pick the FFPROBE file to use")?.let {
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