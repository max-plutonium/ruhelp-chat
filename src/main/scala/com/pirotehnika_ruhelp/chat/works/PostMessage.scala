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

private[works] trait PostMessage extends NetWork {
  private val TAG = classOf[PostMessage].getName

  override final def postMessage(text: String) =
    execute(new PostMessageTask(text))

  private class PostMessageTask(private val text: String) extends Runnable {
    import collection.JavaConversions.mapAsJavaMap
    override def run() = { Chat.handler sendMessage {
        try {
          val url = siteUrl + "?s=" + chatCookies.get("session_id") +
            "&app=shoutbox&module=ajax&section=coreAjax" +
            "&secure_key=" + secureHash + "&type=submit" +
            (if (lastMsgId.isEmpty) "" else "&lastid=" + lastMsgId)

          Log i(TAG, "Post new message to " + url)
          val doc = Jsoup.connect(url).cookies(chatCookies)
            .data("shout", text).timeout(getTimeout).post()

          val messages = extractMessages(doc)
          lastMsgId = messages.last.id

          Log i(TAG, "New message has been posted" +
            (if(1 == messages.size) "" else " with " +
              (messages.size - 1) + " new messages"))
          Messages(messages)

        } catch {
          case e: java.net.SocketTimeoutException =>
            Log w(TAG, "Timeout on post new message")
            PostError(R.string.chat_error_network_timeout)

          case e: java.io.IOException =>
            handleNetworkError(TAG, "Post message", e)
            PostError(R.string.chat_error_network)
        }
      }
    }
  }
}
