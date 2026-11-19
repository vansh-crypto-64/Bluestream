package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "BlueStreamAuth"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun formatAuthError(error: Throwable): String {
    val parts = mutableListOf<String>()

    val apiException = generateSequence(error) { it.cause }
        .filterIsInstance<ApiException>()
        .firstOrNull()
    val firebaseAuthException = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseAuthException>()
        .firstOrNull()
    val firebaseException = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseException>()
        .firstOrNull()

    when {
        firebaseAuthException != null -> {
            parts.add("FirebaseAuthException [${firebaseAuthException.errorCode}]")
            firebaseAuthException.message?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        }
        apiException != null -> {
            parts.add("ApiException (statusCode=${apiException.statusCode})")
            apiException.statusMessage?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                ?: apiException.message?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        }
        error is GetCredentialException -> {
            parts.add("${error::class.java.simpleName} [${error.type}]")
            val detail = error.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                ?: error.message?.takeIf { it.isNotBlank() }
            if (detail != null) parts.add(detail)
        }
        firebaseException != null -> {
            parts.add(firebaseException::class.java.simpleName)
            firebaseException.message?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        }
        else -> {
            parts.add(error::class.java.simpleName)
            error.message?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        }
    }

    val cause = error.cause
    if (cause != null && cause !== apiException && cause !== firebaseAuthException) {
        val causeDesc = "${cause::class.java.simpleName}: ${cause.message ?: "unknown cause"}"
        if (parts.none { it.contains(causeDesc) }) {
            parts.add("Cause: $causeDesc")
        }
    }

    return parts.joinToString("\n").ifBlank { error.toString() }
}

/**
 * Safely checks whether Firebase was initialized (i.e., google-services.json was present
 * and valid) without throwing IllegalStateException on startup.
 */
fun isFirebaseAvailable(context: Context): Boolean {
    return runCatching {
        if (FirebaseApp.getApps(context.applicationContext).isNotEmpty()) {
            true
        } else {
            FirebaseApp.initializeApp(context.applicationContext) != null
        }
    }.getOrDefault(false)
}

fun getSafeFirebaseAuth(context: Context): FirebaseAuth? {
    if (!isFirebaseAvailable(context)) return null
    return runCatching { Firebase.auth }.getOrNull()
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val auth = getSafeFirebaseAuth(context)
    if (auth == null) {
        onUnauthenticated()
        return
    }
    if (auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
    if (clientId == null) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val activity = context.findActivity() ?: context
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val auth = getSafeFirebaseAuth(context)
    if (auth == null) {
        onAuthError("FirebaseAuth unavailable: FirebaseApp is not initialized (missing or invalid google-services.json).")
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        onAuthError("Missing default_web_client_id string resource: ${e.message}")
        return
    }
    if (clientId == null) {
        onAuthError("Google Sign-In failed: R.string.default_web_client_id is empty.")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val activity = context.findActivity()
                ?: throw IllegalStateException("Activity context required for Google Sign-In")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(cred.data)
                val token = googleIdTokenCredential.idToken
                val authResult = auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(token, null)
                ).await()
                if (authResult.user != null) {
                    onAuthSuccess()
                } else {
                    onAuthError("FirebaseAuth.signInWithCredential returned null user.")
                }
            } else {
                onAuthError("Unexpected credential type: ${cred::class.java.simpleName} (${cred.type})")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In cancelled or aborted: ${e.message}", e)
            onAuthCancelled()
            onAuthError(formatAuthError(e))
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(formatAuthError(e))
        }
    }
}

fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                runCatching { getSafeFirebaseAuth(context)?.signOut() }
                onSignOutComplete()
            }
        }
    }
}
