package me.ialistannen.spotifydownloaderjvm.searching

import me.ialistannen.spotifydownloaderjvm.metadata.Metadata

interface TrackUrlSearcher {

    /**
     * Finds the url a track can be downloaded from.
     *
     * @param metadata the metadata to search for
     */
    fun findTrackUrl(metadata: Metadata): String?
}