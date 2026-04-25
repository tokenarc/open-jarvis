package com.openjarvis.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VisionModule(private val context: Context) {

    private val recognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    suspend fun extractText(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                if (extractedText.isNotBlank()) {
                    continuation.resume(extractedText)
                } else {
                    continuation.resume("")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR failed", e)
                continuation.resume("")
            }
    }

    suspend fun extractStructured(bitmap: Bitmap): ScreenOCR = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks.map { block ->
                    OCRBlock(
                        text = block.text,
                        confidence = block.confidence,
                        boundingBox = block.boundingBox?.let { rect ->
                            BoundingBox(rect.left, rect.top, rect.right, rect.bottom)
                        },
                        lines = block.lines.map { line ->
                            OCRLine(
                                text = line.text,
                                confidence = line.confidence,
                                boundingBox = line.boundingBox?.let { rect ->
                                    BoundingBox(rect.left, rect.top, rect.right, rect.bottom)
                                }
                            )
                        }
                    )
                }
                continuation.resume(ScreenOCR(
                    fullText = visionText.text,
                    blocks = blocks
                ))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR failed", e)
                continuation.resume(ScreenOCR("", emptyList()))
            }
    }

    fun release() {
        recognizer.close()
    }

    data class ScreenOCR(
        val fullText: String,
        val blocks: List<OCRBlock>
    )

    data class OCRBlock(
        val text: String,
        val confidence: Float,
        val boundingBox: BoundingBox?,
        val lines: List<OCRLine>
    )

    data class OCRLine(
        val text: String,
        val confidence: Float,
        val boundingBox: BoundingBox?
    )

    data class BoundingBox(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    companion object {
        private const val TAG = "VisionModule"

        @Volatile
        private var instance: VisionModule? = null

        fun getInstance(context: Context): VisionModule {
            return instance ?: synchronized(this) {
                instance ?: VisionModule(context.applicationContext).also { instance = it }
            }
        }
    }
}