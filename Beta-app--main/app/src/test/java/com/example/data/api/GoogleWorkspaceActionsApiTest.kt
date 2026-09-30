package com.example.data.api

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GoogleWorkspaceActionsApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: GoogleWorkspaceActionsApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val base = server.url("/").toString()
        api = GoogleWorkspaceActionsApi(
            client = OkHttpClient(),
            gmailBaseUrl = base,
            calendarBaseUrl = base,
            driveBaseUrl = base
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun unconfirmedEmailSendMakesNoRequest() {
        assertThrows(IllegalStateException::class.java) {
            api.sendEmail("test-token", "person@example.com", "Subject", "Body", confirmed = false)
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun sendEmailPostsBase64UrlMimeWithBearerToken() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"id\":\"sent-id\"}"))

        val result = api.sendEmail("test-token", "person@example.com", "Hello", "Message body", confirmed = true)

        assertEquals(200, result.statusCode)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/gmail/v1/users/me/messages/send", request.path)
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
        val raw = JSONObject(request.body.readUtf8()).getString("raw")
        val mime = String(Base64.decode(raw, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)
        assertTrue(mime.contains("To: person@example.com"))
        assertTrue(mime.contains(Base64.encodeToString("Hello".toByteArray(), Base64.NO_WRAP)))
        val encodedText = mime.substringAfter("\r\n\r\n")
        assertEquals("Message body", String(Base64.decode(encodedText, Base64.NO_WRAP), Charsets.UTF_8))
    }

    @Test
    fun trashEmailPostsToReversibleTrashEndpoint() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"id\":\"message-1\",\"labelIds\":[\"TRASH\"]}"))

        api.trashEmail("test-token", "message-1", confirmed = true)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/gmail/v1/users/me/messages/message-1/trash", request.path)
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
    }

    @Test
    fun permanentlyDeleteEmailRequiresConfirmationAndUsesDeleteEndpoint() {
        assertThrows(IllegalStateException::class.java) {
            api.deleteEmailPermanently("test-token", "message-1", confirmed = false)
        }
        assertEquals(0, server.requestCount)
        server.enqueue(MockResponse().setResponseCode(204))

        api.deleteEmailPermanently("test-token", "message-1", confirmed = true)

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/gmail/v1/users/me/messages/message-1", request.path)
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
    }

    @Test
    fun createCalendarEventPostsRequestedDetails() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"id\":\"event-1\"}"))

        api.createCalendarEvent(
            accessToken = "test-token",
            summary = "Test event",
            startDateTime = "2026-10-01T10:00:00-06:00",
            endDateTime = "2026-10-01T10:30:00-06:00",
            timeZone = "America/Costa_Rica",
            description = "Unit test",
            confirmed = true
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/calendar/v3/calendars/primary/events", request.path)
        val json = JSONObject(request.body.readUtf8())
        assertEquals("Test event", json.getString("summary"))
        assertEquals("America/Costa_Rica", json.getJSONObject("start").getString("timeZone"))
        assertEquals("Unit test", json.getString("description"))
    }

    @Test
    fun deleteCalendarEventRequiresConfirmationAndUsesEventId() {
        server.enqueue(MockResponse().setResponseCode(204))

        api.deleteCalendarEvent("test-token", "event-1", confirmed = true)

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/calendar/v3/calendars/primary/events/event-1", request.path)
    }

    @Test
    fun uploadDriveFileSendsMultipartMetadataAndContent() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"id\":\"file-1\",\"name\":\"note.txt\"}"))

        api.uploadDriveFile(
            accessToken = "test-token",
            fileName = "note.txt",
            mimeType = "text/plain",
            bytes = "hello".toByteArray(),
            confirmed = true
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.startsWith("/upload/drive/v3/files?"))
        assertTrue(request.getHeader("Content-Type")!!.contains("multipart/related"))
        val multipart = request.body.readUtf8()
        assertTrue(multipart.contains("note.txt"))
        assertTrue(multipart.contains("hello"))
    }

    @Test
    fun permanentlyDeleteDriveFileRequiresConfirmationAndUsesDeleteEndpoint() {
        assertThrows(IllegalStateException::class.java) {
            api.deleteDriveFilePermanently("test-token", "file-1", confirmed = false)
        }
        assertEquals(0, server.requestCount)
        server.enqueue(MockResponse().setResponseCode(204))

        api.deleteDriveFilePermanently("test-token", "file-1", confirmed = true)

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/drive/v3/files/file-1", request.path)
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
    }

    @Test
    fun trashDriveFileUsesReversibleUpdateAndRequiresConfirmation() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"id\":\"file-1\",\"trashed\":true}"))

        api.trashDriveFile("test-token", "file-1", confirmed = true)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertTrue(request.path!!.startsWith("/drive/v3/files/file-1?"))
        assertTrue(JSONObject(request.body.readUtf8()).getBoolean("trashed"))
        assertFalse(request.getHeader("Authorization").isNullOrBlank())
    }
}
