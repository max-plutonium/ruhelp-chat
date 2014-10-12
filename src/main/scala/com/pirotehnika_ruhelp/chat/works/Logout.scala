package com.pirotehnika_ruhelp.chat
package works

import android.util.Log
import org.jsoup.{Connection, Jsoup}

protected[chat] trait Logout extends NetworkWorker {
  this: Chat =>
  private val TAG = classOf[Logout].getName

  override protected final val performLogout = new Runnable {
    private final def publishProgress(msg: String) =
      guiHandler sendMessage new UpdateProgress(msg)

    override def run(): Unit =
      guiHandler sendMessage doLogout(enterUrl, getTimeout)

    private def doLogout(baseUrl: String, timeout: Int): MessageForGui = {
      val url = baseUrl + "&do=logout&k=" + secureHash

      try {
        publishProgress("Logout from forum...")
        Log i(TAG, "Logout from " + url)

        val resp = Jsoup.connect(url).cookies(chatCookies)
          .userAgent(getUserAgent).method(Connection.Method.GET)
          .timeout(timeout).execute()

        Log i(TAG, "Connected to " + url)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        exitUser()
        publishProgress("Logout success")
        Log i(TAG, "Logout successful")
        LogoutResult(result = true)

      } catch { case e: java.io.IOException =>
        Log e(TAG, "Logout failure, caused: " + e.getMessage)
        e printStackTrace()
        LogoutResult(result = false)
      }
    }
  }
}
