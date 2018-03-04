package me.ialistannen.spotifydownloaderjvm.gui.main

import com.jfoenix.controls.JFXSlider
import com.jfoenix.controls.JFXTextField
import com.jfoenix.validation.base.ValidatorBase
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.rxjavafx.observables.JavaFxObservable
import io.reactivex.rxjavafx.observers.JavaFxObserver
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextInputControl
import javafx.scene.layout.Pane
import javafx.stage.DirectoryChooser
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
import java.io.File
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

    var spotifyTrackFetcher: SpotifyTrackFetcher? = null
    private var file: File? = null

    @FXML
    private fun initialize() {
        playlistUrl.validators.add(object : ValidatorBase() {
            override fun eval() {
                if (spotifyTrackFetcher == null) {
                    hasErrors.set(true)
                    return
                }

                val textField = srcControl.value as TextInputControl
                hasErrors.set(!spotifyTrackFetcher!!.isValidLink(textField.text))
            }
        })

        val downloadButtonBinding = JavaFxObserver.toBinding(Observable.combineLatest(
                JavaFxObservable.valuesOf(playlistUrl.textProperty().isEmpty, true),
                JavaFxObservable.valuesOf(playlistUrl.textProperty(), "")
                        .map { !playlistUrl.validate() }
                ,
                BiFunction<Boolean, Boolean, Boolean> { one, two -> one || two }
        ))

        downloadButton.disableProperty().bind(downloadButtonBinding)
    }

    @FXML
    fun onDownload() {
        val parallelism = parallelismSlider.value.roundToInt()
        val playlistUrl = this.playlistUrl.text

        val fxmlLoader = FXMLLoader(javaClass.getResource("/fxml/DownloadScreen.fxml"))
        val pane: Pane = fxmlLoader.load()
        val controller: DownloadScreenController = fxmlLoader.getController()

        val spotifyApi = createSpotifyApiFromClientCredentials(
                "***REMOVED***",
                "***REMOVED***"
        )
        val downloader = Downloader(
                TrackDownloader(
                        SpotifyMetadataFetcher(spotifyApi),
                        YoutubeTrackSearcher(),
                        YoutubeDlDownloader(),
                        FfmpegNormalizer(
                                Paths.get("/bin/ffmpeg"), Paths.get("/bin/ffprobe")
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
}