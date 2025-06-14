package com.example.fmapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fmapp.data.remote.AppSupabaseClient // Ensure this import is correct
import io.supabase.gotrue.GoTrue
import io.supabase.gotrue.Session
// import io.supabase.gotrue.user.User // Supabase User - This specific import might not be needed if session.user is directly usable or if AppUser is preferred.
// Using io.supabase.gotrue.user.User from the Supabase library for clarity if needed, or just rely on session.user properties.
// For this code, session.user.id and session.user.email are directly accessed.
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Data class to represent app-specific user details, can be expanded
data class AppUser(val id: String, val email: String?)

class AuthViewModel : ViewModel() {

    private val supabaseAuth: GoTrue = AppSupabaseClient.auth

    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // Check initial session state
        viewModelScope.launch {
            try {
                // Supabase client automatically attempts to load session from storage.
                // The .session() call might refresh it if needed or just return current.
                // For fetching initial state, direct access or specific function might vary by library version.
                // Let's assume supabaseAuth.retrieveSession() or similar exists and works as intended by the prompt.
                // The SupabaseKT library handles this with `supabaseAuth.sessionManager.currentSession()` or `supabaseAuth.sessionStatus.value` for flow.
                // Given the prompt uses `retrieveSession()`, we'll stick to that for now.
                val session = supabaseAuth.retrieveSession()
                session?.user?.let {
                    _currentUser.value = AppUser(it.id, it.email)
                }
            } catch (e: Exception) {
                // Ignore error if no session found or other minor issues on init
                // A more robust app might log this or handle specific cases
                println("Error retrieving session on init: ${e.message}")
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // The signUp function in Supabase returns the user object upon successful registration (before email confirmation if enabled)
                val user = supabaseAuth.signUp(email = email, password = password)
                user?.let {
                    // Check if there's an active session immediately. If email confirmation is required,
                    // session might be null, or user.confirmedAt might be null.
                    val currentSession = supabaseAuth.retrieveSession() // Attempt to get session
                    if (currentSession?.user != null && currentSession.user?.emailConfirmedAt != null) { // Check if user is confirmed and session active
                         _currentUser.value = AppUser(currentSession.user!!.id, currentSession.user!!.email)
                    } else if (currentSession?.user != null && currentSession.user?.emailConfirmedAt == null && supabaseAuth.config.enableEmailConfirmation) {
                        // User exists but email not confirmed
                        _error.value = "Registration successful, please check your email to confirm."
                    }
                     else if (currentSession?.user == null && supabaseAuth.config.enableEmailConfirmation) {
                        _error.value = "Registration successful, please check your email to confirm."
                    }
                    else if (!supabaseAuth.config.enableEmailConfirmation && currentSession?.user != null) {
                        // Auto-login if email confirmation is disabled
                         _currentUser.value = AppUser(currentSession.user!!.id, currentSession.user!!.email)
                    }
                    else {
                        // Fallback, ideally covered by specific checks above
                         _error.value = "Registration state unclear. Please check your email or try logging in."
                    }
                } ?: run {
                     _error.value = "Registration failed: No user data returned."
                }
            } catch (e: Exception) {
                _error.value = "Registration failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val session: Session = supabaseAuth.signIn(email = email, password = password)
                session.user?.let {
                    _currentUser.value = AppUser(it.id, it.email)
                } ?: run {
                    _error.value = "Login failed: User data missing in session."
                }
            } catch (e: Exception) {
                _error.value = "Login failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                supabaseAuth.signOut()
                _currentUser.value = null
            } catch (e: Exception) {
                _error.value = "Sign out failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
