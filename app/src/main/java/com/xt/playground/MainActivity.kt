package com.xt.playground

import android.graphics.Color
import android.graphics.PorterDuff
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import com.opensource.xt.core.XTContext
import com.opensource.xt.core.XTDebug
import com.opensource.xt.foundation.XTFoundationContext
import com.opensource.xt.uikit.XTUIContext

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

}
