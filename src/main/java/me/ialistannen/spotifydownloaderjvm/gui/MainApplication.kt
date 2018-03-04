package me.ialistannen.spotifydownloaderjvm.gui

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage
import me.ialistannen.spotifydownloaderjvm.gui.main.MainScreenController
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