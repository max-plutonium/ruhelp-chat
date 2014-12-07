package com.pirotehnika_ruhelp.chat
package works

import java.util.concurrent.TimeUnit

import android.util.Log
import org.jsoup.Jsoup

private[works] trait ObtainMessages extends NetWork {
  private val TAG = classOf[ObtainMessages].getName

  override protected def enterUser() = {
    Chat.network execute checkMessages
    super.enterUser()
  }

  override protected final val checkMessages: Runnable = new Runnable {
    override def run() = { var interval = getMsgInterval
      try {
        if(inAutoLogin || userEntered)
          doRequest(getTimeout) foreach { messages =>
            Chat.handler sendMessage messages }

      } catch {
        case e: java.net.SocketTimeoutException =>
          Log w(TAG, "Timeout on obtaining new messages")
          interval = 1000

        case e: java.io.IOException =>
          handleNetworkError(TAG, "Obtaining new messages", e)

      } finally if(userEntered) {
        Chat.networkSched.schedule(this, interval, TimeUnit.MILLISECONDS)
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
