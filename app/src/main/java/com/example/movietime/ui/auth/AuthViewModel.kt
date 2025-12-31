package com.example.movietime.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movietime.data.firebase.FirebaseRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "AuthViewModel"
        // You need to replace this with your actual Web Client ID from Firebase Console
        const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
    }
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    val isLoggedIn: Boolean
        get() = firebaseRepository.isLoggedIn
    
    val currentUser: FirebaseUser?
        get() = firebaseRepository.currentUser
    
    init {
        checkAuthState()
    }
    
    private fun checkAuthState() {
        viewModelScope.launch {
            firebaseRepository.observeAuthState().collect { user ->
                _authState.value = if (user != null) {
                    AuthState.Authenticated(user)
                } else {
                    AuthState.Unauthenticated
                }
            }
        }
    }
    
    // Google Sign-In using Credential Manager
    suspend fun signInWithGoogle(context: Context): Result<Unit> {
        _isLoading.value = true
        
        return try {
            val credentialManager = CredentialManager.create(context)
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            val result = credentialManager.getCredential(context, request)
            handleSignInResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            _error.postValue("Помилка входу: ${e.localizedMessage}")
            _isLoading.value = false
            Result.failure(e)
        }
    }
    
    private suspend fun handleSignInResult(result: GetCredentialResponse): Result<Unit> {
        return try {
            when (val credential = result.credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        
                        val signInResult = firebaseRepository.signInWithGoogle(idToken)
                        signInResult.onSuccess {
                            _authState.value = AuthState.Authenticated(it)
                        }.onFailure {
                            _error.postValue(it.message)
                        }
                        
                        _isLoading.value = false
                        if (signInResult.isSuccess) Result.success(Unit) 
                        else Result.failure(signInResult.exceptionOrNull() ?: Exception("Unknown error"))
                    } else {
                        _isLoading.value = false
                        Result.failure(Exception("Unexpected credential type"))
                    }
                }
                else -> {
                    _isLoading.value = false
                    Result.failure(Exception("Unexpected credential type"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Handle sign in result failed", e)
            _isLoading.value = false
            Result.failure(e)
        }
    }
    
    // Email/Password Sign In
    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.postValue("Заповніть всі поля")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            val result = firebaseRepository.signInWithEmail(email, password)
            result.onSuccess {
                _authState.value = AuthState.Authenticated(it)
            }.onFailure {
                _error.postValue(getErrorMessage(it))
            }
            _isLoading.value = false
        }
    }
    
    // Email/Password Sign Up
    fun signUpWithEmail(email: String, password: String, displayName: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _error.postValue("Заповніть всі поля")
            return
        }
        
        if (password.length < 6) {
            _error.postValue("Пароль повинен містити мінімум 6 символів")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            val result = firebaseRepository.signUpWithEmail(email, password, displayName)
            result.onSuccess {
                _authState.value = AuthState.Authenticated(it)
            }.onFailure {
                _error.postValue(getErrorMessage(it))
            }
            _isLoading.value = false
        }
    }
    
    fun signOut() {
        firebaseRepository.signOut()
        _authState.value = AuthState.Unauthenticated
    }
    
    private fun getErrorMessage(exception: Throwable): String {
        return when {
            exception.message?.contains("email") == true -> "Невірний email"
            exception.message?.contains("password") == true -> "Невірний пароль"
            exception.message?.contains("network") == true -> "Перевірте інтернет-з'єднання"
            exception.message?.contains("already") == true -> "Цей email вже зареєстровано"
            else -> exception.localizedMessage ?: "Невідома помилка"
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
}
