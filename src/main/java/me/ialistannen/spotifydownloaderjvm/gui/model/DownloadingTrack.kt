package me.ialistannen.spotifydownloaderjvm.gui.model

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue

data class DownloadingTrack(
        val status: ObservableValue<Status>,
        val title: ObservableValue<String>,
        val artist: ObservableValue<String>,
        val progress: ObservableValue<Number>
) {
    companion object {

        fun newInstance(status: Status, title: String, artist: String,
                        progress: ObservableValue<Number>): DownloadingTrack {
            return DownloadingTrack(
                    SimpleObjectProperty(status),
                    SimpleStringProperty(title),
                    SimpleStringProperty(artist),
                    progress
            )
        }
    }
}

enum class Status {
    QUEUED, DOWNLOADING, FINISHED
}