package com.example.mtg_sorter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mtg_sorter.data.api.RetrofitClient
import com.example.mtg_sorter.data.model.ScryfallCard
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ScannerViewModel : ViewModel() {

    private val _detectedText = MutableStateFlow("")
    val detectedText = _detectedText.asStateFlow()

    private val _scannedCard = MutableStateFlow<ScryfallCard?>(null)
    val scannedCard = _scannedCard.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            detectedText
                .debounce(500)
                .distinctUntilChanged()
                .collect { text ->
                    if (text.length > 3) {
                        searchCard(text)
                    }
                }
        }
    }

    private val detectionsBuffer = mutableListOf<String>()
    private val BUFFER_SIZE = 3

    fun onTextDetected(text: String) {
        // Temporal filtering to stabilize detection
        detectionsBuffer.add(text)
        if (detectionsBuffer.size > BUFFER_SIZE) {
            detectionsBuffer.removeAt(0)
        }

        // Only update if we have a consensus or at least multiple detections of the same string
        val consensus = detectionsBuffer.groupBy { it }
            .maxByOrNull { it.value.size }
            ?.takeIf { it.value.size >= 2 }
            ?.key

        if (consensus != null) {
            _detectedText.value = consensus
        }
    }

    private fun searchCard(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.instance.getCardNamedFuzzy(name)
                if (response.isSuccessful) {
                    _scannedCard.value = response.body()
                } else {
                    // If not found, we don't necessarily want to clear the previous card immediately
                    // but maybe we should if the text is very different.
                    // For now, let's just leave it.
                }
            } catch (e: Exception) {
                _scannedCard.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}
