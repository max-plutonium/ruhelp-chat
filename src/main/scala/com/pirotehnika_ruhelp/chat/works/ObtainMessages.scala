package com.pirotehnika_ruhelp.chat
package works

import android.util.Log
import org.jsoup.{Connection, Jsoup}

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
    override def run(): Unit = {
      var interval = getMsgInterval

      try {
        doRequest(getTimeout) foreach { messages =>
          guiHandler sendMessage messages }

      } catch {
        case e: java.net.SocketTimeoutException =>
          Log w(TAG, "Timeout on request new messages")
          interval = 1000

        case e: java.io.IOException =>
          Log e(TAG, "Check for new messages failure, caused: \""
            + e.getMessage + "\" by: " + e.getCause)
          e printStackTrace()
          exitUser(R.string.chat_error_network)

      } finally if(userEntered) {
        workerHandler postDelayed(this, interval)
      }
    }

    private def doRequest(timeout: Int): Option[Messages] = {
      val url = siteUrl + "?s=" + chatCookies.get("session_id") +
        "&app=shoutbox&module=ajax&section=coreAjax" +
        "&secure_key=" + secureHash + "&type=getShouts" +
        (if (lastMsgId.isEmpty) "" else "&lastid=" + lastMsgId)

      Log i(TAG, "Request for new messages from " + url)
      val resp = Jsoup.connect(url).cookies(chatCookies)
        .method(Connection.Method.GET).timeout(timeout).execute()

      Log i(TAG, "Connected to " + url)
      Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

      val doc = resp parse(); val body = doc.body.html
      if (body equals getString(R.string.key_body_no_permission)) {
        exitUser(R.string.chat_user_not_entered)
        return None

      } else if (body.isEmpty) {
        Log i(TAG, "There are no new messages on server")
        if(inAutoLogin) enterUser()
        return None
      }

      if(inAutoLogin) enterUser()
      val messages = extractMessages(doc)
      lastMsgId = messages.last.id
      Log i(TAG, "Obtained " + messages.size + " new messages")
      Some(Messages(messages))
    }
  }
}
