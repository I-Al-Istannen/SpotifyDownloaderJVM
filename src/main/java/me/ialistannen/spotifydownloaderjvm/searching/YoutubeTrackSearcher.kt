package me.ialistannen.spotifydownloaderjvm.searching

import me.ialistannen.spotifydownloaderjvm.metadata.Metadata
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import java.util.*
import java.util.concurrent.TimeUnit


class YoutubeTrackSearcher : TrackUrlSearcher {

    private val youtubeService: YoutubeService
    private val downloader: DownloaderTestImpl = DownloaderTestImpl(
        OkHttpClient.Builder().connectionPool(ConnectionPool(2, 5, TimeUnit.SECONDS))
    )

    init {
        if (NewPipe.getDownloader() == null) {
            NewPipe.init(downloader, Localization("DE", "de"))
        }
        youtubeService = ServiceList.YouTube
    }

    override fun findTrackUrl(metadata: Metadata): String? {
        val trackArtistName = metadata.artists[0].sanitize()
        val trackTitle = metadata.title.sanitize()

        val search = SearchInfo.getInfo(
            youtubeService,
            youtubeService.searchQHFactory.fromQuery(
                "$trackTitle - $trackArtistName"
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
        return lowercase(Locale.getDefault()).replace(Regex("[^a-zA-Z\\d \\-]"), "")
    }
}

private const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; rv:78.0) Gecko/20100101 Firefox/78.0"

class DownloaderTestImpl(builder: OkHttpClient.Builder) : Downloader() {
    private val client: OkHttpClient = builder.readTimeout(30, TimeUnit.SECONDS).build()

    override fun execute(request: Request): Response {
        val okhttpRequest = buildRequest(request)
        val response: okhttp3.Response = client.newCall(okhttpRequest).execute()

        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", request.url())
        }

        val body = response.body?.string()
        val latestUrl: String = response.request.url.toString()
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            body,
            latestUrl
        )
    }

    private fun buildRequest(request: Request): okhttp3.Request {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()
        val requestBody = dataToSend?.toRequestBody(null, 0)

        val requestBuilder: Builder = Builder()
            .method(httpMethod, requestBody).url(url)
            .addHeader("User-Agent", USER_AGENT)

        for ((headerName, headerValueList) in headers) {
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                for (headerValue in headerValueList) {
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }
        return requestBuilder.build()
    }
}

fun main() {
    val searcher = YoutubeTrackSearcher()
    val res = searcher.findTrackUrl(
        Metadata(
            "Kuscheln, Sex und Händchenhalten", listOf("MAYBEBOP"), "", listOf(), "", "", 0, 0
        )
    )
    println("Result")
    println(res)
    println("Done")
}
