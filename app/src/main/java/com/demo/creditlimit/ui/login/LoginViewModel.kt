package com.demo.creditlimit.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.creditlimit.network.exception.ErrorMessageMapper
import com.demo.creditlimit.network.model.request2.LoginResp
import com.demo.creditlimit.network.repository.AuthRepository
import com.demo.creditlimit.network.state.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
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
                _uiState.value = when (result) {
                    is NetworkResult.Loading -> LoginUiState.Loading
                    is NetworkResult.Success -> LoginUiState.Success(result.data)
                    is NetworkResult.Error -> LoginUiState.Error(
                        ErrorMessageMapper.toUserMessage(result.exception)
                    )
                    is NetworkResult.Empty -> LoginUiState.Error("Unexpected empty response.")
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
    data class Success(val data: LoginResp) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
