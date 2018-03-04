package me.ialistannen.spotifydownloaderjvm.spotify

import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.specification.Track
import io.reactivex.Observable
import me.ialistannen.spotifydownloaderjvm.metadata.Metadata

class SpotifyMetadataFetcher(
        private val spotifyApi: SpotifyApi
) {

    /**
     * Returns the metadata for a given track.
     *
     * @param trackId the id of the track
     * @return an observable returning the track in the future
     */
    fun getTrackMetadata(trackId: String): Observable<Metadata> {
        return spotifyApi.getTrack(trackId).build().toSingle<Track>()
                .toObservable()
                .map {
                    val album = spotifyApi.getAlbum(it.album.id).build().execute()
                    val image = album.images.reduce { img1, img2 ->
                        if (img1.height > img2.height) {
                            img1
                        } else {
                            img2
                        }
                    }

                    Metadata(
                            title = it.name,
                            artists = it.artists.map { it.name },
                            album = it.album.name,
                            albumArtUrl = image.url,
                            trackNumber = it.trackNumber,
                            totalTrackNumber = album.tracks.total,
                            genre = album.genres.toList(),
                            releaseDate = album.releaseDate
                    )
                }
    }
}