package com.unibud.app.data.cloud

import android.app.Application
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

data class AuthUser(val uid: String, val name: String?, val email: String?)

sealed class SignInResult {
    data class Success(val user: AuthUser) : SignInResult()
    /** google-services.json has not been added to this build yet. */
    object NotConfigured : SignInResult()
    object Cancelled : SignInResult()
    data class Failed(val message: String) : SignInResult()
}

/** Google sign-in through Android Credential Manager, exchanged for a Firebase Auth session. */
class AuthRepository(private val app: Application) {

    val isConfigured: Boolean get() = FirebaseApp.getApps(app).isNotEmpty()

    val user: Flow<AuthUser?> = callbackFlow {
        if (!isConfigured) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(activityContext: Context): SignInResult {
        if (!isConfigured) return SignInResult.NotConfigured
        // default_web_client_id is generated from google-services.json, so it is looked up by name.
        val resId = app.resources.getIdentifier("default_web_client_id", "string", app.packageName)
        if (resId == 0) return SignInResult.NotConfigured

        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(app.getString(resId))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseUser = FirebaseAuth.getInstance()
                    .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user
                firebaseUser?.toAuthUser()?.let { SignInResult.Success(it) }
                    ?: SignInResult.Failed("Google did not return an account.")
            } else {
                SignInResult.Failed("Google returned an unexpected sign-in type.")
            }
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            SignInResult.Failed("No Google account is available on this phone. Add one in Android settings and try again.")
        } catch (e: GetCredentialException) {
            SignInResult.Failed(e.message ?: "Google sign-in failed.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SignInResult.Failed(e.message ?: "Sign-in failed.")
        }
    }

    suspend fun signOut(activityContext: Context) {
        if (!isConfigured) return
        FirebaseAuth.getInstance().signOut()
        try {
            CredentialManager.create(activityContext).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Signing out of Firebase already worked, so the app is signed out either way.
        }
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(uid, displayName, email)
}
