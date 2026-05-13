package com.demo.creditlimit.ui.kyc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.creditlimit.network.model.request2.BindBankCardReq
import com.demo.creditlimit.network.model.request2.IfscInfoResp
import com.demo.creditlimit.network.repository.ConfigRepository
import com.demo.creditlimit.network.repository.UserRepository
import com.demo.creditlimit.network.state.NetworkResult
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Form state ─────────────────────────────────────────────────────────────

data class BindCardState(
    val holderName: String = "",
    val bankCard: String = "",          // raw digits only
    val reenterBankCard: String = "",   // raw digits only
    val ifscCode: String = "",
    val ifscInfo: IfscInfoResp? = null,
    val ifscError: String? = null,
    val isLoadingIfsc: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMsg: String? = null
) {
    val bankCardFormatted: String get() = bankCard.chunked(4).joinToString(" ")
    val reenterFormatted: String get() = reenterBankCard.chunked(4).joinToString(" ")

    val isIfscValid: Boolean
        get() = ifscCode.length == 11 && ifscCode.matches(Regex("[A-Z]{4}0[A-Z0-9]{6}"))

    val isComplete: Boolean
        get() = holderName.isNotBlank() &&
            bankCard.length in 8..18 &&
            bankCard == reenterBankCard &&
            isIfscValid &&
            ifscInfo != null &&
            ifscError == null
}

// ── Serializable snapshot for DataStore ────────────────────────────────────

private data class BindCardSnapshot(
    val bankCard: String = "",
    val reenterBankCard: String = "",
    val ifscCode: String = ""
)

// ── UI state ───────────────────────────────────────────────────────────────

sealed class BindCardUiState {
    object Loading : BindCardUiState()
    data class Ready(val cardState: BindCardState = BindCardState()) : BindCardUiState()
    object SubmitSuccess : BindCardUiState()
}

// ── ViewModel ──────────────────────────────────────────────────────────────

class BindCardViewModel(
    private val configRepository: ConfigRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BindCardUiState>(BindCardUiState.Loading)
    val uiState: StateFlow<BindCardUiState> = _uiState.asStateFlow()

    private val gson = Gson()
    private var ifscJob: Job? = null

    init {
        viewModelScope.launch { loadInitialData() }
    }

    private suspend fun loadInitialData() {
        val holderName = userRepository.getBasicInfo()?.aadName?.takeIf { it.isNotBlank() } ?: ""
        val savedJson = configRepository.loadBindCardForm()
        val snapshot = savedJson?.let {
            runCatching { gson.fromJson(it, BindCardSnapshot::class.java) }.getOrNull()
        }
        var state = BindCardState(
            holderName = holderName,
            bankCard = snapshot?.bankCard ?: "",
            reenterBankCard = snapshot?.reenterBankCard ?: "",
            ifscCode = snapshot?.ifscCode ?: ""
        )
        // Trigger IFSC lookup if there's a saved valid code
        if (state.isIfscValid) {
            state = state.copy(isLoadingIfsc = true)
            _uiState.value = BindCardUiState.Ready(state)
            val info = userRepository.getIfscInfo(state.ifscCode)
            state = if (info != null) state.copy(ifscInfo = info, isLoadingIfsc = false)
                    else state.copy(ifscError = "IFSC code not found", isLoadingIfsc = false)
        }
        _uiState.value = BindCardUiState.Ready(state)
    }

    fun updateBankCard(digits: String) {
        val raw = digits.filter { it.isDigit() }.take(18)
        updateState { it.copy(bankCard = raw) }
        saveSnapshot()
    }

    fun updateReenterBankCard(digits: String) {
        val raw = digits.filter { it.isDigit() }.take(18)
        updateState { it.copy(reenterBankCard = raw) }
        saveSnapshot()
    }

    fun updateIfscCode(input: String) {
        val code = input.uppercase().filter { it.isLetterOrDigit() }.take(11)
        updateState { it.copy(ifscCode = code, ifscInfo = null, ifscError = null) }
        saveSnapshot()
        if (code.length == 11 && code.matches(Regex("[A-Z]{4}0[A-Z0-9]{6}"))) {
            loadIfscInfo(code)
        }
    }

    private fun loadIfscInfo(code: String) {
        ifscJob?.cancel()
        updateState { it.copy(isLoadingIfsc = true, ifscError = null, ifscInfo = null) }
        ifscJob = viewModelScope.launch {
            val info = userRepository.getIfscInfo(code)
            if (info != null) {
                updateState { it.copy(ifscInfo = info, isLoadingIfsc = false) }
            } else {
                updateState { it.copy(ifscError = "IFSC code not found", isLoadingIfsc = false) }
            }
        }
    }

    fun submit() {
        val state = ready()?.cardState ?: return
        if (!state.isComplete) return
        updateState { it.copy(isSubmitting = true, errorMsg = null) }

        viewModelScope.launch {
            val req = BindBankCardReq().apply {
                name = state.holderName
                bankCard = state.bankCard
                ifscCode = state.ifscCode
            }
            userRepository.bindBankCard(req).collect { result ->
                when (result) {
                    is NetworkResult.Success, is NetworkResult.Empty -> {
                        configRepository.clearBindCardForm()
                        _uiState.value = BindCardUiState.SubmitSuccess
                    }
                    is NetworkResult.Error -> {
                        updateState {
                            it.copy(isSubmitting = false, errorMsg = result.exception.message ?: "Submit failed")
                        }
                    }
                    is NetworkResult.Loading -> Unit
                }
            }
        }
    }

    fun clearError() {
        updateState { it.copy(errorMsg = null) }
    }

    private fun ready(): BindCardUiState.Ready? = _uiState.value as? BindCardUiState.Ready

    private fun updateState(transform: (BindCardState) -> BindCardState) {
        val r = ready() ?: return
        _uiState.value = r.copy(cardState = transform(r.cardState))
    }

    private fun saveSnapshot() {
        val state = ready()?.cardState ?: return
        viewModelScope.launch {
            configRepository.saveBindCardForm(gson.toJson(BindCardSnapshot(state.bankCard, state.reenterBankCard, state.ifscCode)))
        }
    }
}
