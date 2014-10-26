package com.pirotehnika_ruhelp.chat
package works

import android.content.Context
import android.os.{HandlerThread, Handler}
import android.preference.PreferenceManager
import android.util.Log

private[chat] object NetWork {
  def apply(acontext: Context, agui: GuiWorker): NetworkWorker =
    new { override val context = acontext; override val gui = agui }
    with NetWork with Login with Logout
    with ObtainMembers with ObtainMessages
    with PostMessage
}

private[works] trait NetWork extends NetworkWorker {
  protected val siteUrl = "http://pirotehnika-ruhelp.com/index.php"
  protected val enterUrl = siteUrl + "?app=core&module=global&section=login"
  protected val chatUrl = siteUrl + "/shoutbox/"
  protected var authKey = ""
  protected var secureHash = ""
  protected var chatCookies: java.util.Map[String, String] = null
  protected var lastMsgId = ""
  protected var inAutoLogin = false
  protected var isUserEntered = false
  protected var workerHandler: Handler = null
  protected lazy val prefs = PreferenceManager getDefaultSharedPreferences context

  private val workerThread = new HandlerThread("Chat Worker") {
    override protected def onLooperPrepared() =
      workerHandler = new Handler(getLooper)
  }

  workerThread start()

  override final def ready = workerHandler ne null
  override final def login() = workerHandler post performLogin
  override final def logout() = workerHandler post performLogout
  override final def userEntered = isUserEntered

  override final def tryAutoLogin() = {
    restoreCookies()
    workerHandler post checkMessages
    inAutoLogin = true
  }

  override def postMessage(text: String): Unit

  protected val performLogin: Runnable
  protected val performLogout: Runnable
  protected val checkMessages: Runnable
  protected val checkMembers: Runnable

  protected def enterUser(): Unit = {
    saveCookies()
    inAutoLogin = false; isUserEntered = true
    val enteredMessage = Message("entered",
      getString(R.string.key_system_user), "",
      getString(R.string.chat_user_login))
    gui sendMessage Messages(Seq(enteredMessage))
  }

  protected def exitUser(errorId: Int = -1, errorMsg: String = ""): Unit = {
    authKey = ""; secureHash = ""; chatCookies.clear()
    saveCookies()
    inAutoLogin = false; isUserEntered = false
    val text = if(-1 == errorId) getString(R.string.chat_user_logout)
      else getString(errorId) + (if(errorMsg.isEmpty) "" else "\n" + errorMsg)
    val notEnteredMessage = Message("not entered",
      getString(R.string.key_system_user), "", text)
    gui sendMessage Messages(Seq(notEnteredMessage))
  }

  protected final def saveCookies(): Unit = {
    import java.util.Map.Entry
    import collection.JavaConverters.mutableSetAsJavaSetConverter
    val ed = prefs.edit()
    val strSet = collection.mutable.Set[String]()
    chatCookies.entrySet().toArray(new Array[Entry[String, String]](chatCookies.size)).
      foreach { entry => strSet += entry.getKey + "%" + entry.getValue }
    ed.putString("secureHash", secureHash)
    ed.putStringSet("cookies", strSet.asJava).commit()
  }

  protected final def restoreCookies(): Unit = {
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

  protected final def getString(resId: Int) = context getString resId

  protected final def getMsgInterval = {
    val interval = prefs.getString(getString(R.string.key_network_refresh_interval), "10")
    Integer.parseInt(interval) * 1000
  }

  protected final def getTimeout = {
    val timeout = prefs.getString(getString(R.string.key_network_timeout), "5")
    Integer.parseInt(timeout) * 1000
  }

  protected final def getUserAgent = {
    val info = context.getPackageManager.getPackageInfo(context.getPackageName, 0)
    val userAgent = getString(R.string.key_useragent)
    val verCode = info.versionCode
    val verName = info.versionName
    val sdkCode = android.os.Build.VERSION.SDK_INT
    val sdkName = android.os.Build.VERSION.RELEASE
    val man = android.os.Build.MANUFACTURER
    val model = android.os.Build.MODEL
    val abi = android.os.Build.CPU_ABI
    val serial = android.os.Build.SERIAL
    val bootLoader = android.os.Build.BOOTLOADER
    val radio = android.os.Build.getRadioVersion
    val fp = android.os.Build.FINGERPRINT
    s"$userAgent v$verName (Linux; U; Android $sdkName/$sdkCode; " +
      s"ru-Ru; $model; $abi; $bootLoader; $radio) Mobile/$man Version/$verCode $serial/$fp"
  }

  protected final def extractMessages(doc: org.jsoup.nodes.Document) = {
    import org.jsoup.nodes.Element
    val spans = doc getElementsByTag "span" toArray new Array[Element](0)
    val script = doc getElementsByAttributeValue("type", "text/javascript") get 0

    // Все числа в скрипте - номера сообщений
    val ids = "\\d+".r.findAllIn(script.html).toIndexedSeq
    val names = spans filter (!_.getElementsByAttributeValue("itemprop", "name").isEmpty)
    val timestamps = spans filter (!_.getElementsByAttributeValue("class", "right desc").isEmpty)
    val messages = spans filter (!_.getElementsByAttributeValue("class", "shoutbox_text").isEmpty)

    assert((ids.size == names.size) && (names.size == timestamps.size) && (timestamps.size == messages.size))
    (0 until names.size) map { case i =>
      Message(ids(i), names(i).html, timestamps(i).html, messages(i).html)
    }
  }

  protected def handleNetworkError(tag: String,
    workName: String, e: java.io.IOException) = e match {
    case e: org.jsoup.HttpStatusException =>
      Log e(tag, workName + " fails: can not connect to " + e.getUrl)
      Log e(tag, "Status code [" + e.getStatusCode + "]")
      exitUser(R.string.chat_error_network_bad_request,
        errorMsg = e.getMessage)

    case e: java.io.IOException =>
      Log e(tag, workName + " fails, cause: \""
        + e.getMessage + "\" by: " + e.getCause)
      e printStackTrace()
      exitUser(R.string.chat_error_network,
        errorMsg = e.getMessage)
    }
}
