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

    fun onTextDetected(text: String) {
        _detectedText.value = text
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
