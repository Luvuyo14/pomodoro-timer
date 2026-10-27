package com.luvuyo.pomodoro

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Google answered with an error status (for example 401, 404, 409 or 429). */
class CalendarApiException(val statusCode: Int, detail: String) :
    IOException("HTTP $statusCode: $detail")

data class CalendarEventData(
    val id: String,
    val title: String,
    val description: String,
    val start: String,
    val end: String,
    val timeZoneId: String
)

/** The two Google Calendar REST calls the app needs. */
class GoogleCalendarApi {

    /** Creates a secondary calendar and returns its ID. */
    suspend fun createCalendar(
        accessToken: String,
        name: String,
        description: String,
        timeZoneId: String
    ): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("summary", name)
            .put("description", description)
            .put("timeZone", timeZoneId)
        post(accessToken, "$BASE/calendars", body).getString("id")
    }

    suspend fun insertEvent(
        accessToken: String,
        calendarId: String,
        event: CalendarEventData
    ) {
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("id", event.id)
                .put("summary", event.title)
                .put("description", event.description)
                .put("start", JSONObject().put("dateTime", event.start).put("timeZone", event.timeZoneId))
                .put("end", JSONObject().put("dateTime", event.end).put("timeZone", event.timeZoneId))
                .put("colorId", TOMATO_COLOR_ID)
                // The session is already over, so a reminder would be pointless noise.
                .put("reminders", JSONObject().put("useDefault", false))

            val encodedCalendarId = URLEncoder.encode(calendarId, "UTF-8")
            post(accessToken, "$BASE/calendars/$encodedCalendarId/events", body)
        }
    }

    private fun post(accessToken: String, url: String, body: JSONObject): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) throw CalendarApiException(code, text.take(300))
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val BASE = "https://www.googleapis.com/calendar/v3"
        const val TIMEOUT_MILLIS = 15_000
        const val TOMATO_COLOR_ID = "11" // Google Calendar's "Tomato" event colour
    }
}