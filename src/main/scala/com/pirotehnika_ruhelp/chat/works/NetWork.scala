package com.pirotehnika_ruhelp.chat
package works

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.preference.PreferenceManager
import android.text.Html
import android.text.Html.ImageGetter
import android.util.Log

import scala.concurrent.Future

private[chat] object NetWork {
  def apply(acontext: Context): NetworkWorker =
    new { override val context = acontext }
    with NetWork with Login with Logout
    with ObtainMembers with ObtainMessages
    with PostMessage with DownloadDrawable with ObtainSmiles
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
  protected lazy val prefs = PreferenceManager getDefaultSharedPreferences context


  override final def login() = Chat.network execute performLogin
  override final def logout() = Chat.network execute performLogout
  override final def userEntered = isUserEntered

  override final def tryAutoLogin() {
    restoreCookies()
    Chat.network execute checkMessages
    inAutoLogin = true
  }

  override def postMessage(text: String): Unit
  override def downloadDrawable(url: String): Future[Drawable]
  override def obtainSmiles: Future[Seq[Smile]]


  protected val performLogin: Runnable
  protected val performLogout: Runnable
  protected val checkMessages: Runnable
  protected val checkMembers: Runnable

  protected def enterUser() {
    saveCookies()
    inAutoLogin = false; isUserEntered = true
    val enteredMessage = Message("entered",
      Html.fromHtml(getString(R.string.key_system_user)), Html.fromHtml(""),
      Html.fromHtml(getString(R.string.chat_user_login)))
    Chat.handler sendMessage Messages(Seq(enteredMessage))
  }

  protected def exitUser(errorId: Int = -1, errorMsg: String = "") {
    authKey = ""; secureHash = ""; chatCookies.clear()
    saveCookies()
    inAutoLogin = false; isUserEntered = false
    val text = if(-1 == errorId) getString(R.string.chat_user_logout)
      else getString(errorId) + (if(errorMsg.isEmpty) "" else "\n" + errorMsg)
    val notEnteredMessage = Message("not entered",
      Html.fromHtml(getString(R.string.key_system_user)),
      Html.fromHtml(""), Html.fromHtml(text))
    Chat.handler sendMessage Messages(Seq(notEnteredMessage))
  }

  protected final def saveCookies() {
    import java.util.Map.Entry
    import collection.JavaConverters.mutableSetAsJavaSetConverter
    val ed = prefs.edit()
    val strSet = collection.mutable.Set[String]()
    chatCookies.entrySet().toArray(new Array[Entry[String, String]](chatCookies.size)).
      foreach { entry => strSet += entry.getKey + "%" + entry.getValue }
    ed.putString("secureHash", secureHash)
    ed.putStringSet("cookies", strSet.asJava).commit()
  }

  protected final def restoreCookies() {
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

  protected final val imageGetter = new ImageGetter {
    override final def getDrawable(source: String): Drawable =
      ChatDrawable(Uri parse source)
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
      Message(ids(i), Html.fromHtml(names(i).html),
        Html.fromHtml(timestamps(i).html),
        Html.fromHtml(messages(i).html, imageGetter, null))
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
