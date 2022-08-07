package me.ialistannen.spotifydownloaderjvm.gui.download

import com.jfoenix.controls.JFXButton
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.text.Font
import javafx.util.Callback
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
            cellFactory = Callback {
                object : TableCell<DownloadingTrack, Status>() {
                    override fun updateItem(item: Status?, empty: Boolean) {
                        super.updateItem(item, empty)

                        if (item == null || empty) {
                            graphic = null
                            text = ""
                            tableRow?.style = null
                            return
                        }

                        text = item.name

                        tableRow?.style = item.cssStyle
                    }
                }
            }
        }

        val progressColumn = TableColumn<DownloadingTrack, Number>("Progress").apply {
            setCellValueFactory { it.value.progress }
            setCellFactory {
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
            }
        }
        val errorColumn = TableColumn<DownloadingTrack, String>("Error").apply {
            setCellValueFactory { it.value.error }
            setCellFactory {
                object : TableCell<DownloadingTrack, String>() {
                    override fun updateItem(item: String?, empty: Boolean) {
                        super.updateItem(item, empty)

                        if (item == null || empty) {
                            text = ""
                            graphic = null
                            return
                        }

                        if (item.isBlank()) {
                            graphic = null
                            return
                        }

                        graphic = JFXButton("Show").apply {
                            setOnAction {
                                val alert = Alert(Alert.AlertType.ERROR)
                                alert.title = "Error message"
                                alert.headerText = "An error occurred"
                                alert.dialogPane.expandableContent = TextArea(item).apply {
                                    font = Font.font("monospace", 13.0)
                                }
                                alert.show()
                            }
                        }
                    }
                }
            }
        }

        table.columns.setAll(nameColumn, artistColumn, statusColumn, progressColumn, errorColumn)
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
