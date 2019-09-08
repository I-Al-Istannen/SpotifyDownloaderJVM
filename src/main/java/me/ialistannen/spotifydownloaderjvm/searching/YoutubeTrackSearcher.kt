package me.ialistannen.spotifydownloaderjvm.searching

import me.ialistannen.spotifydownloaderjvm.metadata.Metadata
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.cookie.BasicClientCookie
import org.apache.http.protocol.BasicHttpContext
import org.schabi.newpipe.extractor.*
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.utils.Localization

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

private class SimpleDownloader : Downloader {

    private val client: CloseableHttpClient = HttpClientBuilder.create().build()

    override fun get(siteUrl: String, request: DownloadRequest): DownloadResponse {
        return executeToResponse(HttpGet(siteUrl), request)
    }

    override fun get(siteUrl: String): DownloadResponse = get(siteUrl, DownloadRequest.emptyRequest)

    override fun post(siteUrl: String, request: DownloadRequest): DownloadResponse {
        return executeToResponse(HttpPost(siteUrl), request)
    }

    override fun download(siteUrl: String, customProperties: Map<String, String>): String {
        return get(
            siteUrl,
            DownloadRequest("", customProperties.mapValues { listOf(it.value) })
        ).responseBody
    }

    override fun download(siteUrl: String, localization: Localization): String {
        return get(
            siteUrl,
            DownloadRequest("", mapOf("Accept-Language" to listOf(localization.language)))
        ).responseBody
    }

    override fun download(siteUrl: String): String = download(siteUrl, emptyMap())

    private fun executeToResponse(
        httpUriRequest: HttpUriRequest,
        request: DownloadRequest
    ): DownloadResponse {
        val response = executeRequest(httpUriRequest, request)
        val headers = response.allHeaders
            .groupBy { it.name }
            .mapValues { it.value.map { header -> header.value } }
        return DownloadResponse(response.entity.content.bufferedReader().readText(), headers)
    }

    private fun executeRequest(
        request: HttpUriRequest,
        downloadRequest: DownloadRequest
    ): CloseableHttpResponse {
        downloadRequest.requestHeaders.forEach {
            request.addHeader(it.key, it.value.joinToString { "," })
        }

        val localContext = BasicHttpContext()
        val cookieStore = BasicCookieStore()
        localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore)

        downloadRequest.requestCookies.forEach {
            cookieStore.addCookie(
                BasicClientCookie(it.split("=")[0], it.split("=")[1])
            )
        }

        return client.execute(request, localContext)
    }
}