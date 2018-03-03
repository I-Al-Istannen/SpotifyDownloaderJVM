package me.ialistannen.spotifydownloaderjvm.downloading

import io.reactivex.Observable
import java.nio.file.Path

interface Downloader {

    /**
     * Checks if this Downloader can download the given url.
     *
     * @param url the url to download
     * @return true if this downloader can download the url
     */
    fun canDownloader(url: String): Boolean

    /**
     * Downloads the file.
     *
     * @param url the url to download it from
     * @param path the file to save it as
     * @return an [Observable] showing the progress
     */
    fun download(url: String, path: Path): Observable<Double>
}