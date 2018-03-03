package me.ialistannen.spotifydownloaderjvm.normalization

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Observable
import io.reactivex.Observable.create
import me.ialistannen.spotifydownloaderjvm.normalization.FfmpegWrapper.FfmpegFirstPassProgress.Finished
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

class FfmpegWrapper(
        private val ffmpegPath: Path,
        private val ffprobePath: Path,
        private val targetTruePeak: Double = -1.5,
        private val targetIntegratedLoudness: Double = -16.0,
        private val targetLoudnessRangeTarget: Double = 11.0
) {

    companion object {
        val TIME_EXTRACT_PATTERN = Regex("(\\d{2}):(\\d{2}):(\\d{2}).(\\d{2})")
    }

    /**
     *Applies the loudnorm filter to the given file.
     *
     * @param input the file to apply it to
     * @return an [Observable] that returns the progress
     */
    fun applyLoudNorm(input: Path): Observable<Double> {
        return loudnormFirstPass(input).flatMap {
            when (it) {
                is FfmpegFirstPassProgress.FirstPassProgress -> {
                    return@flatMap Observable.just(it.progress / 2)
                }
                is Finished -> {
                    return@flatMap loudnormSecondPass(input, it).map { it / 2 + 0.5 }
                }
            }
        }
    }

    /**
     * Runs the first pass of the loudnorm filter.
     *
     * @param input the path to the input mp3
     * @return an [Observable] that returns the process
     */
    fun loudnormFirstPass(input: Path): Observable<FfmpegFirstPassProgress> {
        return create {
            val durationMs = getDurationMs(input)
            if (durationMs == null) {
                it.onError(RuntimeException("Could not extract mp3 duration."))
                return@create
            }

            val filterArguments = "loudnorm=" +
                    "I=$targetIntegratedLoudness" +
                    ":TP=$targetTruePeak" +
                    ":LRA=$targetLoudnessRangeTarget" +
                    ":print_format=json"

            val command = listOf(
                    ffmpegPath.toAbsolutePath().toString(),
                    "-i", input.toAbsolutePath().toString(),
                    "-af", filterArguments,
                    "-codec:a", "libmp3lame",  // nice codec
                    "-q:a", "2",  // quality
                    "-f", "null", "-"
            )
            val process = ProcessBuilder(command).start()

            val readOutput = arrayListOf<String>()
            // error = output
            process.errorStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val extractTime = line.extractTime()

                    if (extractTime != null) {
                        if (!it.isDisposed) {
                            it.onNext(FfmpegFirstPassProgress.FirstPassProgress(
                                    extractTime.toDouble() / durationMs
                            ))
                        }
                    } else {
                        readOutput.add(line)
                    }
                }
            }

            val jsonStart = readOutput.indexOfLast { "{" in it }
            val json = readOutput.subList(jsonStart, readOutput.size).joinToString("\n")

            val objectMapper = ObjectMapper()
            val finished = objectMapper.readValue(json, Finished::class.java)

            it.onNext(finished)
            it.onComplete()
        }
    }

    private fun getDurationMs(file: Path): Long? {
        val process = ProcessBuilder(listOf(
                ffprobePath.toAbsolutePath().toString(),
                file.toAbsolutePath().toString()
        )).start()

        return process.errorStream.bufferedReader().readText().extractTime()
    }

    private fun String.extractTime(): Long? {
        val matchResult = TIME_EXTRACT_PATTERN.find(this) ?: return null
        val groups = matchResult.groupValues

        var time = TimeUnit.HOURS.toMillis(groups[1].toLong())
        time += TimeUnit.MINUTES.toMillis(groups[2].toLong())
        time += TimeUnit.SECONDS.toMillis(groups[3].toLong())
        time += groups[4].toLong()

        return time
    }

    /**
     * Runs the second pass of the loudnorm filter.
     *
     * @param input the path to the input mp3
     * @param measured the measured values
     * @return an [Observable] that returns the progress
     */
    fun loudnormSecondPass(input: Path, measured: Finished): Observable<Double> {
        return create {
            val tempFile = Files.createTempFile("FFmpegWrapper", input.fileName.toString())
            val durationMs = getDurationMs(input)!!

            val filterAguments = listOf(
                    "loudnorm=" +
                            "I=$targetIntegratedLoudness" +
                            ":TP=$targetTruePeak" +
                            ":LRA=$targetLoudnessRangeTarget",
                    "measured_I=${measured.integratedLoudness}" +
                            ":measured_TP=${measured.truePeak}" +
                            ":measured_LRA=${measured.loudnessRangeTarget}",
                    "measured_thresh=${measured.threshold}:offset=${measured.offset}",
                    "linear=true:print_format=summary"
            ).joinToString(":")

            val command = listOf(
                    ffmpegPath.toAbsolutePath().toString(),
                    "-y",
                    "-i", input.toAbsolutePath().toString(),
                    "-af", filterAguments,
                    tempFile.toAbsolutePath().toString()
            )

            val process = ProcessBuilder(command).start()

            // error = output
            process.errorStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val extractTime = line.extractTime()

                    if (extractTime != null) {
                        if (!it.isDisposed) {
                            it.onNext(extractTime.toDouble() / durationMs)
                        }
                    }
                }
            }

            Files.move(tempFile, input, StandardCopyOption.REPLACE_EXISTING)

            it.onComplete()
        }
    }

    sealed class FfmpegFirstPassProgress {
        data class FirstPassProgress(val progress: Double) : FfmpegFirstPassProgress()
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Finished(
                @JsonProperty("input_i")
                val integratedLoudness: Double,
                @JsonProperty("input_lra")
                val loudnessRangeTarget: Double,
                @JsonProperty("input_tp")
                val truePeak: Double,
                @JsonProperty("input_thresh")
                val threshold: Double,
                @JsonProperty("target_offset")
                val offset: Double
        ) : FfmpegFirstPassProgress()
    }
}