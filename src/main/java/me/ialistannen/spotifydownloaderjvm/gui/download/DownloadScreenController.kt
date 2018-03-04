package me.ialistannen.spotifydownloaderjvm.gui.download

import javafx.fxml.FXML
import javafx.scene.control.*
import me.ialistannen.spotifydownloaderjvm.gui.model.DownloadingTrack
import me.ialistannen.spotifydownloaderjvm.gui.model.Status
import kotlin.math.roundToInt

class DownloadScreenController {

    @FXML
    private lateinit var table: TableView<DownloadingTrack>

    @FXML
    private lateinit var downloadButton: Button

    @FXML
    private lateinit var playlistName: Label

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

                        text = (item.toDouble() * 100).roundToInt().toString()
                    }
                }
            })
        }

        table.columns.setAll(nameColumn, artistColumn, statusColumn, progressColumn)
    }

    @FXML
    fun onCancel() {
        downloader.stopDownload()
        downloadButton.scene.window?.hide()
    }

    @FXML
    fun onDownload() {
        downloadButton.isDisable = true
        downloader.startDownload(table.items)
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