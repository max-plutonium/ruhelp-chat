package com.pirotehnika_ruhelp.chat
package works

import android.util.Log
import org.jsoup.Jsoup

protected[chat] trait PostMessage extends NetworkWorker {
  this: Chat =>
  private val TAG = classOf[PostMessage].getName

  override protected final def postMessage(text: String): Unit =
    workerHandler post new PostMessageTask(text)

  private class PostMessageTask(private val text: String) extends Runnable {
    override def run(): Unit = {
      guiHandler sendMessage {
        try {
          val url = siteUrl + "?s=" + chatCookies.get("session_id") +
            "&app=shoutbox&module=ajax&section=coreAjax" +
            "&secure_key=" + secureHash + "&type=submit" +
            (if (lastMsgId.isEmpty) "" else "&lastid=" + lastMsgId)

          Log i(TAG, "Post new message to " + url)
          val doc = Jsoup.connect(url).timeout(getTimeout)
            .data("shout", text).cookies(chatCookies).post()

          val messages = extractMessages(doc)
          lastMsgId = messages.last.id
          Log i(TAG, "New message are posted, obtained: " + (messages.size - 1))
          Messages(messages)

        } catch {
          case e: java.net.SocketTimeoutException =>
            Log w(TAG, "Timeout on post new message")
            PostError(R.string.chat_error_network_timeout)

          case e: org.jsoup.HttpStatusException =>
            Log w(TAG, "Not connected to " + e.getUrl)
            Log w(TAG, "Status code [" + e.getStatusCode + "]")
            exitUser()
            PostError(R.string.chat_error_network_bad_request)

          case e: java.io.IOException =>
            Log e(TAG, "Check for new messages failure, caused: \""
              + e.getMessage + "\" by: " + e.getCause)
            e printStackTrace()
            exitUser()
            PostError(R.string.chat_error_network)
        }
      }
    }
  }
}
