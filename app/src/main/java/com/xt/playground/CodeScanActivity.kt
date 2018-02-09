package com.xt.playground

import android.app.Activity
import android.graphics.*
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.TextView
import com.dlazaro66.qrcodereaderview.QRCodeReaderView

/**
 * Created by cuiminghui on 2018/2/9.
 */
/**
 * Created by keepjacky on 2017/10/24.
 */
class CodeScanActivity : Activity(), QRCodeReaderView.OnQRCodeReadListener {

    private var barcodeScanned = false
    private var readerView: QRCodeReaderView? = null
    private var centerView: View? = null
    private var hasInit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "扫码"
        setContentView(R.layout.qrscan)
        readerView = findViewById(R.id.qrdecoderview) as? QRCodeReaderView
        readerView?.setOnQRCodeReadListener(this)
        readerView?.setQRDecodingEnabled(true)
        readerView?.setAutofocusInterval(2000L)
        readerView?.setBackCamera()
    }

    override fun onQRCodeRead(text: String?, points: Array<out PointF>?) {
        if (barcodeScanned) return
        val text = text ?: return
        MainActivity.onQRCode?.invoke(text)
        barcodeScanned = true
        finish()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !hasInit){
            setupView()
            hasInit = true
        }
    }

    fun setupView(){
        val scanRectLeftOrRightPadding = 25
        val contentRect = Rect()
        window.findViewById<View>(Window.ID_ANDROID_CONTENT).getDrawingRect(contentRect)
        val screenSize = Point()
        windowManager.defaultDisplay.getSize(screenSize)
        val scale = resources.displayMetrics.density
        val scanAreaWidth = Math.ceil(screenSize.x.toDouble() - scanRectLeftOrRightPadding * 2 * scale)
        val maskViewTop = View(this)
        val maskViewTopHeight = ((contentRect.height() - scanAreaWidth) / 2.0 - 50 * scale)
        addContentView(maskViewTop, ViewGroup.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, maskViewTopHeight.toInt()))
        maskViewTop.setBackgroundColor(Color.parseColor("#80000000"))
        maskViewTop.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            maskViewTop.x = 0.0f
            maskViewTop.y = 0.0f
        }
        val maskViewLeft = View(this)
        addContentView(maskViewLeft, ViewGroup.LayoutParams((scanRectLeftOrRightPadding * scale).toInt(), (scanAreaWidth + 1).toInt()))
        maskViewLeft.setBackgroundColor(Color.parseColor("#80000000"))
        maskViewLeft.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            maskViewLeft.x = 0.0f
            maskViewLeft.y = maskViewTop.height.toFloat()
        }
        val maskViewRight = View(this)
        addContentView(maskViewRight, ViewGroup.LayoutParams((scanRectLeftOrRightPadding * scale).toInt(), (scanAreaWidth + 1).toInt()))
        maskViewRight.setBackgroundColor(Color.parseColor("#80000000"))
        maskViewRight.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            maskViewRight.x = screenSize.x - (maskViewRight.width).toFloat()
            maskViewRight.y = maskViewTop.height.toFloat()
        }
        val maskViewBottom = View(this)
        val maskViewBottomHeight = ((contentRect.height() - scanAreaWidth) / 2.0 + 50 * scale)
        addContentView(maskViewBottom, ViewGroup.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, maskViewBottomHeight.toInt()))
        maskViewBottom.setBackgroundColor(Color.parseColor("#80000000"))
        maskViewBottom.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            maskViewBottom.x = 0.0f
            maskViewBottom.y = (contentRect.height() - maskViewBottom.height).toFloat()
        }
        val textView = TextView(this)
        textView.text = "将二维码放入框内，即可扫描"
        textView.setBackgroundColor(Color.TRANSPARENT)
        textView.textSize = 13.0f
        textView.setTextColor(Color.WHITE)
        addContentView(textView, ViewGroup.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        textView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            textView.x = screenSize.x / 2.0f - textView.width / 2.0f
            textView.y = (maskViewTopHeight.toFloat() + scanAreaWidth + 10.0f).toFloat()
        }
        val centerView = centerView()
        centerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            centerView.x = maskViewLeft.width.toFloat()
            centerView.y = maskViewTop.height.toFloat()
            centerView.invalidate()
        }
        addContentView(centerView, ViewGroup.LayoutParams(scanAreaWidth.toInt(), scanAreaWidth.toInt()))
    }

    fun centerView(): View {
        this.centerView?.let {
            return it
        }
        val view = object: View(this) {
            override fun draw(canvas: Canvas?) {
                super.draw(canvas)
                canvas?.let { canvas ->
                    val paint = Paint()
                    paint.color = 0xff009fff.toInt()
                    canvas.drawRect(0f, 0f, (20 * resources.displayMetrics.density), (2 * resources.displayMetrics.density), paint)
                    canvas.drawRect(0f, 0f, (2 * resources.displayMetrics.density), (20 * resources.displayMetrics.density), paint)
                    canvas.drawRect(canvas.width - (20 * resources.displayMetrics.density), 0f, canvas.width.toFloat(), (2 * resources.displayMetrics.density), paint)
                    canvas.drawRect(canvas.width - (2 * resources.displayMetrics.density), 0f, canvas.width.toFloat(), (20 * resources.displayMetrics.density), paint)
                    canvas.drawRect(0f, canvas.height - (20 * resources.displayMetrics.density), (2 * resources.displayMetrics.density), canvas.height.toFloat(), paint)
                    canvas.drawRect(0f, canvas.height - (2 * resources.displayMetrics.density), (20 * resources.displayMetrics.density), canvas.height.toFloat(), paint)
                    canvas.drawRect(canvas.width - (20 * resources.displayMetrics.density), canvas.height - (2 * resources.displayMetrics.density), canvas.width.toFloat(), canvas.height.toFloat(), paint)
                    canvas.drawRect(canvas.width - (2 * resources.displayMetrics.density), canvas.height - (20 * resources.displayMetrics.density), canvas.width.toFloat(), canvas.height.toFloat(), paint)
                }
            }
        }
        this.centerView = view
        return view
    }

}