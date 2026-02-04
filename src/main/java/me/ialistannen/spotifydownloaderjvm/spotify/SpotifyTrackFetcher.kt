package me.ialistannen.spotifydownloaderjvm.spotify

import io.reactivex.Observable
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack

class SpotifyTrackFetcher(
    private val spotifyApi: SpotifyApi,
) {
    /**
     * Returns all tracks in the given playlist.
     *
     * @param playlistId the id of the playlist
     */
    fun getPlaylistTracks(playlistId: String): Observable<PlaylistTrack> = spotifyApi.getAllTracksFromPlaylist(playlistId)

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
    fun getPlaylistTracksFromLink(playlistLink: String): Observable<PlaylistTrack> = getPlaylistTracks(getPlaylistIdFromLink(playlistLink))

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
    fun getPlaylistName(playlistId: String): Observable<String> =
        Observable
            .fromCallable {
                executeWithRetry { spotifyApi.getPlaylist(playlistId).build().execute() }
            }.map { it.name }

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
    fun getPlaylistNameFromLink(playlistLink: String): Observable<String> = getPlaylistName(getPlaylistIdFromLink(playlistLink))
}
