package com.example.data.api

import android.util.Base64
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Direct Google Workspace REST actions for the isolated test build.
 * Call only after showing the full action preview and receiving an explicit user confirmation.
 * OAuth scopes are deliberately not requested here; the existing production review stays read-only.
 */
class GoogleWorkspaceActionsApi(
    private val client: OkHttpClient = OkHttpClient(),
    gmailBaseUrl: String = "https://gmail.googleapis.com/",
    calendarBaseUrl: String = "https://www.googleapis.com/",
    driveBaseUrl: String = "https://www.googleapis.com/"
) {
    private val gmailBase = gmailBaseUrl.toHttpUrl()
    private val calendarBase = calendarBaseUrl.toHttpUrl()
    private val driveBase = driveBaseUrl.toHttpUrl()

    /** Send one plain-text email. A visible confirmation is required before this call. */
    @Throws(IOException::class)
    fun sendEmail(
        accessToken: String,
        recipient: String,
        subject: String,
        body: String,
        confirmed: Boolean
    ): GoogleWorkspaceApiResult {
        requireConfirmed(confirmed)
        requireSafeHeader(recipient, "recipient")
        requireSafeHeader(subject, "subject")
        requireToken(accessToken)
        require(recipient.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) { "Invalid recipient address." }

        val encodedSubject = Base64.encodeToString(
            subject.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )
        val encodedBody = Base64.encodeToString(
            body.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )
        val mime = buildString {
            append("To: ").append(recipient).append("\r\n")
            append("Subject: =?UTF-8?B?").append(encodedSubject).append("?=\r\n")
            append("MIME-Version: 1.0\r\n")
            append("Content-Type: text/plain; charset=UTF-8\r\n")
            append("Content-Transfer-Encoding: base64\r\n\r\n")
            append(encodedBody)
        }
        val raw = Base64.encodeToString(
            mime.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val json = JSONObject().put("raw", raw).toString()
        val url = url(gmailBase, "gmail", "v1", "users", "me", "messages", "send")
        return execute("POST", url, accessToken, json.toRequestBody(JSON))
    }

    /** Move a Gmail message to Trash; this is reversible in Gmail. */
    @Throws(IOException::class)
    fun trashEmail(accessToken: String, messageId: String, confirmed: Boolean): GoogleWorkspaceApiResult {
        requireConfirmed(confirmed)
        requireToken(accessToken)
        requireId(messageId, "messageId")
        val url = url(gmailBase, "gmail", "v1", "users", "me", "messages", messageId, "trash")
        return execute("POST", url, accessToken, EMPTY_BODY)
    }

    /** Create a timed event on the signed-in user's primary calendar. */
    @Throws(IOException::class)
    fun createCalendarEvent(
        accessToken: String,
        summary: String,
        startDateTime: String,
        endDateTime: String,
        timeZone: String,
        description: String? = null,
        confirmed: Boolean
    ): GoogleWorkspaceApiResult {
        requireConfirmed(confirmed)
        requireToken(accessToken)
        require(summary.isNotBlank()) { "Event title is required." }
        require(startDateTime.isNotBlank() && endDateTime.isNotBlank()) { "Event start and end are required." }
        require(timeZone.isNotBlank()) { "Event time zone is required." }

        val start = JSONObject().put("dateTime", startDateTime).put("timeZone", timeZone)
        val end = JSONObject().put("dateTime", endDateTime).put("timeZone", timeZone)
        val event = JSONObject()
            .put("summary", summary)
            .put("start", start)
            .put("end", end)
        if (!description.isNullOrBlank()) event.put("description", description)
        val url = url(calendarBase, "calendar", "v3", "calendars", "primary", "events")
        return execute("POST", url, accessToken, event.toString().toRequestBody(JSON))
    }

    /** Permanently delete a calendar event. The UI must display the event and request confirmation. */
    @Throws(IOException::class)
    fun deleteCalendarEvent(accessToken: String, eventId: String, confirmed: Boolean): GoogleWorkspaceApiResult {
        requireConfirmed(confirmed)
        requireToken(accessToken)
        requireId(eventId, "eventId")
        val url = url(calendarBase, "calendar", "v3", "calendars", "primary", "events", eventId)
        return execute("DELETE", url, accessToken, null)
    }

    /** Upload a file to Drive using multipart/related. Limited to 10 MiB per request. */
    @Throws(IOException::class)
    fun uploadDriveFile(
        accessToken: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        confirmed: Boolean
    ): GoogleWorkspaceApiResult {
        requireConfirmed(confirmed)
        requireToken(accessToken)
        require(fileName.isNotBlank()) { "File name is required." }
        require(mimeType.matches(Regex("^[A-Za-z0-9.+-]+/[A-Za-z0-9.+-]+$"))) { "Invalid MIME type." }
        require(bytes.size <= MAX_UPLOAD_BYTES) { "Files must be 10 MiB or smaller for this test action." }

        val metadata = JSONObject()
            .put("name", fileName)
            .put("mimeType", mimeType)
        val multipart = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(
                Headers.Builder().add("Content-Type", "application/json; charset=UTF-8").build(),
                metadata.toString().toRequestBody(JSON)
            )
            .addPart(
                Headers.Builder().add("Content-Type", mimeType).build(),
                bytes.toRequestBody(mimeType.toMediaType())
            )
            .build()
        val url = url(driveBase, "upload", "drive", "v3", "files")
            .newBuilder()
            .addQueryParameter("uploadType", "multipart")
            .addQueryParameter("fields", "id,name,mimeType,webViewLink")
            .build()
        return execute("POST", url, accessToken, multipart)
    }

    /** Move a Drive file to Trash; this is reversible and limited by the granted Drive scope. */
    @Throws(IOException::class)
    fun trashDriveFile(accessToken: String, fileId: String, confirmed: Boolean): GoogleWorkspaceApiResult {
        requireConfirmed(confirmed)
        requireToken(accessToken)
        requireId(fileId, "fileId")
        val url = url(driveBase, "drive", "v3", "files", fileId)
            .newBuilder()
            .addQueryParameter("fields", "id,trashed")
            .build()
        val body = JSONObject().put("trashed", true).toString().toRequestBody(JSON)
        return execute("PATCH", url, accessToken, body)
    }

    private fun execute(
        method: String,
        url: HttpUrl,
        accessToken: String,
        body: RequestBody?
    ): GoogleWorkspaceApiResult {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .method(method, body)
            .build()
        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw GoogleWorkspaceApiException(response.code, responseText.take(MAX_ERROR_CHARS))
            }
            return GoogleWorkspaceApiResult(response.code, responseText)
        }
    }

    private fun url(base: HttpUrl, vararg segments: String): HttpUrl {
        val builder = base.newBuilder()
        segments.forEach { segment -> builder.addPathSegment(segment) }
        return builder.build()
    }

    private fun requireConfirmed(confirmed: Boolean) {
        check(confirmed) { "Explicit user confirmation is required before a Google Workspace write action." }
    }

    private fun requireToken(token: String) {
        require(token.isNotBlank() && !token.contains('\n') && !token.contains('\r')) { "A valid Google access token is required." }
    }

    private fun requireId(id: String, name: String) {
        require(id.isNotBlank() && !id.contains('\n') && !id.contains('\r')) { "$name is required." }
    }

    private fun requireSafeHeader(value: String, name: String) {
        require(!value.contains('\n') && !value.contains('\r')) { "$name must not contain line breaks." }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val EMPTY_BODY = ByteArray(0).toRequestBody(null)
        private const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024
        private const val MAX_ERROR_CHARS = 1200
    }
}

data class GoogleWorkspaceApiResult(val statusCode: Int, val body: String)

class GoogleWorkspaceApiException(val statusCode: Int, val responseBody: String) :
    IOException("Google Workspace API returned HTTP $statusCode")
