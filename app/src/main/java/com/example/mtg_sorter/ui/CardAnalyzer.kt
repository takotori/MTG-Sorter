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
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val textBlocks = visionText.textBlocks
                    if (textBlocks.isNotEmpty()) {
                        // MTG card names have a few distinct properties:
                        // 1. They are usually the largest font size (tallest line height).
                        // 2. They are usually a single line (or occasionally two).
                        // 3. They are not as dense as the description text.

                        val bestBlock = textBlocks
                            .filter { it.text.length in 3..60 && it.lines.size <= 2 }
                            .maxByOrNull { block ->
                                // Calculate the average height of lines in this block
                                block.lines.mapNotNull { it.boundingBox?.height() }.average()
                            } ?: textBlocks.maxByOrNull { it.boundingBox?.height() ?: 0 }
                            ?: textBlocks.first()

                        val detectedName = bestBlock.text.trim().replace("\n", " ")
                        if (detectedName.isNotBlank() && detectedName.length > 3) {
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
