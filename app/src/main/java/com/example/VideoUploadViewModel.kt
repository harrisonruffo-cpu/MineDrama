package com.example.viewmodels

import android.net.Uri
import android.content.ContentResolver
import android.lifecycle.ViewModel
import android.lifecycle.viewModelScope
import com.example.Appwrite
import io.appwrite.ID
import io.appwrite.models.InputFile
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
                val inputFile = InputFile.fromUri(contentResolver, uri)
                val response = Appwrite.storage.createFile(
                    bucketId = "videos",
                    fileId = ID.unique(),
                    file = inputFile
                )
                _state.value = UploadState.Success
            } catch (e: Exception) {
                _state.value = UploadState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    fun reset() { _state.value = UploadState.Idle }
}
