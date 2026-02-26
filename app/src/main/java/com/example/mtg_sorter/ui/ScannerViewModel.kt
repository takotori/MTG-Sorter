package com.example.mtg_sorter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mtg_sorter.data.api.MoxfieldRetrofit
import com.example.mtg_sorter.data.api.RetrofitClient
import com.example.mtg_sorter.data.local.CardDatabaseHelper
import com.example.mtg_sorter.data.model.DeckMatch
import com.example.mtg_sorter.data.model.LoadedDeck
import com.example.mtg_sorter.data.model.MoxfieldDeckResponse
import com.example.mtg_sorter.data.model.ScryfallCard
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = CardDatabaseHelper(application)

    private val _detectedText = MutableStateFlow("")
    val detectedText = _detectedText.asStateFlow()

    private val _scannedCard = MutableStateFlow<ScryfallCard?>(null)
    val scannedCard = _scannedCard.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isScanLocked = MutableStateFlow(false)
    val isScanLocked = _isScanLocked.asStateFlow()

    // Loaded Moxfield decks
    private val _decks = MutableStateFlow<List<LoadedDeck>>(emptyList())
    val decks = _decks.asStateFlow()

    init {
        viewModelScope.launch {
            detectedText
                .debounce(150)
                .distinctUntilChanged()
                .collect { text ->
                    if (_isScanLocked.value) return@collect
                    if (text.length >= 3) {
                        searchCard(text)
                    }
                }
        }
    }

    private val detectionsBuffer = mutableListOf<String>()
    private val BUFFER_SIZE = 3

    fun onTextDetected(text: String) {
        if (_isScanLocked.value) return

        // Temporal filtering to stabilize detection
        detectionsBuffer.add(text)
        if (detectionsBuffer.size > BUFFER_SIZE) {
            detectionsBuffer.removeAt(0)
        }

        // Only update if we have a consensus
        val consensus = detectionsBuffer.groupBy { it }
            .maxByOrNull { it.value.size }
            ?.takeIf { it.value.size >= 2 }
            ?.key

        if (consensus != null && consensus != _detectedText.value) {
            _detectedText.value = consensus
        }
    }

    private fun normalizeName(s: String): String = s.trim().lowercase()

    // Build a map of required quantities aggregated over all boards
    private fun aggregateQuantities(resp: MoxfieldDeckResponse): Map<String, Int> {
        val total = mutableMapOf<String, Int>()
        fun addBoard(board: Map<String, com.example.mtg_sorter.data.model.MoxfieldCardEntry>?) {
            board?.forEach { (key, entry) ->
                val nm = normalizeName(entry.card?.name ?: key)
                val qty = entry.quantity
                if (qty > 0) total[nm] = (total[nm] ?: 0) + qty
            }
        }
        addBoard(resp.mainboard)
        addBoard(resp.sideboard)
        addBoard(resp.commanders)
        addBoard(resp.companions)
        return total
    }

    fun addDeck(input: String) {
        val id = extractDeckId(input) ?: return
        if (_decks.value.any { it.id == id }) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = MoxfieldRetrofit.instance.getDeck(id)
                if (resp.isSuccessful) {
                    val body = resp.body() ?: return@launch
                    val totals = aggregateQuantities(body)
                    val loaded = LoadedDeck(
                        id = body.id ?: id,
                        name = body.name ?: id,
                        cardTotals = totals,
                        collected = emptyMap()
                    )
                    _decks.value = _decks.value + loaded
                } else {
                    // Log or handle error (e.g., 404, 403)
                }
            } catch (e: Exception) {
                // Log or handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun extractDeckId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        // If looks like a URL, pull the last path segment that matches the ID pattern (contains dashes)
        return if (trimmed.startsWith("http")) {
            val parts = trimmed.trimEnd('/').split('/')
            parts.lastOrNull { it.count { ch -> ch == '-' } >= 3 } ?: parts.lastOrNull()
        } else trimmed
    }

    fun sortCardIntoDeck(deckId: String, cardName: String) {
        val nm = normalizeName(cardName)
        _decks.update { currentDecks ->
            currentDecks.map { deck ->
                if (deck.id == deckId) {
                    val total = deck.cardTotals[nm] ?: return@map deck
                    val currentCollected = deck.collected[nm] ?: 0
                    if (currentCollected >= total) return@map deck
                    
                    val newCollected = deck.collected.toMutableMap()
                    newCollected[nm] = currentCollected + 1
                    deck.copy(collected = newCollected)
                } else {
                    deck
                }
            }
        }
    }

    fun computeMatchesFor(name: String?): List<DeckMatch> {
        val nm = name?.let { normalizeName(it) } ?: return emptyList()
        return decks.value.mapNotNull { deck ->
            val total = deck.cardTotals[nm] ?: return@mapNotNull null
            val collectedValue = deck.collected[nm] ?: 0
            DeckMatch(deckId = deck.id, deckName = deck.name, total = total, collected = collectedValue)
        }
    }

    private fun searchCard(name: String) {
        viewModelScope.launch {
            _scannedCard.value = null
            _isLoading.value = true
            try {
                // Try local database first
                val localCard = dbHelper.getCardByName(name)
                val card = localCard ?: run {
                    println("search card: $name")
                    val response = RetrofitClient.instance.getCardNamedFuzzy(name)
                    if (response.isSuccessful) response.body() else null
                }

                _scannedCard.value = card

                if (card != null) {
                    val matches = computeMatchesFor(card.name)
                    if (matches.isNotEmpty()) {
                        _isScanLocked.value = true
                        delay(2000)
                        detectionsBuffer.clear()
                        _detectedText.value = ""
                        _isScanLocked.value = false
                    }
                }
            } catch (e: Exception) {
                _scannedCard.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}
