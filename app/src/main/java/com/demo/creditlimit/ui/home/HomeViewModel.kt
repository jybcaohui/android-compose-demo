package com.demo.creditlimit.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.creditlimit.navigation.KycRouter
import com.demo.creditlimit.navigation.Screen
import com.demo.creditlimit.network.repository.UserRepository
import com.demo.creditlimit.network.state.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val userRepository: UserRepository,
    private val isLoggedIn: StateFlow<Boolean?>
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun handleGetStarted() {
        if (isLoggedIn.value != true) {
            _uiState.value = HomeUiState.NavigateTo(Screen.Login)
            return
        }
        viewModelScope.launch {
            userRepository.getUserProfile().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.value = HomeUiState.Loading
                    is NetworkResult.Success -> {
                        val next = KycRouter.resolveNextScreen(result.data.profile)
                            ?: Screen.CreditResult
                        _uiState.value = HomeUiState.NavigateTo(next)
                    }
                    is NetworkResult.Error -> _uiState.value = HomeUiState.Error
                    is NetworkResult.Empty -> _uiState.value =
                        HomeUiState.NavigateTo(Screen.KycSupplementaryInfo)
                }
            }
        }
    }

    fun resetNavigation() {
        _uiState.value = HomeUiState.Idle
    }
}

sealed class HomeUiState {
    data object Idle : HomeUiState()
    data object Loading : HomeUiState()
    data class NavigateTo(val screen: Screen) : HomeUiState()
    data object Error : HomeUiState()
}
