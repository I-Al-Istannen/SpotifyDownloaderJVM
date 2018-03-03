package me.ialistannen.spotifydownloaderjvm.gui

import javafx.application.Application
import javafx.stage.Stage

class MainApplication : Application() {

    override fun start(primaryStage: Stage) {
        primaryStage.centerOnScreen()
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(MainApplication::class.java, *args)
}