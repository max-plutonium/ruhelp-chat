package com.pirotehnika_ruhelp.chat
package works

import android.util.Log
import org.jsoup.Jsoup

protected[chat] trait ObtainMessages extends NetworkWorker {
  this: Chat =>
  private val TAG = classOf[ObtainMessages].getName

  override protected def enterUser() = {
    workerHandler post checkMessages
    super.enterUser()
  }

  override protected def exitUser(errorId: Int = -1, errorMsg: String = "") = {
    workerHandler removeCallbacks checkMessages
    super.exitUser(errorId, errorMsg)
  }

  override protected final val checkMessages: Runnable = new Runnable {
    override def run() = { var interval = getMsgInterval
      try {
        doRequest(getTimeout) foreach { messages =>
          guiHandler sendMessage messages }

      } catch {
        case e: java.net.SocketTimeoutException =>
          Log w(TAG, "Timeout on obtaining new messages")
          interval = 1000

        case e: java.io.IOException =>
          handleNetworkError(TAG, "Obtaining new messages", e)

      } finally if(userEntered) {
        workerHandler postDelayed(this, interval)
      }
    }

    private def doRequest(timeout: Int): Option[Messages] = {
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
