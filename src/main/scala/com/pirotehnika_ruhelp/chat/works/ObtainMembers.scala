package com.pirotehnika_ruhelp.chat
package works

import android.util.Log
import org.jsoup.Jsoup

protected[chat] trait ObtainMembers extends NetworkWorker {
  this: Chat =>
  private val TAG = classOf[ObtainMembers].getName

  override protected def enterUser() = {
    workerHandler post checkMembers
    super.enterUser()
  }

  override protected def exitUser(errorId: Int = -1, errorMsg: String = "") = {
    workerHandler removeCallbacks checkMembers
    super.exitUser(errorId, errorMsg)
  }

  override protected final val checkMembers: Runnable = new Runnable {
    override def run() = { var interval = getMsgInterval * 3
      try {
        doRequest(getTimeout) foreach { members =>
          guiHandler sendMessage members }

      } catch {
        case e: java.net.SocketTimeoutException =>
          Log w(TAG, "Timeout on obtaining members")
          interval = 1000

        case e: java.io.IOException =>
          handleNetworkError(TAG, "Obtaining members", e)

      } finally if(userEntered) {
        workerHandler postDelayed(this, interval)
      }
    }

    private def doRequest(timeout: Int): Option[Members] = {
      val url = siteUrl + "?s=" + chatCookies.get("session_id") +
        "&app=shoutbox&module=ajax&section=coreAjax" +
        "&secure_key=" + secureHash + "&type=getMembers"

      Log d(TAG, "Obtaining members from " + url)
      val body = Jsoup.connect(url).cookies(chatCookies)
        .timeout(timeout).ignoreContentType(true).get().body.html

      if (body equals getString(R.string.key_body_no_permission)) {
        Log e(TAG, "Obtained no-permission error")
        exitUser(R.string.chat_error_no_permission)
        return None
      }

      val membersRawInfo = body.replaceAll("&quot;", "").replaceAll("\\\\", "").
        replaceAll("&lt;", "<").replaceAll("&gt;", ">")
      val total = "\\d+".r.findFirstIn("TOTAL:\\d+".r.findFirstIn(membersRawInfo).get).get
      val guests = "\\d+".r.findFirstIn("GUESTS:\\d+".r.findFirstIn(membersRawInfo).get).get
      val members = "\\d+".r.findFirstIn("MEMBERS:\\d+".r.findFirstIn(membersRawInfo).get).get
      val anons = "\\d+".r.findFirstIn("ANON:\\d+".r.findFirstIn(membersRawInfo).get).get

      val names = "NAMES:\\[\n(.+)\\]".r.findFirstIn(membersRawInfo) map {
        names => names.substring(8, names.size - 1) } map { _.split(",") }

      val users = names map (_ map { name => val elem = Jsoup parse name select "a"
        Member(elem.html, elem attr "href", elem attr "title") } toIndexedSeq)

      Some(Members(Integer.parseInt(total),
        Integer.parseInt(guests), Integer.parseInt(members),
        Integer.parseInt(anons), users))
    }
  }
}