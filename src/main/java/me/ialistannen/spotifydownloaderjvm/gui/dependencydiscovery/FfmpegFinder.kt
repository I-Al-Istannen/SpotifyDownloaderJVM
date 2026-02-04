package me.ialistannen.spotifydownloaderjvm.gui.dependencydiscovery

import io.reactivex.Maybe
import me.ialistannen.spotifydownloaderjvm.gui.util.findExecutable
import me.ialistannen.spotifydownloaderjvm.gui.util.getInitialFolder
import java.nio.file.Path

/**
 * Searches for FFMPEG/FFPROBE.
 */
class FfmpegFinder {
    /**
     * Searches for FFMPEG on the PATH/in this folder.
     *
     * @return the path to ffmpeg, if found
     */
    fun findFfmpeg(): Maybe<Path> = findExecutable("ffmpeg", getInitialFolder(FfmpegFinder::class.java))

    /**
     * Searches for FFPROBE on the PATH/in this folder.
     *
     * @return the path to ffprobe, if found
     */
    fun findFfprobe(): Maybe<Path> = findExecutable("ffprobe", getInitialFolder(FfmpegFinder::class.java))
}
