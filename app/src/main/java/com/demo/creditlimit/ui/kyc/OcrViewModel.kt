package com.demo.creditlimit.ui.kyc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.creditlimit.network.model.request2.IdentityInfoReq
import com.demo.creditlimit.network.repository.UserRepository
import com.demo.creditlimit.network.state.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

// ── Confirm form state ─────────────────────────────────────────────────────

data class OcrConfirmState(
    val imageUrl: String = "",
    val aadNumber: String = "",
    val aadName: String = "",
    val birthday: String = "",
    val gender: Long = 1L
) {
    val isAadNumberValid get() = aadNumber.length == 12 && aadNumber.all { it.isDigit() }
    val isNameValid get() = aadName.length in 3..30
    val isBirthdayValid get() = birthday.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))

    val isAgeOutOfRange: Boolean
        get() {
            if (!isBirthdayValid) return false
            val year = birthday.take(4).toIntOrNull() ?: return false
            val age = Calendar.getInstance().get(Calendar.YEAR) - year
            return age !in 20..60
        }

    val isComplete get() = isAadNumberValid && isNameValid && isBirthdayValid
    val genderLabel get() = if (gender == 1L) "Male" else "Female"
}

// ── UI state ───────────────────────────────────────────────────────────────

sealed class OcrUiState {
    object Loading : OcrUiState()
    data class Ready(
        val confirmState: OcrConfirmState? = null,
        val isProcessing: Boolean = false,
        val showConfirmDialog: Boolean = false,
        val showSourceSheet: Boolean = false,
        val showDatePicker: Boolean = false,
        val showGenderSheet: Boolean = false,
        val isSubmitting: Boolean = false,
        val ageWarningVisible: Boolean = false,
        val errorMsg: String? = null
    ) : OcrUiState() {
        val canProceed get() = confirmState != null && !isProcessing
    }
    object SubmitSuccess : OcrUiState()
}

// ── ViewModel ──────────────────────────────────────────────────────────────

class OcrViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OcrUiState>(OcrUiState.Loading)
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadHistory() }
    }

    private suspend fun loadHistory() {
        val history = userRepository.getAadhaarFrontHistory()
        val confirmState = history?.takeIf { !it.cid.isNullOrBlank() }?.let { r ->
            OcrConfirmState(
                imageUrl = r.url ?: "",
                aadNumber = (r.cid ?: "").filter { it.isDigit() }.take(12),
                aadName = (r.name ?: "").replace("\n", "").trim().take(30),
                birthday = r.birthday ?: "",
                gender = r.gender.toLong().coerceIn(1L, 2L)
            )
        }
        _uiState.value = OcrUiState.Ready(confirmState = confirmState)
    }

    fun showSourceSheet() = updateReady { it.copy(showSourceSheet = true) }
    fun hideSourceSheet() = updateReady { it.copy(showSourceSheet = false) }

    fun uploadAndOcr(imageBytes: ByteArray) {
        updateReady { it.copy(isProcessing = true, showSourceSheet = false, errorMsg = null) }
        viewModelScope.launch {
            val url = userRepository.uploadImage(imageBytes)
            if (url == null) {
                updateReady { it.copy(isProcessing = false, errorMsg = "Upload failed, please retry") }
                return@launch
            }
            val ocr = userRepository.submitAadhaarFrontOcr(url)
            if (ocr == null) {
                updateReady { it.copy(isProcessing = false, errorMsg = "Recognition failed, please retry") }
                return@launch
            }
            val confirm = OcrConfirmState(
                imageUrl = url,
                aadNumber = (ocr.cid ?: "").filter { it.isDigit() }.take(12),
                aadName = (ocr.name ?: "").replace("\n", "").trim().take(30),
                birthday = ocr.birthday ?: "",
                gender = ocr.gender.toLong().coerceIn(1L, 2L)
            )
            updateReady { it.copy(isProcessing = false, confirmState = confirm, showConfirmDialog = true) }
        }
    }

    fun openConfirmDialog() = updateReady { it.copy(showConfirmDialog = true) }
    fun closeConfirmDialog() = updateReady { it.copy(showConfirmDialog = false) }

    fun updateAadNumber(v: String) = updateConfirm { it.copy(aadNumber = v.filter(Char::isDigit).take(12)) }
    fun updateAadName(v: String) = updateConfirm { it.copy(aadName = v.replace("\n", "").take(30)) }
    fun updateBirthday(v: String) = updateConfirm { it.copy(birthday = v) }
    fun updateGender(v: Long) {
        updateConfirm { it.copy(gender = v) }
        updateReady { it.copy(showGenderSheet = false) }
    }

    fun openDatePicker() = updateReady { it.copy(showDatePicker = true) }
    fun closeDatePicker() = updateReady { it.copy(showDatePicker = false) }
    fun openGenderSheet() = updateReady { it.copy(showGenderSheet = true) }
    fun closeGenderSheet() = updateReady { it.copy(showGenderSheet = false) }

    fun agreeAndSubmit() {
        val ready = ready() ?: return
        val confirm = ready.confirmState ?: return
        if (!confirm.isComplete) return
        if (confirm.isAgeOutOfRange) {
            updateReady { it.copy(ageWarningVisible = true) }
            return
        }
        updateReady { it.copy(isSubmitting = true, errorMsg = null) }
        viewModelScope.launch {
            val req = IdentityInfoReq().apply {
                aadNumber = confirm.aadNumber
                aadName = confirm.aadName
                name = confirm.aadName
                aadBirthday = confirm.birthday
                birthday = confirm.birthday
                gender = confirm.gender
            }
            userRepository.submitIdentity(req).collect { result ->
                when (result) {
                    is NetworkResult.Success, is NetworkResult.Empty ->
                        _uiState.value = OcrUiState.SubmitSuccess
                    is NetworkResult.Error ->
                        updateReady { it.copy(isSubmitting = false, errorMsg = result.exception.message ?: "Submit failed") }
                    is NetworkResult.Loading -> Unit
                }
            }
        }
    }

    fun clearError() = updateReady { it.copy(errorMsg = null) }
    fun clearAgeWarning() = updateReady { it.copy(ageWarningVisible = false) }

    private fun ready() = _uiState.value as? OcrUiState.Ready

    private fun updateReady(f: (OcrUiState.Ready) -> OcrUiState.Ready) {
        val r = ready() ?: return
        _uiState.value = f(r)
    }

    private fun updateConfirm(f: (OcrConfirmState) -> OcrConfirmState) {
        val r = ready() ?: return
        val c = r.confirmState ?: return
        _uiState.value = r.copy(confirmState = f(c))
    }
}
