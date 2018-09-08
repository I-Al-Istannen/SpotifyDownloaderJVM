package me.ialistannen.spotifydownloaderjvm.searching

import me.ialistannen.spotifydownloaderjvm.metadata.Metadata
import org.schabi.newpipe.extractor.Downloader
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
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
        youtubeService = ServiceList.YouTube
    }

    override fun findTrackUrl(metadata: Metadata): String? {
        val trackArtistName = metadata.artists[0].sanitize()
        val trackTitle = metadata.title.sanitize()

        val search = SearchInfo.getInfo(
                youtubeService,
                youtubeService.searchQHFactory.fromQuery(
                        "$trackTitle - $trackArtistName",
                        listOf(YoutubeSearchQueryHandlerFactory.VIDEOS), ""
                ),
                "de"
        )
//        val search = youtubeService.searchEngine.search(
//                "$trackTitle - $trackArtistName",
//                0,
//                "de",
//                SearchEngine.Filter.STREAM
//        )
        val results = search.relatedItems
                .filterIsInstance(StreamInfoItem::class.java)
                .filter { it.streamType == StreamType.VIDEO_STREAM }


        val filteredForTrack = results
                .filterNot { "cover" in it.name.sanitize() }
                .filterNot { "karaoke" in it.name.sanitize() }

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