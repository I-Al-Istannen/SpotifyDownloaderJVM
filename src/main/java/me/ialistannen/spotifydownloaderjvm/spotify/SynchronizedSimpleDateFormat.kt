package me.ialistannen.spotifydownloaderjvm.spotify

import com.wrapper.spotify.SpotifyApi
import java.lang.reflect.Modifier
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*

class SynchronizedSimpleDateFormat(pattern: String) : SimpleDateFormat(pattern) {

    companion object {

        fun injectIntoSpotify() {
            val field = SpotifyApi::class.java.getField("SIMPLE_DATE_FORMAT")
            field.isAccessible = true
            val modifiesField = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
            modifiesField.isAccessible = true
            modifiesField.setInt(field, field.modifiers and (Modifier.FINAL.inv()))

            val simpleDateFormat = SynchronizedSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            simpleDateFormat.timeZone = TimeZone.getTimeZone("gmt")

            field.set(null, simpleDateFormat)
        }
    }

    override fun parse(source: String?): Date {
        synchronized(this) {
            return super.parse(source)
        }
    }

    override fun parse(text: String?, pos: ParsePosition?): Date {
        synchronized(this) {
            return super.parse(text, pos)
        }
    }

    override fun parseObject(source: String?): Any {
        synchronized(this) {
            return super.parseObject(source)
        }
    }

    override fun parseObject(source: String?, pos: ParsePosition?): Any {
        synchronized(this) {
            return super.parseObject(source, pos)
        }
    }
}