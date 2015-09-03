/*
 * Copyright (C) 2014-2015 Max Plutonium <plutonium.max@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE ABOVE LISTED COPYRIGHT HOLDER(S) BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name(s) of the above copyright
 * holders shall not be used in advertising or otherwise to promote the
 * sale, use or other dealings in this Software without prior written
 * authorization.
 */
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
