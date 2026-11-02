package com.openjarvis.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.webkit.*
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent

class EuronicsActivity : ComponentActivity() {
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var reportWebView: WebView

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pendingFileCallback?.onReceiveValue(if (uri == null) null else arrayOf(uri))
        pendingFileCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 8, 12, 8)
            setBackgroundColor(android.graphics.Color.rgb(18, 61, 114))
        }
        val back = Button(this).apply {
            text = "‹ Back"
            setOnClickListener { finish() }
        }
        val title = TextView(this).apply {
            text = "Euronics Service Report"
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(12, 4, 4, 4)
        }
        toolbar.addView(back, LinearLayout.LayoutParams(-2, -2))
        toolbar.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))

        reportWebView = WebView(this)
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        reportWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        reportWebView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    Toast.makeText(this@EuronicsActivity, "Euronics page load error: ${error?.description}", Toast.LENGTH_LONG).show()
                }
            }
        }
        reportWebView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                pendingFileCallback?.onReceiveValue(null)
                pendingFileCallback = filePathCallback
                imagePicker.launch("image/*")
                return true
            }
        }
        reportWebView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            try {
                val request = android.app.DownloadManager.Request(Uri.parse(url))
                    .setMimeType(mimeType)
                    .addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                    .setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                    .setDescription("Downloading Euronics report")
                    .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType))
                (getSystemService(DOWNLOAD_SERVICE) as android.app.DownloadManager).enqueue(request)
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        root.addView(reportWebView, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        reportWebView.loadUrl("https://appassets.androidplatform.net/assets/euronics/index.html")
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (reportWebView.canGoBack()) reportWebView.goBack() else finish()
            }
        })
    }

    override fun onDestroy() {
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = null
        reportWebView.stopLoading()
        reportWebView.destroy()
        super.onDestroy()
    }
}
