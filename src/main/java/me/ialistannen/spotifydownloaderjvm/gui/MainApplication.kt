package me.ialistannen.spotifydownloaderjvm.gui

import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.schedulers.Schedulers
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.TextField
import javafx.scene.layout.Pane
import javafx.scene.text.Font
import javafx.stage.Stage
import me.ialistannen.spotifydownloaderjvm.gui.dependencydiscovery.FfmpegYoutubeDlDownloader
import me.ialistannen.spotifydownloaderjvm.gui.main.MainScreenController
import me.ialistannen.spotifydownloaderjvm.gui.util.getInitialFolder
import me.ialistannen.spotifydownloaderjvm.gui.util.getStackTraceString
import me.ialistannen.spotifydownloaderjvm.spotify.SynchronizedSimpleDateFormat

class MainApplication : Application() {

    companion object {
        lateinit var stage: Stage
            private set
    }

    override fun start(primaryStage: Stage) {
        stage = primaryStage

        SynchronizedSimpleDateFormat.injectIntoSpotify()

        val fxmlLoader = FXMLLoader(javaClass.getResource("/fxml/MainScreen.fxml"))
        val pane: Pane = fxmlLoader.load()
        val controller: MainScreenController = fxmlLoader.getController()

        if ("client_id" in parameters.named) {
            controller.setClientId(parameters.named["client_id"]!!)
        }
        if ("client_secret" in parameters.named) {
            controller.setClientSecret(parameters.named["client_secret"]!!)
        }
        
        
        primaryStage.scene = Scene(pane)

        primaryStage.sizeToScene()
        primaryStage.centerOnScreen()
        primaryStage.show()

        FfmpegYoutubeDlDownloader().apply {
            needsToDownloadDependencies().subscribe { needsDownload ->
                if (needsDownload) {
                    if ("win" !in System.getProperty("os.name").toLowerCase()) {
                        val alert = Alert(Alert.AlertType.ERROR)
                        alert.title = "Dependency missing"
                        alert.headerText = "Please install all dependencies and restart the" +
                                " application"
                        alert.showAndWait()
                    } else {
                        val alert = Alert(Alert.AlertType.ERROR)
                        alert.title = "Dependency missing"
                        alert.headerText = "I will now try to install the dependencies, do not" +
                                " close the application until I say so."
                        alert.show()
                        downloadDependencies(this)
                    }
                }
            }
        }
    }

    private fun downloadDependencies(downloader: FfmpegYoutubeDlDownloader) {
        downloader.download(getInitialFolder(MainApplication::class.java))
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(
                        { _ ->
                            val alert = Alert(Alert.AlertType.INFORMATION)
                            alert.title = "Download successful"
                            alert.headerText = "Please restart the application now"
                            alert.showAndWait()
                            Platform.exit()
                        },
                        {
                            val alert = Alert(Alert.AlertType.ERROR)
                            alert.title = "Download unsuccessful"
                            alert.headerText = "Please download the dependencies manually and" +
                                    " restart the application"
                            alert.dialogPane.expandableContent = TextField(it.getStackTraceString())
                                    .apply {
                                        font = Font.font("Monospaced", 15.0)
                                    }
                            alert.showAndWait()
                        }
                )
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Supported arguments:")
        println("--client_id=<client_id>")
        println("--client_secret=<client_secret>")
    }
    Application.launch(MainApplication::class.java, *args)
}