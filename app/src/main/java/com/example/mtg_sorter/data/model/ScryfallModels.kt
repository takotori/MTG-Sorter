package com.example.mtg_sorter.data.model

import com.google.gson.annotations.SerializedName

data class ScryfallCard(
    val id: String,
    val name: String,
    @SerializedName("image_uris") val imageUris: ImageUris?,
    @SerializedName("mana_cost") val manaCost: String?,
    @SerializedName("type_line") val typeLine: String?,
    @SerializedName("oracle_text") val oracleText: String?,
    val set: String,
    @SerializedName("set_name") val setName: String,
    val rarity: String
)

data class ImageUris(
    val small: String,
    val normal: String,
    val large: String,
    val png: String,
    @SerializedName("art_crop") val artCrop: String,
    @SerializedName("border_crop") val borderCrop: String
)

data class ScryfallSearchResponse(
    @SerializedName("total_cards") val totalCards: Int,
    @SerializedName("has_more") val hasMore: Boolean,
    val data: List<ScryfallCard>
)
