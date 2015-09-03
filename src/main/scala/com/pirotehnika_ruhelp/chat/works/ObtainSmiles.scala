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
import org.jsoup.nodes.Element

import scala.concurrent.Future

private[works] trait ObtainSmiles extends NetWork {
  private val TAG = classOf[ObtainSmiles].getName

  override final def obtainSmiles: Future[Seq[Smile]] = Future {
    import collection.JavaConversions.mapAsJavaMap
    val url = siteUrl + "?s=" + chatCookies.get("session_id") +
      "&app=forums&module=extras&section=legends"

    Log d(TAG, "Obtaining smiles from " + url)
    val doc = Jsoup.connect(url).cookies(chatCookies)
      .timeout(getTimeout).ignoreContentType(true).get()

    val links = doc select "a" toArray new Array[Element](0)
    val imgs = doc select "img" toArray new Array[Element](0)

    Log i(TAG, "Obtained " + links.size + " smiles")

    assert(links.size == imgs.size)
    (0 until links.size) map { case i =>
      Smile(links(i).html, imgs(i) attr "src")
    }

  } (this)
}
