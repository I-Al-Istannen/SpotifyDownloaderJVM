package me.ialistannen.spotifydownloaderjvm.searching

import me.ialistannen.spotifydownloaderjvm.metadata.Metadata
import org.schabi.newpipe.extractor.Downloader
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.search.SearchEngine
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import java.net.HttpURLConnection
import java.net.URL

class YoutubeTrackSearcher : TrackUrlSearcher {

    private val youtubeService: YoutubeService

    init {
        if (NewPipe.getDownloader() == null) {
            NewPipe.init(SimpleDownloader())
        }
        youtubeService = YoutubeService(1, "Youtube")
    }

    override fun findTrackUrl(metadata: Metadata): String? {
        val trackArtistName = metadata.artists[0].sanitize()
        val trackTitle = metadata.title.sanitize()

        val search = youtubeService.searchEngine.search(
                "$trackTitle - $trackArtistName",
                0,
                "de",
                SearchEngine.Filter.STREAM
        )
        val results = search.searchResult.resultList
                .filterIsInstance(StreamInfoItem::class.java)
                .filter { it.stream_type == StreamType.VIDEO_STREAM }

        val filteredForTrack = results
                .filter { trackTitle in it.name.sanitize() }
                .filter { trackArtistName in it.name.sanitize() }
                .sortedByDescending { it.viewCount }

        val vevoChannelUpload = results.find { "VEVO" in it.uploaderName }
        if (vevoChannelUpload != null) {
            return vevoChannelUpload.url
        }

        // TODO: Punish "Live" videos?

        return filteredForTrack.firstOrNull()?.url
    }

    private fun String.sanitize(): String {
        return toLowerCase().replace(Regex("[^a-zA-Z0-9 \\-]"), "")
    }
}

private class SimpleDownloader : Downloader {
    override fun download(siteUrl: String, language: String): String =
            download(siteUrl, mapOf("Accept-Language" to language))

    override fun download(siteUrl: String, customProperties: Map<String, String>): String {
        val connection = URL(siteUrl).openConnection() as HttpURLConnection
        for (property in customProperties) {
            connection.addRequestProperty(property.key, property.value)
        }
        return connection.inputStream.bufferedReader().readText()
    }

    override fun download(siteUrl: String): String = download(siteUrl, emptyMap())
}

fun main(args: Array<String>) {
    val trackSearcher = YoutubeTrackSearcher()

    println(trackSearcher.findTrackUrl(
            Metadata(
                    title = "Chicago",
//                    title = "Fiji Water",
                    album = "Reel 2",
                    artists = listOf("Clueso"),
//                    artists = listOf("Owl City"),
                    genre = listOf("Pop", "Electronic"),
                    albumArtUrl = "https://i.scdn.co/image/0d1ee0bdb67f225c437efd8e152655e56575d2dc",
                    releaseDate = "01.12.2017",
                    trackNumber = 2,
                    totalTrackNumber = 3
            )
    ))
}