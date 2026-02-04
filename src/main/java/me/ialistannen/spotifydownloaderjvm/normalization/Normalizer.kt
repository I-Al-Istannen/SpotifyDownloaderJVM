package me.ialistannen.spotifydownloaderjvm.normalization

import io.reactivex.Observable
import java.nio.file.Path

/**
 * Normalizes a MP3 file.
 */
interface Normalizer {
    /**
     * Normalizes a given file.
     *
     * @param file the path to the file
     * @return an [Observable] tracking the progress
     */
    fun normalize(file: Path): Observable<Double>
}
