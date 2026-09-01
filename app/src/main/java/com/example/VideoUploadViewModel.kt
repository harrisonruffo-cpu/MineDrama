package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.AppwriteStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VideoUploadViewModel : ViewModel() {
    sealed class UploadState {
        object Idle : UploadState()
        object Loading : UploadState()
        data class Success(val downloadUrl: String) : UploadState()
        data class Error(val msg: String) : UploadState()
    }

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state

    fun uploadVideo(
        context: Context,
        uri: Uri,
        bucketId: String = AppwriteStorageManager.BUCKET_VIDEOS,
        titulo: String = "",
        descricao: String = "",
        userId: String = ""
    ) {
        _state.value = UploadState.Loading
        viewModelScope.launch {
            val result = AppwriteStorageManager.uploadFile(
                context = context,
                uri = uri,
                bucketId = bucketId,
                mimeType = "video/mp4"
            )
            result.onSuccess { url ->
                _state.value = UploadState.Success(url)
            }.onFailure { error ->
                _state.value = UploadState.Error(error.localizedMessage ?: "Erro desconhecido ao enviar vídeo para o Appwrite")
            }
        }
    }

    fun reset() { _state.value = UploadState.Idle }
}

