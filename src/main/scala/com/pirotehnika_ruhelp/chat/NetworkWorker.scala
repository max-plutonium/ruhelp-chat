package com.pirotehnika_ruhelp.chat

import android.os.{Handler, HandlerThread}

protected[chat] trait NetworkWorker { this: Chat =>
  protected val siteUrl = "http://pirotehnika-ruhelp.com/index.php"
  protected val enterUrl = siteUrl + "?app=core&module=global&section=login"
  protected val chatUrl = siteUrl + "/shoutbox/"
  protected var userEntered = false
  protected var authKey = ""
  protected var secureHash = ""
  protected var chatCookies: java.util.Map[String, String] = null
  protected var lastMsgId = ""

  private[chat] var workerHandler: Handler = null
  private val workerThread = new HandlerThread("Chat Worker") {
      override protected def onLooperPrepared() =
        workerHandler = new Handler(getLooper)
    }

  workerThread start()

  protected[this] def enterUser(): Unit = {
    saveCookies()
    userEntered = true
  }

  protected[this] def exitUser(): Unit = {
    authKey = ""; secureHash = ""; chatCookies.clear()
    saveCookies()
    userEntered = false
  }

  protected val checkForEnter: Runnable
  protected val performLogin: Runnable
  protected val performLogout: Runnable
  protected val checkForNewMessages: Runnable
  protected def postMessage(text: String): Unit

  protected[this] final def getMsgInterval = {
    val interval = prefs.getString(getString(R.string.key_network_refresh_interval), "10")
    Integer.parseInt(interval) * 1000
  }

  protected[this] final def getTimeout = {
    val timeout = prefs.getString(getString(R.string.key_network_timeout), "5")
    Integer.parseInt(timeout) * 1000
  }

  protected[this] final def saveCookies(): Unit = {
    import java.util.Map.Entry
    import collection.JavaConverters.mutableSetAsJavaSetConverter
    val ed = prefs.edit()
    val strSet = collection.mutable.Set[String]()
    chatCookies.entrySet().toArray(new Array[Entry[String, String]](chatCookies.size)).
      foreach { entry => strSet += entry.getKey + "%" + entry.getValue }
    ed.putString("secureHash", secureHash)
    ed.putStringSet("cookies", strSet.asJava).commit()
  }

  protected[this] final def restoreCookies(): Unit = {
    import collection.JavaConversions.mutableMapAsJavaMap
    if(chatCookies eq null)
      chatCookies = collection.mutable.Map[String, String]()
    secureHash = prefs getString("secureHash", "")
    val strSet = prefs getStringSet("cookies", null)
    if(strSet ne null)
      strSet.toArray(new Array[String](strSet.size)).
        foreach { str => val lst = str split "%"
        chatCookies put(lst(0), lst(1))
      }
  }

  protected[this] final def extractMessages(doc: org.jsoup.nodes.Document) = {
    import org.jsoup.nodes.Element
    val spans = doc getElementsByTag "span" toArray new Array[Element](0)
    val script = doc getElementsByAttributeValue("type", "text/javascript") get 0

    // Все числа в скрипте - номера сообщений
    val ids = "(\\d+)".r.findAllIn(script.html).toIndexedSeq
    val names = spans filter (!_.getElementsByAttributeValue("itemprop", "name").isEmpty)
    val timestamps = spans filter (!_.getElementsByAttributeValue("class", "right desc").isEmpty)
    val messages = spans filter (!_.getElementsByAttributeValue("class", "shoutbox_text").isEmpty)

    assert((ids.size == names.size) && (names.size == timestamps.size) && (timestamps.size == messages.size))
    (0 until names.size) map { case i =>
      Message(ids(i), names(i).html, timestamps(i).html, messages(i).html)
    }
  }
}
