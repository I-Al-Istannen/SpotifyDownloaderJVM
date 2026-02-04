package me.ialistannen.spotifydownloaderjvm.metadata

import com.mpatric.mp3agic.ID3v2
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class Mp3gicMetadataInjector : MetadataInjector {
    override fun inject(
        file: Path,
        metadata: Metadata,
    ) {
        val mp3File = Mp3File(file)

        val tag: ID3v2 =
            if (mp3File.hasId3v2Tag()) {
                mp3File.id3v2Tag
            } else {
                ID3v24Tag()
            }

        tag.title = metadata.title
        tag.album = metadata.album
        tag.artist = metadata.artists.joinToString(", ")
        tag.genreDescription = metadata.genre.joinToString(", ")
        tag.track = "${metadata.trackNumber}/${metadata.totalTrackNumber}"
        tag.date = metadata.releaseDate

        val image = downloadImage(metadata.albumArtUrl)
        tag.setAlbumImage(image.first, image.second)

        val tmpFile = Files.createTempFile("MetadataInjector", file.fileName.toString())
        mp3File.save(tmpFile.toAbsolutePath().toString())
        Files.move(tmpFile, file, StandardCopyOption.REPLACE_EXISTING)
    }

    /**
     * Returns the image and its mime type
     *
     * @param albumArtUrl the url to the image
     */
    private fun downloadImage(albumArtUrl: String): Pair<ByteArray, String> {
        val urlConnection = URL(albumArtUrl).openConnection()
        val bytes = urlConnection.getInputStream().readBytes()
        return bytes to urlConnection.contentType
    }
}

fun main() {
    Mp3gicMetadataInjector().inject(
        Paths.get("/tmp/hm.mp3"),
        Metadata(
            title = "Fiji Water",
            album = "Reel 2",
            artists = listOf("Owl City"),
            genre = listOf("Pop", "Electronic"),
            albumArtUrl = "https://i.scdn.co/image/0d1ee0bdb67f225c437efd8e152655e56575d2dc",
            releaseDate = "01.12.2017",
            trackNumber = 2,
            totalTrackNumber = 3,
        ),
    )

    val mp3File = Mp3File("/tmp/hm.mp3")
    val tag = mp3File.id3v2Tag
    for (method in tag::class.java.methods) {
        if (method.name.startsWith("get") && method.parameterCount == 0) {
            println("%-30s %-30s".format(method.name, method.invoke(tag)))
        }
    }
}
