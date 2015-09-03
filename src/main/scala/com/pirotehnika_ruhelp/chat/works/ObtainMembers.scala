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

private[works] trait ObtainMembers extends NetWork {
  private val TAG = classOf[ObtainMembers].getName

  override protected def enterUser() = {
    execute(checkMembers)
    super.enterUser()
  }

  override protected def exitUser(errorId: Int = -1, errorMsg: String = "") = {
    stopTask(checkMembers)
    super.exitUser(errorId, errorMsg)
  }

  override protected final val checkMembers: Runnable = new Runnable {
    override def run() = { var interval = getMsgInterval * 3
      try {
        if(userEntered)
          doRequest(getTimeout) foreach { members =>
            Chat.handler sendMessage members }

      } catch {
        case e: java.net.SocketTimeoutException =>
          Log w(TAG, "Timeout on obtaining members")
          interval = 1000

        case e: java.io.IOException =>
          handleNetworkError(TAG, "Obtaining members", e)

      } finally if(userEntered) {
        schedule(this, interval)
      }
    }

    private def doRequest(timeout: Int): Option[Members] = {
      import collection.JavaConversions.mapAsJavaMap
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
