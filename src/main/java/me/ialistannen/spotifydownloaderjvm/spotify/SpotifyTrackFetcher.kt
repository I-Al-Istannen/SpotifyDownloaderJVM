package me.ialistannen.spotifydownloaderjvm.spotify

import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.specification.PlaylistTrack
import io.reactivex.Observable

class SpotifyTrackFetcher(
        private val spotifyApi: SpotifyApi
) {

    /**
     * Returns all tracks in the given playlist.
     *
     * @param playlistId the id of the playlist
     * @param userId the id of the user
     */
    fun getPlaylistTracks(userId: String, playlistId: String): Observable<PlaylistTrack> {
        return spotifyApi.getAllTracksFromPlaylist(userId, playlistId)
    }

    /**
     * Returns the tracks of a playlist from its link in the following format:
     * `https://open.spotify.com/user/x40fn74nzd798rvmpy6o5vue7/playlist/5oxZIYU1L9N1CczN0C4JkM`
     * and optional garbage at the end.
     *
     * @param playlistLink the link of the playlist
     * @return the tracks in the playlist
     * @throws IllegalArgumentException if the link is invalid
     * @see [getPlaylistTracks]
     */
    fun getPlaylistTracksFromLink(playlistLink: String): Observable<PlaylistTrack> {
        val regex = Regex("user/(.+?)/playlist/(.+?)[^a-z0-9A-Z]")
        val match = regex.find(playlistLink) ?: throw IllegalArgumentException("Invalid link")
        val values = match.groupValues
        return getPlaylistTracks(values[1], values[2])
    }
}

fun main(args: Array<String>) {
    val spotifyApi = SpotifyApi.builder()
            .setClientId("***REMOVED***")
            .setClientSecret("***REMOVED***")
            .build()
    val clientCredentials = spotifyApi.clientCredentials().build().execute()
    spotifyApi.accessToken = clientCredentials.accessToken

    val trackFetcher = SpotifyTrackFetcher(spotifyApi)

    trackFetcher.getPlaylistTracks("i_al_istannen", "4aotmA6b398BWSYDvou6tv").subscribe {
        println("${it.addedAt} | ${it.track.name} - ${it.track.album.name} (${it.track.album.artists.map { it.name }})")
    }
    println()
    trackFetcher.getPlaylistTracksFromLink("https://open.spotify.com/user/i_al_istannen/playlist/4aotmA6b398BWSYDvou6tv?si=0WEYDpwASJyVZ7M31bq0Xg").subscribe {
        println("${it.addedAt} | ${it.track.name} - ${it.track.album.name} (${it.track.album.artists.map { it.name }})")
    }
}