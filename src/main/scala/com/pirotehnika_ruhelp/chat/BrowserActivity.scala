package com.pirotehnika_ruhelp.chat

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView

class BrowserActivity extends Activity {
  private lazy val webView = findViewById(R.id.webView).asInstanceOf[WebView]

  override def onCreate(savedInstanceState: Bundle): Unit = {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.browser)
      webView loadUrl getIntent.getData.toString
    }
}
