package me.ialistannen.spotifydownloaderjvm.gui.download

import javafx.beans.property.SimpleDoubleProperty
import javafx.fxml.FXML
import javafx.scene.control.*
import me.ialistannen.spotifydownloaderjvm.gui.model.DownloadingTrack
import me.ialistannen.spotifydownloaderjvm.gui.model.Status

class DownloadScreenController {

    @FXML
    private lateinit var table: TableView<DownloadingTrack>

    @FXML
    private lateinit var downloadButton: Button

    @FXML
    private lateinit var playlistName: Label

    var parallelism: Int = 0
    lateinit var downloader: Downloader

    @FXML
    private fun initialize() {
        val nameColumn = TableColumn<DownloadingTrack, String>("Name").apply {
            setCellValueFactory { it.value.title }
        }
        val artistColumn = TableColumn<DownloadingTrack, String>("Artist").apply {
            setCellValueFactory { it.value.artist }
        }
        val statusColumn = TableColumn<DownloadingTrack, Status>("Status").apply {
            setCellValueFactory { it.value.status }
        }
        val progressColumn = TableColumn<DownloadingTrack, Number>("Progress").apply {
            setCellValueFactory { it.value.progress }
            setCellFactory({ _ ->
                object : TableCell<DownloadingTrack, Number>() {
                    override fun updateItem(item: Number?, empty: Boolean) {
                        super.updateItem(item, empty)

                        if (item == null || empty) {
                            text = ""
                            graphic = null
                            return
                        }

                        text = item.toInt().toString()
                    }
                }
            })
        }

        table.columns.setAll(nameColumn, artistColumn, statusColumn, progressColumn)

        setTracks(listOf(
                DownloadingTrack.newInstance(
                        Status.QUEUED, "Fiji Water", "Owl city", SimpleDoubleProperty(0.0)
                ),
                DownloadingTrack.newInstance(
                        Status.QUEUED, "Montana", "Owl city", SimpleDoubleProperty(2.0)
                ),
                DownloadingTrack.newInstance(
                        Status.DOWNLOADING, "Fireflies", "Owl city", SimpleDoubleProperty(20.0)
                )
        ))
    }

    @FXML
    fun onCancel() {
        downloadButton.scene.window?.hide()
    }

    @FXML
    fun onDownload() {
        downloadButton.isDisable = true
    }

    /**
     * Sets the tracks that this gui displays.
     *
     * @param tracks the tracks to display
     */
    fun setTracks(tracks: List<DownloadingTrack>) {
        table.items.setAll(tracks)
    }

    /**
     * Sets the title of the playlist.
     *
     * @param name the name of the playlist
     */
    fun setPlaylistName(name: String) {
        playlistName.text = name
    }
}