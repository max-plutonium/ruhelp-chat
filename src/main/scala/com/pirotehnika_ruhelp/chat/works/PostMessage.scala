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
