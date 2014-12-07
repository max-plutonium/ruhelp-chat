package com.pirotehnika_ruhelp.chat
package works

import android.net.Uri
import android.util.Log
import org.jsoup.nodes.Element
import org.jsoup.{Connection, Jsoup}

private[works] trait Login extends NetWork {
  private val TAG = classOf[Login].getName

  override protected final val performLogin = new Runnable {
    private final def publishProgress(msg: String) =
      Chat.handler sendMessage new UpdateProgress(msg)

    override def run() = doLogin(enterUrl, getTimeout)

    private def doLogin(url: String, timeout: Int): Unit = {
      import collection.JavaConversions.mapAsJavaMap
      import collection.JavaConversions.mapAsScalaMap
      val name = prefs getString(getString(R.string.key_user_name), "")
      val pass = prefs getString(getString(R.string.key_user_pass), "")
      val remember = prefs getBoolean(getString(R.string.key_user_remember), false)
      val anon = prefs getBoolean(getString(R.string.key_user_anon), false)
      restoreCookies()

      try {
        publishProgress("Connect to forum...")
        Log i(TAG, "Obtain login form from " + url)

        var resp = Jsoup.connect(url).cookies(chatCookies)
          .userAgent(getUserAgent).method(Connection.Method.GET)
          .timeout(timeout).execute()

        publishProgress("Connected. Parse login form...")
        Log i(TAG, "Connected to " + url)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        val form = resp parse() getElementsByTag "form" forms() get 0
        assert(authKey.isEmpty)
        authKey = form getElementsByAttributeValue(
          "name", getString(R.string.key_form_auth)) `val`()
        val action = if (form.hasAttr("action"))
          form.absUrl("action") else form.baseUri()
        val method = if (form.attr("method").toUpperCase.equals("POST"))
          Connection.Method.POST else Connection.Method.GET

        publishProgress("Send login info to forum...")
        Log i(TAG, "Send login info to " + action)

        // Не используем user-agent, т.к. будет недоступен чат вообще
        resp = Jsoup.connect(action).cookies(chatCookies)
          .data(getString(R.string.key_form_auth), authKey)
          .data(getString(R.string.key_form_referrer), chatUrl)
          .data(getString(R.string.key_form_name), name)
          .data(getString(R.string.key_form_pass), pass)
          .data(getString(R.string.key_form_remember), if (remember) "1" else "0")
          .data(getString(R.string.key_form_anon), if (anon) "1" else "0")
          .method(method).timeout(timeout).execute()

        Log i(TAG, "Connected to " + action)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)
        chatCookies = resp.cookies

        publishProgress("Parse result...")
        Log i(TAG, "Parse result")
        val doc = resp parse()

        // Ищем на странице элемент <p class="message error" ...
        // Если находим, значит форум прислал нам описание ошибки
        doc.getElementsByTag("p").toArray(new Array[Element](1)).
          filter { _.attr("class") equals getString(R.string.key_form_login_error_class)
        } foreach { res => val msg = res.text
          Log w(TAG, "Login failure, caused: " + msg)
          exitUser(R.string.chat_error_login, errorMsg = msg)
          return
        }

        // Поиск ссылки для выхода
        val link = doc getElementsByAttributeValueStarting("href",
          url + "&do=logout") get 0 attr "href"

        // Из нее берем параметр k - он нужен для составления запросов
        assert(secureHash isEmpty)
        secureHash = Uri parse link getQueryParameter "k"

        enterUser()
        Log i(TAG, "Login successful")

      } catch {
        case e: java.net.SocketTimeoutException =>
          Log w(TAG, "Timeout on login")
          exitUser(R.string.chat_error_network_timeout)

        case e: java.io.IOException =>
          handleNetworkError(TAG, "Login", e)

        case e: IndexOutOfBoundsException =>
          Log e(TAG, "Login fails, cause: " + e.getMessage)
          e printStackTrace()
          exitUser(R.string.chat_error_user)
      }
    }
  }
}
