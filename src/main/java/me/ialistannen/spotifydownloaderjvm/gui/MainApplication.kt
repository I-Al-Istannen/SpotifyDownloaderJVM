package me.ialistannen.spotifydownloaderjvm.gui

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage

class MainApplication : Application() {

    companion object {
        lateinit var stage: Stage
            private set
    }

    override fun start(primaryStage: Stage) {
        stage = primaryStage

//        val fxmlLoader = FXMLLoader(javaClass.getResource("/fxml/MainScreen.fxml"))
//        val pane: Pane = fxmlLoader.load()
//        val controller: MainScreenController = fxmlLoader.getController()
//        controller.spotifyTrackFetcher = SpotifyTrackFetcher(
//                createSpotifyApiFromClientCredentials(
//                        "***REMOVED***",
//                        "***REMOVED***"
//                )
//        )
        val fxmlLoader = FXMLLoader(javaClass.getResource("/fxml/DownloadScreen.fxml"))
        val pane = fxmlLoader.load<Pane>()

        primaryStage.scene = Scene(pane)

        primaryStage.sizeToScene()
        primaryStage.centerOnScreen()
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(MainApplication::class.java, *args)
}