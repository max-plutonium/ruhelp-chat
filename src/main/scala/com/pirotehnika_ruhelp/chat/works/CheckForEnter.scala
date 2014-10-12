package com.pirotehnika_ruhelp.chat
package works

import android.util.Log
import org.jsoup.Jsoup

protected[chat] trait CheckForEnter extends NetworkWorker {
  this: Chat =>
  private val TAG = classOf[CheckForEnter].getName

  override protected final val checkForEnter = new Runnable {
    override def run(): Unit = {
      try {
        guiHandler sendMessage doCheck(getTimeout)

      } catch {
        case e: java.net.SocketTimeoutException =>
          Log w(TAG, "Timeout on check for user enter")
          workerHandler post this
      }
    }

    private def doCheck(timeout: Int): MessageForGui = {
      restoreCookies()
      val url = siteUrl + "?s=" + chatCookies.get("session_id") +
        "&app=shoutbox&module=ajax&section=coreAjax" +
        "&secure_key=" + secureHash + "&type=getMembers"

      try {
        Log i(TAG, "Check for user already entered from " + url)

        // Не используем user-agent, т.к. будет недоступен чат вообще
        val body = Jsoup.connect(url).cookies(chatCookies)
          .timeout(timeout).ignoreContentType(true).get().body.html

        if(body equals getString(R.string.key_body_no_permission)) {
          Log i(TAG, "User is not entered because cookies are invalid")
          exitUser()
          return AlreadyEntered(result = false)
        }

        val usersRaw = body.replaceAll("&quot;", "").replaceAll("\\\\", "").
          replaceAll("&lt;", "<").replaceAll("&gt;", ">")
        val total = "TOTAL:(\\d+)".r.findFirstIn(usersRaw)
        val names = """NAMES:\[\n(.+)\]""".r.findFirstIn(usersRaw)
        val guests = "GUESTS:(\\d+)".r.findFirstIn(usersRaw)
        val members = "MEMBERS:(\\d+)".r.findFirstIn(usersRaw)
        val anon = "ANON:(\\d+)".r.findFirstIn(usersRaw)
        enterUser()
        Log i(TAG, "Login successful because cookies still valid")
        AlreadyEntered(result = true)

      } catch {
        case e: java.net.SocketTimeoutException =>
          throw e

        case e: org.jsoup.HttpStatusException =>
          Log w(TAG, "Not connected to " + e.getUrl)
          Log w(TAG, "Status code [" + e.getStatusCode + "]")
          exitUser()
          AlreadyEntered(result = false,
            errorId = R.string.chat_error_network_bad_request)

        case e: java.io.IOException =>
          Log e(TAG, "Check for user enter failure, caused: \""
            + e.getMessage + "\" by: " + e.getCause)
          e printStackTrace()
          exitUser()
          AlreadyEntered(result = false,
            errorId = R.string.chat_error_network)
      }
    }
  }
}
