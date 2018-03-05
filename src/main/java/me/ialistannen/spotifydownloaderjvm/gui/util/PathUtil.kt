package me.ialistannen.spotifydownloaderjvm.gui.util

import io.reactivex.Maybe
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes


/**
 * Returns the initial folder this code was loaded from.
 *
 * @return the folder this was loaded from
 */
fun getInitialFolder(clazz: Class<*>): Path {
    return Paths.get(clazz.protectionDomain.codeSource.location.toURI()).parent
}

/**
 * Finds an executable starting with the given name in the given initial folder (infitely deep)
 * OR on the path (2 folders deep, max).
 *
 * @param nameStart the start of its name, case sensitive
 * @param initialFolder the initial folder to search in
 */
fun findExecutable(nameStart: String, initialFolder: Path): Maybe<Path> {
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

/**
 * Finds a file starting with the given name in a folder.
 *
 * @param folder the folder to look inside
 * @param nameStart the start of its name, case sensitive
 * @param depth the maximum depth to traverse
 */
fun findInFolder(folder: Path, nameStart: String, depth: Int): Path? {
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