package me.ialistannen.spotifydownloaderjvm.spotify

import io.reactivex.Observable
import io.reactivex.Single
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack
import se.michaelthelin.spotify.requests.IRequest

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
fun <T> IRequest<T>.toSingle(): Single<T> {
    return Single.fromCallable { this.execute() }
}

/**
 * Checks if this string roughly follows this format.
 * `https://open.spotify.com/playlist/playlist id?si=whatever`
 * and optional garbage at the end.
 *
 * @return true if ti follows the format roughly
 */
fun String.isValidPlaylistLink(): Boolean {
    val regex = Regex("/playlist/(.+?)[^a-z\\dA-Z]")
    return regex.find(this) != null
}


/**
 * Returns all tracks in a playlist.
 *
 * @param playlistId the id of the playlist
 * @return all tracks in this playlist
 */
fun SpotifyApi.getAllTracksFromPlaylist(playlistId: String): Observable<PlaylistTrack> {
    return Observable.create { observable ->
        var currentOffset = 1

        while (true) {
            val tracksRequest = getPlaylistsItems(playlistId)
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
