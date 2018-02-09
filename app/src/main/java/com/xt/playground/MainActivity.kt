package com.xt.playground

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import com.opensource.xt.core.XTContext
import com.opensource.xt.core.XTDebug
import com.opensource.xt.foundation.XTFoundationContext
import com.opensource.xt.uikit.XTUIContext
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import java.util.zip.Inflater

class MainActivity : AppCompatActivity() {

    init {
        XTUIContext.addDefaultAttachContext(XTFoundationContext::class.java as Class<XTContext>)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onStart(view: View) {
        val dialog = AlertDialog.Builder(this)
                    .setItems(arrayOf("Run Sample", "Connect To Debugger", "Scan QRCode"), { _, idx ->
                        when (idx) {
                            0 -> {
                                XTUIContext.createWithAssets(this, "sample.min.js", {
                                    it.start()
                                })
                            }
                            1 -> {
                                onDebug()
                            }
                            2 -> {
                                startScanner()
                            }
                        }
                    })
                    .create()
        dialog.window.setGravity(Gravity.BOTTOM)
        dialog.show()
    }

    fun onDebug() {
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        val editText = EditText(this)
        editText.setSingleLine(true)
        editText.imeOptions = EditorInfo.IME_ACTION_GO
        editText.text.clear()
        editText.text.append(this.getSharedPreferences("Sample", android.content.Context.MODE_PRIVATE).getString("DEBUG_ADDRESS", "10.0.2.2:8081"))
        dialogBuilder.setTitle("Enter IP:Port")
        dialogBuilder.setPositiveButton("确认", { _, _ ->
            startDebugWithAddress(editText.text.toString())
        })
        dialogBuilder.setNegativeButton("取消", { _, _ -> })
        dialogBuilder.setCancelable(false)
        val dialog = dialogBuilder.create()
        editText.setOnEditorActionListener { _, _, _ ->
            dialog.dismiss()
            startDebugWithAddress(editText.text.toString())
            return@setOnEditorActionListener true
        }
        dialog.setView(editText, (20.0 * editText.resources.displayMetrics.density).toInt(), (20.0 * editText.resources.displayMetrics.density).toInt(), (20.0 * editText.resources.displayMetrics.density).toInt(), (20.0 * editText.resources.displayMetrics.density).toInt())
        dialog.show()
    }

    fun startDebugWithAddress(address: String) {
        findViewById<View>(R.id.textView3).visibility = View.GONE
        findViewById<View>(R.id.progressBar).visibility = View.VISIBLE
        (findViewById<ProgressBar>(R.id.progressBar) as ProgressBar)?.indeterminateDrawable?.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY)
        this.getSharedPreferences("Sample", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("DEBUG_ADDRESS", address)
                .apply()
        val IP = address.split(":").firstOrNull() ?: return
        val port = address.split(":").lastOrNull() ?: return
        XTUIContext.currentDebugApplicationContext = this
        XTDebug.sharedDebugger.delegate = XTUIContext
        XTDebug.debugWithIP(IP, port, this)
    }

    fun startScanner() {
        setupQRCodeHandler()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                startScanner()
            } else {
                ActivityCompat.requestPermissions(this, listOf(Manifest.permission.CAMERA).toTypedArray(), 1)
            }
        }
        else {
            startActivity(Intent(this, CodeScanActivity::class.java))
        }
    }

    fun setupQRCodeHandler() {
        onQRCode = {
            try {
                val uri = Uri.parse(it)
                if (uri.query.startsWith("ws://")) {
                    var found = false
                    uri.query.split("|||")
                            .filter { it.isNotEmpty() && it.startsWith("ws://") }
                            .forEach { wsServer ->
                                val wsHostname = wsServer.substring(5).split(":").first()
                                val wsPort = wsServer.substring(5).split(":").last().toInt()
                                val req = Request.Builder()
                                            .url("http://" + wsHostname + ":" + (wsPort + 1) + "/status")
                                            .get()
                                            .build()
                                OkHttpClient().newCall(req).enqueue(object : Callback {
                                    override fun onFailure(call: Call?, e: IOException?) {}
                                    override fun onResponse(call: Call?, response: Response?) {
                                        if (found) { return }
                                        if (response?.body()?.string() == "continue") {
                                            found = true
                                            this@MainActivity.runOnUiThread {
                                                this@MainActivity.startDebugWithAddress("$wsHostname:$wsPort")
                                            }
                                        }
                                    }
                                })
                            }
                }
                else if (uri.query.startsWith("eval=")) {
                    val base64Encoded = uri.query.substring(5).split('&').first()
                    val code = String(this.inflate(Base64.decode(base64Encoded, 0))!!)
                    val tmpFile = File.createTempFile("tmp_", "js")
                    tmpFile.writeText(code)
                    this@MainActivity.runOnUiThread {
                        XTUIContext.createWithSourceURL(this@MainActivity, Uri.fromFile(tmpFile).toString(), {
                            it.start()
                        }, {
                            kotlin.io.println(true)
                        })
                    }
                }
                else if (uri.query.startsWith("url=")) {
                    val req = Request.Builder().url(String(Base64.decode(uri.query.substring(4).split("&").first(), 0))).get().build()
                    OkHttpClient().newCall(req).enqueue(object : Callback {
                        override fun onFailure(call: Call?, e: IOException?) {}
                        override fun onResponse(call: Call?, response: Response?) {
                            response?.body()?.string()?.let { code ->
                                val tmpFile = File.createTempFile("tmp_", "js")
                                tmpFile.writeText(code)
                                this@MainActivity.runOnUiThread {
                                    XTUIContext.createWithSourceURL(this@MainActivity, Uri.fromFile(tmpFile).toString(), {
                                        it.start()
                                    }, {
                                        kotlin.io.println(true)
                                    })
                                }
                            }
                        }
                    })
                }
            } catch (e: Exception) {}
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.firstOrNull() === PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, CodeScanActivity::class.java))
        }
    }

    private fun inflate(byteArray: ByteArray): ByteArray? {
        try {
            val inflater = Inflater()
            inflater.setInput(byteArray, 0, byteArray.size)
            val inflatedBytes = ByteArray(2048)
            val inflatedOutputStream = ByteArrayOutputStream()
            while (true) {
                val count = inflater.inflate(inflatedBytes, 0, 2048)
                if (count <= 0) {
                    break
                }
                else {
                    inflatedOutputStream.write(inflatedBytes, 0, count)
                }
            }
            return inflatedOutputStream.toByteArray()
        } catch (e: Exception) { e.printStackTrace(); }
        return null
    }

    companion object {

        var onQRCode: ((code: String) -> Unit)? = null

    }

}
