package me.ialistannen.spotifydownloaderjvm.gui.model

import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue

data class DownloadingTrack(
        val status: ObjectProperty<Status>,
        val title: ObservableValue<String>,
        val artist: ObservableValue<String>,
        val progress: DoubleProperty,
        val id: ObservableValue<String>
) {
    companion object {

        fun newInstance(status: Status, title: String, artist: String, id: String,
                        progress: DoubleProperty): DownloadingTrack {
            return DownloadingTrack(
                    SimpleObjectProperty(status),
                    SimpleStringProperty(title),
                    SimpleStringProperty(artist),
                    progress,
                    SimpleStringProperty(id)
            )
        }
    }
}

enum class Status {
    QUEUED, DOWNLOADING, PROCESSING, FINISHED
}