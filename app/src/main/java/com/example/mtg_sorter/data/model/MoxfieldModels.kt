package com.example.mtg_sorter.data.model

import com.google.gson.annotations.SerializedName

// Network models based on Moxfield v2 decks/all/{id} response.
// Only the fields we use are modeled; extra fields are ignored by Gson.
data class MoxfieldDeckResponse(
    val id: String?,
    val name: String?,
    val mainboard: Map<String, MoxfieldCardEntry>?,
    val sideboard: Map<String, MoxfieldCardEntry>?,
    val commanders: Map<String, MoxfieldCardEntry>?,
    val companions: Map<String, MoxfieldCardEntry>?
)

data class MoxfieldCardEntry(
    val quantity: Int = 0,
    val card: MoxfieldCard? = null
)

data class MoxfieldCard(
    val name: String? = null
)

// App-level models for deck handling

data class LoadedDeck(
    val id: String,
    val name: String,
    val cardTotals: Map<String, Int>, // normalized card name -> required qty (aggregated across boards)
    val collected: Map<String, Int> = emptyMap() // normalized card name -> collected qty
) {
    val totalCards: Int get() = cardTotals.values.sum()
    val totalCollected: Int get() = collected.values.sum()
}

data class DeckMatch(
    val deckId: String,
    val deckName: String,
    val total: Int,
    val collected: Int
)