package com.pirotehnika_ruhelp.chat

import android.os.Bundle

class BrowserActivity extends TypedActivity {
  private lazy val webView = findView(TR.webView)

  override def onCreate(savedInstanceState: Bundle): Unit = {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.browser)
      webView loadUrl getIntent.getData.toString
    }
}
