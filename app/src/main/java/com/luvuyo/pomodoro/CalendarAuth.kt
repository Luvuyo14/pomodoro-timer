package com.luvuyo.pomodoro

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Asks Google for permission to use the app's own calendar, and returns short-lived tokens. */
object CalendarAuth {

    /** Create a calendar for this app and manage events only on calendars the app created. */
    const val SCOPE_URL = "https://www.googleapis.com/auth/calendar.app.created"

    private fun request(): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SCOPE_URL)))
            .build()

    /**
     * Returns a fresh access token if permission was already granted. If the user still has
     * to approve something, the result has a resolution (a consent screen to show).
     */
    suspend fun authorize(context: Context): AuthorizationResult =
        Identity.getAuthorizationClient(context).authorize(request()).awaitResult()

    /** Reads the result after the consent screen closes. Throws ApiException on failure. */
    fun resultFromIntent(context: Context, data: Intent): AuthorizationResult =
        Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)

    fun hasCalendarAccess(result: AuthorizationResult): Boolean =
        result.grantedScopes.contains(SCOPE_URL)
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}