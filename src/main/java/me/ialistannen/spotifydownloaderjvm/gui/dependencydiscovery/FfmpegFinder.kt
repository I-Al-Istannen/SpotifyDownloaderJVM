package me.ialistannen.spotifydownloaderjvm.gui.dependencydiscovery

import io.reactivex.Maybe
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

/**
 * Searches for FFMPEG/FFPROBE.
 */
class FfmpegFinder {

    /**
     * Searches for FFMPEG on the PATH/in this folder.
     *
     * @return the path to ffmpeg, if found
     */
    fun findFfmpeg(): Maybe<Path> {
        return findExecutable("ffmpeg", getInitialFolder())
    }

    /**
     * Searches for FFPROBE on the PATH/in this folder.
     *
     * @return the path to ffprobe, if found
     */
    fun findFfprobe(): Maybe<Path> {
        return findExecutable("ffprobe", getInitialFolder())
    }

    private fun getInitialFolder(): Path {
        return Paths.get(javaClass.protectionDomain.codeSource.location.toURI()).parent
    }

    private fun findExecutable(nameStart: String, initialFolder: Path): Maybe<Path> {
        return Maybe.create { emitter ->
            val initialResult: Path? = findInFolder(initialFolder, nameStart, Integer.MAX_VALUE)

            if (initialResult != null) {
                emitter.onSuccess(initialResult)
                return@create
            }

            val path = System.getenv("PATH")

            val filesOnPath = if (path.contains(":")) {
                path.split(":")
            } else {
                path.split(";")
            }

            filesOnPath.map { Paths.get(it) }
                    .filter { Files.exists(it) }
                    .forEach {
                        if (Files.isRegularFile(it)) {
                            if (it.fileName.startsWith(nameStart)) {
                                emitter.onSuccess(it)
                            }
                        } else {
                            val result = findInFolder(it, nameStart, 2)
                            if (result != null) {
                                emitter.onSuccess(result)
                            }
                        }
                    }

            emitter.onComplete()
        }

    }

    private fun findInFolder(folder: Path, nameStart: String, depth: Int): Path? {
        if (!Files.isDirectory(folder)) {
            return null
        }
        var found: Path? = null
        Files.walkFileTree(folder, emptySet(), depth, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (!file.fileName.toString().startsWith(nameStart)) {
                    return FileVisitResult.CONTINUE
                }

                if (Files.isExecutable(file)) {
                    found = file
                    return FileVisitResult.TERMINATE
                }

                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                exc.printStackTrace()
                return FileVisitResult.CONTINUE
            }
        })

        return found
    }
}