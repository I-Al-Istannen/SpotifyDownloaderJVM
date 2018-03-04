package me.ialistannen.spotifydownloaderjvm.gui.model

import javafx.beans.property.*
import javafx.beans.value.ObservableValue

data class DownloadingTrack(
        val status: ObjectProperty<Status>,
        val title: ObservableValue<String>,
        val artist: ObservableValue<String>,
        val progress: DoubleProperty,
        val id: ObservableValue<String>,
        val error: StringProperty
) {
    companion object {

        fun newInstance(status: Status, title: String, artist: String, id: String,
                        progress: DoubleProperty): DownloadingTrack {
            return DownloadingTrack(
                    SimpleObjectProperty(status),
                    SimpleStringProperty(title),
                    SimpleStringProperty(artist),
                    progress,
                    SimpleStringProperty(id),
                    SimpleStringProperty("")
            )
        }
    }
}

enum class Status(val cssStyle: String) {
    QUEUED(""),
    DOWNLOADING("-fx-background-color: -fx-color-palette-400;"),
    PROCESSING("-fx-background-color: -fx-color-palette-400;"),
    ERROR("-fx-background-color: #6A1B9A;"),
    NOT_FOUND("-fx-background-color: #4527A0;"),
    FINISHED("")
}