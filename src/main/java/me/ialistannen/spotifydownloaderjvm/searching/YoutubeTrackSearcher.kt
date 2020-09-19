package me.ialistannen.spotifydownloaderjvm.searching

import me.ialistannen.spotifydownloaderjvm.metadata.Metadata
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.protocol.BasicHttpContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

class YoutubeTrackSearcher : TrackUrlSearcher {

    private val youtubeService: YoutubeService

    init {
        if (NewPipe.getDownloader() == null) {
            NewPipe.init(SimpleDownloader(), Localization("DE", "de"))
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
            )
        )
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

private class SimpleDownloader : Downloader() {

    private val client: CloseableHttpClient
        get() = HttpClientBuilder.create()
            .setDefaultRequestConfig(
                RequestConfig.copy(RequestConfig.DEFAULT).apply {
                    setConnectTimeout(10_000)
                    setSocketTimeout(10_000)
                    setConnectionRequestTimeout(5_000)
                }.build()
            )
            .build()

    override fun execute(request: Request): Response {
        val response = executeRequest(request)

        return Response(
            response.statusLine.statusCode,
            response.statusLine.reasonPhrase,
            response.allHeaders.map { it.name to listOf(it.value) }.toMap(),
            response.entity.content.bufferedReader().readText(),
            request.url()
        )
    }

    private fun executeRequest(
        downloadRequest: Request
    ): CloseableHttpResponse {
        val request = when (downloadRequest.httpMethod().toUpperCase()) {
            "GET" -> HttpGet(downloadRequest.url())
            else -> throw IllegalArgumentException("unknown method")
        }
        downloadRequest.headers().forEach {
            request.addHeader(it.key, it.value.joinToString(","))
        }

        // Not needed, but let's throw it in
        if ("User-Agent" !in downloadRequest.headers()) {
            request.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64)")
        }

        val localContext = BasicHttpContext()
        val cookieStore = BasicCookieStore()
        localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore)

        return client.execute(request, localContext)
    }
}
