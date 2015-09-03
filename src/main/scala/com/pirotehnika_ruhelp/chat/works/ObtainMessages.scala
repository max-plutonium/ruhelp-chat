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
import org.jsoup.Jsoup

private[works] trait ObtainMessages extends NetWork {
  private val TAG = classOf[ObtainMessages].getName

  override protected def enterUser() = {
    execute(checkMessages)
    super.enterUser()
  }

  override protected def exitUser(errorId: Int = -1, errorMsg: String = "") = {
    stopTask(checkMessages)
    super.exitUser(errorId, errorMsg)
  }

  override protected final val checkMessages: Runnable = new Runnable {
    override def run() = { var interval = getMsgInterval
      try {
        doRequest(getTimeout) foreach { messages =>
          Chat.handler sendMessage messages }

      } catch {
        case e: java.net.SocketTimeoutException =>
          Log w(TAG, "Timeout on obtaining new messages")
          interval = 1000

        case e: java.io.IOException =>
          handleNetworkError(TAG, "Obtaining new messages", e)

      } finally if(userEntered) {
        schedule(this, interval)
      }
    }

    private def doRequest(timeout: Int): Option[Messages] = {
      import collection.JavaConversions.mapAsJavaMap
      val url = siteUrl + "?s=" + chatCookies.get("session_id") +
        "&app=shoutbox&module=ajax&section=coreAjax" +
        "&secure_key=" + secureHash + "&type=getShouts" +
        (if (lastMsgId.isEmpty) "" else "&lastid=" + lastMsgId)

      Log d(TAG, "Obtaining new messages from " + url)
      val doc = Jsoup.connect(url).cookies(chatCookies)
        .timeout(timeout).ignoreContentType(true).get()

      val body = doc.body.html
      if (body equals getString(R.string.key_body_no_permission)) {
        if(inAutoLogin) {
          Log i(TAG, "User is not entered because cookies are invalid")
          exitUser(R.string.chat_user_not_entered)
        } else {
          Log e(TAG, "Obtained no-permission error")
          exitUser(R.string.chat_error_no_permission)
        }
        return None

      } else if (body.isEmpty) {
        Log i(TAG, "There are no new messages on server")
        if(inAutoLogin) {
          Log i(TAG, "Login successful because cookies still valid")
          enterUser()
        }
        return None
      }

      if(inAutoLogin) {
        Log i(TAG, "Login successful because cookies still valid")
        enterUser()
      }

      val messages = extractMessages(doc)
      lastMsgId = messages.last.id

      Log i(TAG, "Obtained " + messages.size + " new messages")
      Some(Messages(messages))
    }
  }
}
