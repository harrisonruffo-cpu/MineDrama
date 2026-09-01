package com.example.viewmodels

import android.net.Uri
import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VideoUploadViewModel : ViewModel() {
    sealed class UploadState {
        object Idle : UploadState()
        object Loading : UploadState()
        object Success : UploadState()
        data class Error(val msg: String) : UploadState()
    }

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state

    fun uploadVideo(
        uri: Uri,
        contentResolver: ContentResolver,
        titulo: String,
        descricao: String,
        userId: String
    ) {
        _state.value = UploadState.Loading
        viewModelScope.launch {
            try {
                // Simulação de upload: apenas aguarda 2 segundos e retorna sucesso
                // Substitua isso pelo código real do Appwrite quando estiver funcionando
                kotlinx.coroutines.delay(2000)
                _state.value = UploadState.Success
            } catch (e: Exception) {
                _state.value = UploadState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    fun reset() { _state.value = UploadState.Idle }
}
