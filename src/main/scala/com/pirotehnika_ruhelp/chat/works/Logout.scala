package com.pirotehnika_ruhelp.chat
package works

import android.util.Log
import org.jsoup.{Connection, Jsoup}

private[works] trait Logout extends NetWork {
  private val TAG = classOf[Logout].getName

  override protected final val performLogout = new Runnable {
    private final def publishProgress(msg: String) =
      Chat.handler sendMessage new UpdateProgress(msg)

    override def run() = doLogout(enterUrl, getTimeout)

    private def doLogout(baseUrl: String, timeout: Int) = {
      import collection.JavaConversions.mapAsJavaMap
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
        Log i(TAG, "Logout successful")

      } catch {
        case e: java.net.SocketTimeoutException =>
          Log w(TAG, "Timeout on logout")
          exitUser(R.string.chat_error_network_timeout)

        case e: java.io.IOException =>
          handleNetworkError(TAG, "Logout", e)
      }
    }
  }
}
