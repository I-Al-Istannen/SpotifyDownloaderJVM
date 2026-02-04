package me.ialistannen.spotifydownloaderjvm.metadata

import java.nio.file.Path

interface MetadataInjector {
    /**
     * Injects the given [Metadata] in the mp3 file.
     *
     * @param file the path to the file
     * @param metadata the [Metadata] to inject
     */
    fun inject(
        file: Path,
        metadata: Metadata,
    )
}
