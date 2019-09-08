package me.ialistannen.spotifydownloaderjvm.spotify

import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.specification.Playlist
import com.wrapper.spotify.model_objects.specification.PlaylistTrack
import io.reactivex.Observable

class SpotifyTrackFetcher(
    private val spotifyApi: SpotifyApi
) {

    /**
     * Returns all tracks in the given playlist.
     *
     * @param playlistId the id of the playlist
     */
    fun getPlaylistTracks(playlistId: String): Observable<PlaylistTrack> {
        return spotifyApi.getAllTracksFromPlaylist(playlistId)
    }

    /**
     * Returns the tracks of a playlist from its link in the following format:
     * `https://open.spotify.com/playlist/playlist id?si=whatever`
     * and optional garbage at the end.
     *
     * @param playlistLink the link of the playlist
     * @return the tracks in the playlist
     * @throws IllegalArgumentException if the link is invalid
     * @see [getPlaylistTracks]
     */
    fun getPlaylistTracksFromLink(playlistLink: String): Observable<PlaylistTrack> {
        return getPlaylistTracks(getPlaylistIdFromLink(playlistLink))
    }

    private fun getPlaylistIdFromLink(playlistLink: String): String {
        val regex = Regex("/playlist/(.+?)(\\?|&|\$).*")
        val match = regex.find(playlistLink) ?: throw IllegalArgumentException("Invalid link")
        val values = match.groupValues
        return values[1]
    }

    /**
     * Returns the name of the playlist.
     *
     * @param playlistId the id of the playlist
     */
    fun getPlaylistName(playlistId: String): Observable<String> {
        return spotifyApi.getPlaylist(playlistId).build().toSingle<Playlist>()
            .toObservable()
            .map {
                it.name
            }
    }

    /**
     * Returns the name of a playlist from its link in the following format:
     * `https://open.spotify.com/playlist/playlist id?si=whatever`
     * and optional garbage at the end.
     *
     * @param playlistLink the link of the playlist
     * @return the name ofd the playlist
     * @throws IllegalArgumentException if the link is invalid
     * @see [getPlaylistName]
     */
    fun getPlaylistNameFromLink(playlistLink: String): Observable<String> {
        return getPlaylistName(getPlaylistIdFromLink(playlistLink))
    }
}