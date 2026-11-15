package com.autoscroll.prov2

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlInput: EditText
    private var isWebAutoRunning = false
    private var webSpeed = 3
    private var webAutoClick = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#181825"))
        }

        // --- THANH CÔNG CỤ TRÊN CÙNG (CHUYỂN ĐỔI PHƯƠNG ÁN A & B) ---
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 16)
            setBackgroundColor(Color.parseColor("#1e1e2e"))
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleTv = TextView(this).apply {
            text = "⚡ AutoScroll Pro v2 (2-trong-1)"
            setTextColor(Color.parseColor("#89b4fa"))
            textSize = 16f
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val overlayBtn = Button(this).apply {
            text = "🚀 Bật Nút Nổi Toàn Máy (PA B)"
            textSize = 11f
            setTextColor(Color.parseColor("#11111b"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#f9e2af"))
                cornerRadius = 20f
            }
            setPadding(24, 10, 24, 10)
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Hãy tìm 'AutoScroll Pro v2' trong danh sách Dịch vụ đã tải xuống và BẬT lên để hiện nút nổi!",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        titleRow.addView(titleTv)
        titleRow.addView(overlayBtn)

        // Thanh nhập địa chỉ Web (Phương án A)
        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 16, 0, 0)
        }

        urlInput = EditText(this).apply {
            hint = "Nhập link web hoặc từ khóa Google..."
            setHintTextColor(Color.parseColor("#6c7086"))
            setTextColor(Color.parseColor("#cdd6f4"))
            textSize = 13f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_GO
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#313244"))
                cornerRadius = 16f
            }
            setPadding(28, 18, 28, 18)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = 16
            }
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                    loadTargetUrl(text.toString())
                    true
                } else false
            }
        }

        val goBtn = Button(this).apply {
            text = "Đi"
            textSize = 13f
            setTextColor(Color.parseColor("#11111b"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#89b4fa"))
                cornerRadius = 16f
            }
            setOnClickListener { loadTargetUrl(urlInput.text.toString()) }
        }

        addressRow.addView(urlInput)
        addressRow.addView(goBtn)

        topBar.addView(titleRow)
        topBar.addView(addressRow)

        // --- WEBVIEW TRÌNH DUYỆT (PHƯƠNG ÁN A) ---
        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.userAgentString = settings.userAgentString.replace("; wv", "")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    urlInput.setText(url ?: "")
                    if (isWebAutoRunning) {
                        injectWebAutoScroll()
                    }
                }
            }
            webChromeClient = WebChromeClient()
        }

        // --- BẢNG ĐIỀU KHIỂN TRÌNH DUYỆT TÍCH HỢP (GIỐNG HỆT POPUP.HTML) ---
        val bottomPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 20, 28, 24)
            setBackgroundColor(Color.parseColor("#1e1e2e"))
        }

        val controlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val startStopBtn = Button(this).apply {
            text = "▶ BẮT ĐẦU CHẠY (TRONG WEB)"
            textSize = 13f
            setTextColor(Color.parseColor("#11111b"))
            paint.isFakeBoldText = true
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#a6e3a1"))
                cornerRadius = 20f
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = 16
            }
        }

        val autoClickCheck = CheckBox(this).apply {
            text = "🎯 Tự ấn màn hình"
            setTextColor(Color.parseColor("#cdd6f4"))
            textSize = 12f
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                webAutoClick = isChecked
                if (isWebAutoRunning) injectWebAutoScroll()
            }
        }

        controlRow.addView(startStopBtn)
        controlRow.addView(autoClickCheck)

        val speedRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 14, 0, 0)
        }

        val speedLabel = TextView(this).apply {
            text = "Tốc độ cuộn: 3"
            setTextColor(Color.parseColor("#a6adc8"))
            textSize = 12f
        }

        val speedSeek = SeekBar(this).apply {
            max = 9
            progress = 2
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    webSpeed = progress + 1
                    speedLabel.text = "Tốc độ cuộn: $webSpeed"
                    if (isWebAutoRunning) injectWebAutoScroll()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        speedRow.addView(speedLabel)
        speedRow.addView(speedSeek)

        startStopBtn.setOnClickListener {
            isWebAutoRunning = !isWebAutoRunning
            if (isWebAutoRunning) {
                startStopBtn.text = "⏹ DỪNG LẠI"
                startStopBtn.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#f38ba8"))
                    cornerRadius = 20f
                }
                injectWebAutoScroll()
            } else {
                startStopBtn.text = "▶ BẮT ĐẦU CHẠY (TRONG WEB)"
                startStopBtn.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#a6e3a1"))
                    cornerRadius = 20f
                }
                stopWebAutoScroll()
            }
        }

        bottomPanel.addView(controlRow)
        bottomPanel.addView(speedRow)

        root.addView(topBar)
        root.addView(webView)
        root.addView(bottomPanel)

        setContentView(root)
        webView.loadUrl("https://www.google.com")
    }

    private fun loadTargetUrl(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return
        val finalUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
            "https://$trimmed"
        } else {
            "https://www.google.com/search?q=" + android.net.Uri.encode(trimmed)
        }
        webView.loadUrl(finalUrl)
    }

    // Nhúng chính xác thuật toán từ popup.js của bạn vào trang web
    private fun injectWebAutoScroll() {
        val jsCode = """
            (function(speed, enableAutoClick) {
                if (window.__pageAutoScroller) cancelAnimationFrame(window.__pageAutoScroller);
                if (window.__pageAutoClickTimer) clearInterval(window.__pageAutoClickTimer);

                let dir = 1;
                let pausing = false;

                if (enableAutoClick) {
                    const keywords = [
                        'ấn vào màn hình',
                        'chạm vào màn hình',
                        'nhấn vào màn hình',
                        'bấm vào màn hình',
                        'nhấn để tiếp tục',
                        'chạm để tiếp tục',
                        'bấm để tiếp tục',
                        'ấn để tiếp tục',
                        'click to continue',
                        'tap to continue'
                    ];

                    function checkAndClick() {
                        const candidates = document.querySelectorAll('button, a, div, span, p, h1, h2, h3, h4, h5, h6, label');
                        for (const el of candidates) {
                            if (el.children.length > 4) continue;
                            const text = (el.innerText || el.textContent || '').trim().toLowerCase();
                            if (!text) continue;

                            const isMatch = keywords.some(kw => text.includes(kw));
                            if (isMatch) {
                                const rect = el.getBoundingClientRect();
                                const style = window.getComputedStyle(el);
                                if (rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0') {
                                    ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(evtType => {
                                        el.dispatchEvent(new MouseEvent(evtType, { bubbles: true, cancelable: true, view: window }));
                                    });
                                    if (typeof el.click === 'function') el.click();
                                    break;
                                }
                            }
                        }
                    }
                    window.__pageAutoClickTimer = setInterval(checkAndClick, 400);
                    checkAndClick();
                }

                function scrollLoop() {
                    if (!pausing) {
                        window.scrollBy(0, dir * speed);
                        const atBottom = window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 2;
                        const atTop = window.scrollY <= 0;

                        if (dir === 1 && atBottom) {
                            pausing = true;
                            setTimeout(() => { dir = -1; pausing = false; }, 1200);
                        } else if (dir === -1 && atTop) {
                            pausing = true;
                            setTimeout(() => { dir = 1; pausing = false; }, 1200);
                        }
                    }
                    window.__pageAutoScroller = requestAnimationFrame(scrollLoop);
                }
                window.__pageAutoScroller = requestAnimationFrame(scrollLoop);
            })($webSpeed, $webAutoClick);
        """.trimIndent()
        webView.evaluateJavascript(jsCode, null)
    }

    private fun stopWebAutoScroll() {
        val stopJs = """
            (function() {
                if (window.__pageAutoScroller) {
                    cancelAnimationFrame(window.__pageAutoScroller);
                    window.__pageAutoScroller = null;
                }
                if (window.__pageAutoClickTimer) {
                    clearInterval(window.__pageAutoClickTimer);
                    window.__pageAutoClickTimer = null;
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(stopJs, null)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
