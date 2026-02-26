package com.example.mtg_sorter.ui

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class CardAnalyzer(
    private val onTextDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotation)
            
            // Calculate upright dimensions
            val isRotated = rotation == 90 || rotation == 270
            val uprightWidth = if (isRotated) imageProxy.height else imageProxy.width
            val uprightHeight = if (isRotated) imageProxy.width else imageProxy.height

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val textBlocks = visionText.textBlocks
                    if (textBlocks.isNotEmpty()) {
                        // MTG card names have a few distinct properties:
                        // 1. They are usually the largest font size (tallest line height).
                        // 2. They are usually a single line (or occasionally two).
                        // 3. They are not as dense as the description text.
                        // 4. Position: User is told to align the card name in a specific box.

                        val scoredBlocks = textBlocks.mapNotNull { block ->
                            val box = block.boundingBox ?: return@mapNotNull null
                            val text = block.text.trim().replace("\n", " ")
                            
                            if (text.length < 3 || text.length > 60 || block.lines.size > 2) return@mapNotNull null
                            
                            // Normalize coordinates (0.0 to 1.0)
                            val centerX = box.centerX().toFloat() / uprightWidth
                            val centerY = box.centerY().toFloat() / uprightHeight
                            val blockHeight = box.height().toFloat() / uprightHeight
                            
                            // Spatial filtering: The guidance box is around Y=0.15 to 0.25
                            // We weight blocks higher if they are in this vertical zone and horizontally centered.
                            
                            // Penalty for being too far from the horizontal center
                            // MTG names are left-aligned but the block might span more.
                            // We still prefer it not being stuck to one edge entirely if possible,
                            // but let's be more lenient.
                            val horizontalFactor = 1.0f - Math.abs(centerX - 0.5f) 
                            
                            // Penalty for being outside the vertical focus zone (around 0.2)
                            val targetY = 0.2f
                            val verticalFactor = 1.0f - Math.min(Math.abs(centerY - targetY) * 2.0f, 1.0f)
                            
                            // Font size factor (block height relative to image)
                            // A typical card name takes up about 3-6% of the upright height
                            val sizeFactor = Math.min(blockHeight * 20.0f, 2.0f) 
                            
                            // Combined score
                            val score = horizontalFactor * verticalFactor * sizeFactor
                            
                            if (score > 0.001f) {
                                Pair(block, score)
                            } else {
                                null
                            }
                        }

                        val bestBlock = if (scoredBlocks.isNotEmpty()) {
                            scoredBlocks.maxByOrNull { it.second }?.first
                        } else {
                            // Fallback to previous heuristic if no spatial matches (maybe box is offset)
                            textBlocks
                                .filter { it.text.length in 3..60 && it.lines.size <= 2 }
                                .maxByOrNull { block ->
                                    block.lines.mapNotNull { it.boundingBox?.height() }.average()
                                } ?: textBlocks.maxByOrNull { it.boundingBox?.height() ?: 0 }
                        }

                        val detectedName = bestBlock?.text?.trim()?.replace("\n", " ")
                        if (detectedName != null && detectedName.isNotBlank() && detectedName.length >= 3) {
                            onTextDetected(detectedName)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
