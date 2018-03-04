package me.ialistannen.spotifydownloaderjvm.spotify

import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.specification.PlaylistTrack
import com.wrapper.spotify.requests.IRequest
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Creates a [SpotifyApi] from the given client credentials.
 *
 * @param clientId the client id
 * @param clientSecret the client secret
 */
fun createSpotifyApiFromClientCredentials(clientSecret: String, clientId: String): SpotifyApi {
    val api = SpotifyApi.builder().setClientId(clientId).setClientSecret(clientSecret).build()

    val credentials = api.clientCredentials().build().execute()
    api.accessToken = credentials.accessToken

    return api
}

/**
 * Converts an [IRequest] to a [Single].
 */
fun <T> IRequest.toSingle(): Single<T> {
    return Single.fromCallable { this.execute() as T }
}

/**
 * Checks if this string roughly follows this format.
 * `https://open.spotify.com/user/x40fn74nzd798rvmpy6o5vue7/playlist/5oxZIYU1L9N1CczN0C4JkM`
 * and optional garbage at the end.
 *
 * @return true if ti follows the format roughly
 */
fun String.isValidPlaylistLink(): Boolean {
    val regex = Regex("user/(.+?)/playlist/(.+?)[^a-z0-9A-Z]")
    return regex.find(this) != null
}


/**
 * Returns all tracks in a playlist.
 *
 * @param userId the id of the user
 * @param playlistId the id of the playlist
 * @return all tracks in this playlist
 */
fun SpotifyApi.getAllTracksFromPlaylist(userId: String, playlistId: String): Observable<PlaylistTrack> {
    return Observable.create { observable ->
        var currentOffset = 1

        while (true) {
            val tracksRequest = getPlaylistsTracks(userId, playlistId)
                    .offset(currentOffset - 1)
                    .build()
            val paging = tracksRequest.execute()

            if (!observable.isDisposed) {
                paging.items.forEach { observable.onNext(it) }
            }

            currentOffset += paging.items.size

            if (currentOffset >= paging.total) {
                break
            }
        }

        if (!observable.isDisposed) {
            observable.onComplete()
        }
    }
}