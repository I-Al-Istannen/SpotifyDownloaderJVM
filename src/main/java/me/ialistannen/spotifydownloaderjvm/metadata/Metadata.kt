package me.ialistannen.spotifydownloaderjvm.metadata

data class Metadata(
    val title: String,
    val artists: List<String>,
    val album: String,
    val genre: List<String>,
    val albumArtUrl: String,
    val releaseDate: String,
    val trackNumber: Int,
    val totalTrackNumber: Int,
)
