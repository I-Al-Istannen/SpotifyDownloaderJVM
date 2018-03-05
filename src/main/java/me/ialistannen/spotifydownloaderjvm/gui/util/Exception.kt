package me.ialistannen.spotifydownloaderjvm.gui.util

import java.io.PrintWriter
import java.io.StringWriter

fun Throwable.getStackTraceString(): String {
    val writer = StringWriter()
    val printWriter = PrintWriter(writer)
    printStackTrace(printWriter)
    return writer.toString()
}
