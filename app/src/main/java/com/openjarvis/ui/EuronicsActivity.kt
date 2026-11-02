package com.openjarvis.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.os.Bundle
import android.util.Log
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
import androidx.activity.result.ActivityResult
import android.content.Intent
import java.io.File
import java.io.FileOutputStream

class EuronicsActivity : ComponentActivity() {
    companion object {
        private const val DOWNLOAD_BRIDGE_SCRIPT = """
            (function() {
              if (window.__euronicsDownloadBridgeInstalled) return;
              window.__euronicsDownloadBridgeInstalled = true;
              document.addEventListener('click', function(event) {
                var anchor = event.target && event.target.closest ? event.target.closest('a') : null;
                if (!anchor || !anchor.href || !anchor.href.startsWith('blob:')) return;
                event.preventDefault();
                var name = anchor.getAttribute('download') || 'Euronics_Report';
                fetch(anchor.href).then(function(response) { return response.blob(); }).then(function(blob) {
                  var reader = new FileReader();
                  reader.onloadend = function() {
                    var data = String(reader.result || '').split(',')[1] || '';
                    if (window.AndroidEuronics && data) AndroidEuronics.saveFile(name, blob.type || 'application/octet-stream', data);
                  };
                  reader.readAsDataURL(blob);
                }).catch(function(error) { console.error('Euronics download failed', error); });
              }, true);
            })();
        """
    }
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var reportWebView: WebView

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        val data = result.data
        val uris = if (result.resultCode == RESULT_OK && data != null) {
            val clip = data.clipData
            when {
                clip != null -> Array(clip.itemCount) { index -> clip.getItemAt(index).uri }
                data.data != null -> arrayOf(data.data!!)
                else -> null
            }
        } else null
        pendingFileCallback?.onReceiveValue(uris)
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
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript("document.title") { title ->
                    Log.i("EuronicsWebView", "EURONICS_HTML_TITLE:$title")
                }
                view?.evaluateJavascript(DOWNLOAD_BRIDGE_SCRIPT, null)
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
                val accept = fileChooserParams?.acceptTypes
                    ?.flatMap { it.split(",") }
                    ?.map { it.trim() }
                    ?.firstOrNull { it.isNotBlank() } ?: "*/*"
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = if (accept.startsWith("image/")) "image/*" else accept
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                }
                filePicker.launch(Intent.createChooser(intent, "Choose Euronics file"))
                return true
            }
        }
        reportWebView.addJavascriptInterface(EuronicsDownloadBridge(this), "AndroidEuronics")
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

    private inner class EuronicsDownloadBridge(private val context: Context) {
        @JavascriptInterface
        fun saveFile(fileName: String, mimeType: String, base64Data: String) {
            try {
                val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
                    .ifBlank { "Euronics_Report" }
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Euronics")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("Could not create Downloads file")
                    contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: throw IllegalStateException("Could not write Downloads file")
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(uri, values, null, null)
                } else {
                    val dir = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Euronics")
                    if (!dir.exists()) dir.mkdirs()
                    FileOutputStream(File(dir, safeName)).use { it.write(bytes) }
                }
                runOnUiThread { Toast.makeText(context, "Saved in Downloads/Euronics/$safeName", Toast.LENGTH_LONG).show() }
            } catch (e: Exception) {
                Log.e("EuronicsWebView", "Report download failed", e)
                runOnUiThread { Toast.makeText(context, "Report download failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    override fun onDestroy() {
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = null
        reportWebView.stopLoading()
        reportWebView.destroy()
        super.onDestroy()
    }
}
