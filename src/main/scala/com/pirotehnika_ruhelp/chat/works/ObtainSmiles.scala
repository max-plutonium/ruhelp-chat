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

  } (Chat.network)
}
