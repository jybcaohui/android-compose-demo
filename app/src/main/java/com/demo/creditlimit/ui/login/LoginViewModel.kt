package com.demo.creditlimit.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.creditlimit.navigation.KycRouter
import com.demo.creditlimit.navigation.Screen
import com.demo.creditlimit.network.exception.ErrorMessageMapper
import com.demo.creditlimit.network.repository.AuthRepository
import com.demo.creditlimit.network.repository.UserRepository
import com.demo.creditlimit.network.state.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val appName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.PhoneInput)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun sendVcode(phone: String) {
        if (phone.isBlank()) {
            _uiState.value = LoginUiState.Error("Phone number is required.")
            return
        }
        viewModelScope.launch {
            authRepository.sendVcode(phone, appName).collect { result ->
                _uiState.value = when (result) {
                    is NetworkResult.Loading -> LoginUiState.Loading
                    is NetworkResult.Success, is NetworkResult.Empty ->
                        LoginUiState.OtpSent(phone)
                    is NetworkResult.Error -> LoginUiState.Error(
                        ErrorMessageMapper.toUserMessage(result.exception)
                    )
                }
            }
        }
    }

    fun login(phone: String, otp: String, deviceId: String) {
        if (otp.isBlank()) {
            _uiState.value = LoginUiState.Error("Verification code is required.")
            return
        }
        viewModelScope.launch {
            authRepository.login(phone, otp, deviceId, appName).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.value = LoginUiState.Loading
                    is NetworkResult.Success -> fetchProfileAndNavigate()
                    is NetworkResult.Error -> _uiState.value = LoginUiState.Error(
                        ErrorMessageMapper.toUserMessage(result.exception)
                    )
                    is NetworkResult.Empty -> _uiState.value =
                        LoginUiState.Error("Unexpected empty response.")
                }
            }
        }
    }

    private fun fetchProfileAndNavigate() {
        viewModelScope.launch {
            userRepository.getUserProfile().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.value = LoginUiState.Loading
                    is NetworkResult.Success -> {
                        val nextScreen = KycRouter.resolveNextScreen(result.data.profile)
                            ?: Screen.CreditResult
                        _uiState.value = LoginUiState.NavigateTo(nextScreen)
                    }
                    is NetworkResult.Error -> {
                        // Profile fetch failed — fall back to supplementary info (first KYC step)
                        _uiState.value = LoginUiState.NavigateTo(Screen.KycSupplementaryInfo)
                    }
                    is NetworkResult.Empty -> {
                        _uiState.value = LoginUiState.NavigateTo(Screen.KycSupplementaryInfo)
                    }
                }
            }
        }
    }

    fun backToPhoneInput() {
        _uiState.value = LoginUiState.PhoneInput
    }

    fun resetError() {
        val current = _uiState.value
        _uiState.value = if (current is LoginUiState.OtpSent) current else LoginUiState.PhoneInput
    }
}

sealed class LoginUiState {
    data object PhoneInput : LoginUiState()
    data class OtpSent(val phone: String) : LoginUiState()
    data object Loading : LoginUiState()
    data class NavigateTo(val screen: Screen) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
