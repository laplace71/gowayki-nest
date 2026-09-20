package com.gowayki.nesh.app.features.sign_in.presentation.StateFlow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowayki.nesh.app.common.modules.auth.AuthCopy
import com.gowayki.nesh.app.di.AppDi
import com.gowayki.nesh.app.di.AuthDi
import com.gowayki.nesh.app.features.sign_in.domain.models.CompleteProfileInput
import com.gowayki.nesh.app.features.sign_in.domain.models.ProfileGender
import com.gowayki.nesh.app.features.sign_in.domain.usecases.CompleteOwnProfileUseCase
import com.gowayki.nesh.app.features.sign_in.domain.usecases.GetAccountStatusUseCase
import com.gowayki.nesh.app.features.sign_in.domain.usecases.ObserveAuthSessionUseCase
import com.gowayki.nesh.app.features.sign_in.domain.usecases.SignInWithGoogleOAuthUseCase
import com.gowayki.nesh.app.features.sign_in.domain.usecases.SignOutUseCase
import com.gowayki.nesh.data.entities.auth.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(
    private val observeSession: ObserveAuthSessionUseCase = AuthDi.observeSession(),
    private val signInOAuth: SignInWithGoogleOAuthUseCase = AppDi.get(),
    private val signOut: SignOutUseCase = AuthDi.signOut(),
    private val getAccountStatus: GetAccountStatusUseCase = AuthDi.accountStatus(),
    private val completeProfile: CompleteOwnProfileUseCase = AuthDi.completeProfile(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { user ->
                if (user == null) {
                    _uiState.update {
                        SignInUiState(isLoading = false, isAccountLoading = false)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isAuthenticated = true,
                            userEmail = user.email,
                            displayName = user.displayName ?: it.displayName,
                            avatarUrl = user.photoUrl ?: it.avatarUrl,
                            registerName = it.registerName.ifBlank {
                                user.displayName.orEmpty()
                            },
                            isLoading = false,
                            error = null,
                        )
                    }
                    refreshAccountStatus(user)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onSignInStarted() {
        _uiState.update { it.copy(isLoading = true, error = null) }
    }

    fun onNativeSignInClosed() {
        _uiState.update {
            it.copy(isLoading = false, error = AuthCopy.closedByUser)
        }
    }

    fun onNativeSignInError(message: String?) {
        _uiState.update {
            it.copy(
                isLoading = false,
                error = message?.takeIf { msg -> msg.isNotBlank() } ?: AuthCopy.authFailed,
            )
        }
    }

    fun signInWithGoogleOAuth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { signInOAuth() }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: AuthCopy.authFailed,
                        )
                    }
                }
        }
    }

    fun onRegisterNameChange(value: String) {
        _uiState.update { it.copy(registerName = value) }
    }

    fun onRegisterGenderChange(value: ProfileGender) {
        _uiState.update { it.copy(registerGender = value) }
    }

    fun onAcceptTermsChange(value: Boolean) {
        _uiState.update { it.copy(acceptTerms = value) }
    }

    fun submitRegistration() {
        val state = _uiState.value
        val name = state.registerName.trim()
        if (name.length < 2) {
            _uiState.update { it.copy(error = AuthCopy.nameRequired) }
            return
        }
        if (!state.acceptTerms) {
            _uiState.update { it.copy(error = AuthCopy.termsRequired) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                completeProfile(
                    CompleteProfileInput(
                        fullName = name,
                        avatarUrl = state.avatarUrl,
                        gender = state.registerGender,
                        acceptTerms = true,
                    ),
                )
            }.onSuccess { result ->
                if (!result.success) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.error ?: AuthCopy.registrationFailed,
                        )
                    }
                    return@onSuccess
                }
                refreshAccountStatus()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: AuthCopy.registrationFailed,
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { signOut() }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: AuthCopy.authFailed)
                    }
                }
        }
    }

    private suspend fun refreshAccountStatus(user: AuthUser? = null) {
        _uiState.update { it.copy(isAccountLoading = true) }
        runCatching { getAccountStatus() }
            .onSuccess { status ->
                _uiState.update {
                    it.copy(
                        isAccountLoading = false,
                        isLoading = false,
                        isAuthenticated = status.authenticated,
                        needsRegistration = status.needsRegistration,
                        profileComplete = status.profileComplete,
                        userEmail = status.user?.email ?: user?.email ?: it.userEmail,
                        displayName = status.profile?.name
                            ?: user?.displayName
                            ?: it.displayName,
                        avatarUrl = status.profile?.avatarUrl
                            ?: user?.photoUrl
                            ?: it.avatarUrl,
                        registerName = it.registerName.ifBlank {
                            status.profile?.name
                                ?: user?.displayName
                                ?: ""
                        },
                        error = status.error ?: it.error,
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAccountLoading = false,
                        isLoading = false,
                        // Sin status, pedimos registro por seguridad si hay sesión.
                        needsRegistration = true,
                        error = error.message ?: AuthCopy.accountStatusFailed,
                    )
                }
            }
    }
}
