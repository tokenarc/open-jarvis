package com.openjarvis.vision

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.coroutines.resume

class ScreenshotCapture(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var isCapturing = false

    fun startCapture(): Boolean {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intents = mutableListOf<Intent>()
        
        for (activity in getActivities()) {
            val intent = projectionManager.createScreenCaptureIntent()
            intents.add(intent)
        }
        
        return intents.isNotEmpty()
    }

    fun initialize(projection: MediaProjection): Bitmap? {
        release()

        val windowManager = context.getSystemService(WindowManager::class.java)
        val display = windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        handlerThread = HandlerThread("ScreenshotThread").apply { start() }
        handler = Handler(handlerThread!!.looper)

        imageReader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            2
        )

        mediaProjection = projection
        isCapturing = true

        return null
    }

    fun capture(): Bitmap? {
        if (!isCapturing || imageReader == null) return null

        val surface = imageReader?.surface ?: return null
        
        try {
            mediaProjection?.createVirtualDisplay(
                "Screenshot",
                surface.width,
                surface.height,
                surface.allocation,
                surface.allocation,
                surface,
                null,
                handler
            )

            val image = imageReader?.acquireLatestImage()
            return image?.let { img ->
                val planes = img.planes
                val buffer: ByteBuffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * img.width

                val bitmap = Bitmap.createBitmap(
                    img.width + rowPadding / pixelStride,
                    img.height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                img.close()

                if (rowPadding > 0) {
                    Bitmap.createBitmap(bitmap, 0, 0, img.width, img.height)
                } else {
                    bitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun release() {
        isCapturing = false
        mediaProjection?.stop()
        mediaProjection = null
        imageReader?.close()
        imageReader = null
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
    }

    private fun getActivities(): List<Activity> {
        return listOf()
    }

    companion object {
        @Volatile
        private var instance: ScreenshotCapture? = null

        fun getInstance(context: Context): ScreenshotCapture {
            return instance ?: synchronized(this) {
                instance ?: ScreenshotCapture(context.applicationContext).also { instance = it }
            }
        }
    }
}