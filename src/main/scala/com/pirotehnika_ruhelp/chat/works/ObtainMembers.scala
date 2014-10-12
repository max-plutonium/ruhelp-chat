package com.pirotehnika_ruhelp.chat
package works

import android.util.Log
import org.jsoup.Jsoup

protected[chat] trait ObtainMembers extends NetworkWorker {
  this: Chat =>
  private val TAG = classOf[ObtainMembers].getName

  override protected final val checkMembers = new Runnable {
    override def run(): Unit = {
      try {
        guiHandler sendMessage doRequest(getTimeout)

      } catch {
        case e: java.net.SocketTimeoutException =>
          Log w(TAG, "Timeout on request for members")
          workerHandler post this

        case e: java.io.IOException =>
          Log e(TAG, "Check for user enter failure, caused: \""
            + e.getMessage + "\" by: " + e.getCause)
          e printStackTrace()
          exitUser(R.string.chat_error_network)
      }
    }

    private def doRequest(timeout: Int): MessageForGui = {
      val url = siteUrl + "?s=" + chatCookies.get("session_id") +
        "&app=shoutbox&module=ajax&section=coreAjax" +
        "&secure_key=" + secureHash + "&type=getMembers"

      Log i(TAG, "Request for members from " + url)

      // Не используем user-agent, т.к. будет недоступен чат вообще
      val body = Jsoup.connect(url).cookies(chatCookies)
        .timeout(timeout).ignoreContentType(true).get().body.html

      if (body equals getString(R.string.key_body_no_permission)) {
        Log e(TAG, "Obtained no-permission error")
        exitUser()
        val noPermMessage = Message("not entered",
          getString(R.string.key_system_user), "",
          getString(R.string.chat_error_no_permission))
        return Messages(Seq(noPermMessage))
      }

      val membersRawInfo = body.replaceAll("&quot;", "").replaceAll("\\\\", "").
        replaceAll("&lt;", "<").replaceAll("&gt;", ">")
      val total = "\\d+".r.findFirstIn("TOTAL:\\d+".r.findFirstIn(membersRawInfo).get).get
      val guests = "\\d+".r.findFirstIn("GUESTS:\\d+".r.findFirstIn(membersRawInfo).get).get
      val members = "\\d+".r.findFirstIn("MEMBERS:\\d+".r.findFirstIn(membersRawInfo).get).get
      val anons = "\\d+".r.findFirstIn("ANON:\\d+".r.findFirstIn(membersRawInfo).get).get

      val names = "NAMES:\\[\n(.+)\\]".r.findFirstIn(membersRawInfo) map {
        names => names.substring(8, names.size - 1)
      } map {
        _.split(",")
      }

      val users = names map (_ map { name => val elem = Jsoup parse name select "a"
        Member(elem.html, elem attr "href", elem attr "title")
      } toIndexedSeq)

      Members(Integer.parseInt(total),
        Integer.parseInt(guests), Integer.parseInt(members),
        Integer.parseInt(anons), users)
    }
  }
}
