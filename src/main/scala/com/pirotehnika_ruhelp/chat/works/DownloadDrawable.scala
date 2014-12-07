package com.pirotehnika_ruhelp.chat
package works

import android.graphics.drawable.Drawable
import android.util.Log
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient

import scala.concurrent.Future

private[works] trait DownloadDrawable extends NetWork {
  private val TAG = classOf[DownloadDrawable].getName

  override final def downloadDrawable(url: String) = Future {
    try {
      val resp = new DefaultHttpClient() execute new HttpGet(url)
      Drawable.createFromStream(resp.getEntity.getContent, null)

    } catch {
      case e: java.io.IOException =>
        Log e(TAG, "Drawable downloading fails, cause: " + e.getMessage)
        e printStackTrace()
        null
    }
  } (this)
}
