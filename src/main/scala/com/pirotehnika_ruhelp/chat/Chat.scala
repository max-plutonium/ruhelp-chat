package com.pirotehnika_ruhelp.chat

import android.app.{ProgressDialog, Activity}
import android.content.Context
import android.net.{Uri, ConnectivityManager}
import android.os.{HandlerThread, Looper, Handler}
import android.preference.PreferenceManager
import android.util.Log
import android.widget.{Toast, ListView}
import org.jsoup.nodes.Element
import org.jsoup.{Connection, Jsoup}

protected[chat] class Chat(private val activity: Activity) {

  private var userEntered = false

  private lazy val prefs = PreferenceManager getDefaultSharedPreferences activity
  private val guiHandler: GuiHandler = new GuiHandler(activity)
  private var workerHandler: NetworkHandler = null
  private val workerThread: HandlerThread =
    new HandlerThread("Chat Worker") {
      override protected def onLooperPrepared() =
        workerHandler = new NetworkHandler(getLooper)
    }

  workerThread start()

  import Chat._

  final def start() = guiHandler sendMessage StartChat
  final def login() = guiHandler sendMessage Login
  final def logout() = guiHandler sendMessage Logout
  final def isUserEntered = if(workerHandler eq null) false
    else workerHandler.checkForNewMessages ne null
  final def isLoginOrLogout = guiHandler.loginOrLogout

  private final def getMsgInterval = {
    val interval = prefs.getString(getString(R.string.key_network_refresh_interval), "10")
    Integer.parseInt(interval) * 1000
  }

  private final def getTimeout = {
    val timeout = prefs.getString(getString(R.string.key_network_timeout), "5")
    Integer.parseInt(timeout) * 1000
  }

  private[chat] final def getUserAgent = {
    val info = activity.getPackageManager.getPackageInfo(activity.getPackageName, 0)
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

  private final def getString(resId: Int) = activity getString resId

  final def isNetworkAvailable: Boolean = {
    val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    if(cm eq null) false
    val netInfo = cm.getAllNetworkInfo
    if(netInfo eq null) false
    netInfo exists (ni => if (ni.isConnected) {
      Log d(TAG, ni.getTypeName + " connection found")
      true } else false)
  }

  private sealed trait GuiMessage
  private case object StartChat extends GuiMessage
  private case object Login extends GuiMessage
  private case class UpdateProgress(message: String) extends GuiMessage
  private case class LoginResult(success: Boolean,
    errorStringId: Int = -1, errorMsg: String = "") extends GuiMessage
  private case object Logout extends GuiMessage
  private case class LogoutResult(result: Boolean) extends GuiMessage
  private case class Messages(seq: Seq[Message]) extends GuiMessage

  private class GuiHandler(private val context: Context) extends Handler {
    private val arrayList = collection.mutable.ArrayBuffer[Message]()
    private lazy val listAdapter = MessageAdapter(context, arrayList)
    private lazy val lstChat = activity.findViewById(R.id.lstChat).asInstanceOf[ListView]
    private var progressDialog: ProgressDialog = null
    private[Chat] var loginOrLogout = false

    def sendMessage(msg: GuiMessage) = super.sendMessage(obtainMessage(1, msg))

    protected def startProgress(titleId: Int, steps: Int) = {
      progressDialog = new ProgressDialog(context)
      progressDialog setTitle titleId
      progressDialog setMessage ""
      progressDialog setProgressStyle ProgressDialog.STYLE_HORIZONTAL
      progressDialog setProgress 0
      progressDialog setMax steps
      progressDialog show()
    }

    protected def updateProgress(message: String) = {
      progressDialog setProgress progressDialog.getProgress + 1
      progressDialog setMessage message
    }

    protected def stopProgress() = {
      progressDialog dismiss()
      progressDialog = null
    }

    override def handleMessage(message: android.os.Message): Unit = {
      super.handleMessage(message)
      message.obj match {
        case StartChat =>
          lstChat setAdapter listAdapter

        case Login =>
          startProgress(R.string.chat_login_progress_title, 5)
          workerHandler post workerHandler.performLogin
          loginOrLogout = true
        case UpdateProgress(msg) =>
          updateProgress(msg)
        case result: LoginResult =>
          stopProgress()
          onLoginResult(result)
          loginOrLogout = false

        case Logout =>
          startProgress(R.string.chat_logout_progress_title, 2)
          workerHandler post workerHandler.performLogout
          loginOrLogout = true
        case LogoutResult(result) =>
          stopProgress()
          Toast makeText(context, if(result) R.string.chat_user_logout
            else R.string.chat_error_network, Toast.LENGTH_LONG) show()
          loginOrLogout = false

        case Messages(seq) =>
          arrayList ++= seq
          listAdapter notifyDataSetChanged()
          lstChat smoothScrollToPosition listAdapter.getCount
      }
    }

    private def onLoginResult(result: LoginResult) = {
      result match {
        case LoginResult(false, errorStringId, "") if -1 != errorStringId =>
          Toast makeText(context, errorStringId, Toast.LENGTH_LONG) show()
        case LoginResult(false, errorStringId, msg) if -1 != errorStringId =>
          Toast makeText(context, getString(errorStringId)
            + "\n" + msg, Toast.LENGTH_LONG) show()
        case LoginResult(true, _, _) =>
          Toast makeText(context, R.string.chat_user_login,
            Toast.LENGTH_LONG) show()
      }
    }
  }

  private class NetworkHandler(looper: Looper) extends Handler(looper) {
    private var authKey = ""
    private var secureHash = ""
    private var chatCookies: java.util.Map[String, String] = null
    private var lastMsgId = ""

    private final def enterUser() = {
      assert(checkForNewMessages eq null)
      checkForNewMessages = new CheckForNewMessages
      workerHandler post checkForNewMessages
    }

    private final def exitUser() = {
      removeCallbacks(checkForNewMessages)
      authKey = ""; secureHash = ""; chatCookies = null
      checkForNewMessages = null
    }

    private final def publishProgress(msg: String) =
      guiHandler sendMessage new UpdateProgress(msg)

    val performLogin = new Runnable {
      override def run(): Unit =
        guiHandler sendMessage doLogin(enterUrl, getTimeout)

      private def doLogin(url: String, timeout: Int): GuiMessage = {
        val name = prefs getString(getString(R.string.key_user_name), "")
        val pass = prefs getString(getString(R.string.key_user_pass), "")
        val remember = prefs getBoolean(getString(R.string.key_user_remember), false)
        val anon = prefs getBoolean(getString(R.string.key_user_anon), false)
        var resp: Connection.Response = null

        try {
          publishProgress("Connect to forum...")
          Log i(TAG, "Request for login info from " + url)
          resp = Jsoup.connect(url).timeout(timeout)
            .userAgent(getUserAgent).method(Connection.Method.GET).execute()

          publishProgress("Connected. Parse login form...")
          Log i(TAG, "Connected to " + url)
          Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

          val form = resp parse() getElementsByTag "form" forms() get 0
          assert(authKey isEmpty)
          authKey = form getElementsByAttributeValue(
            "name", getString(R.string.key_form_auth)) `val`()
          val action = if (form.hasAttr("action"))
            form.absUrl("action") else form.baseUri()
          val method = if (form.attr("method").toUpperCase.equals("POST"))
            Connection.Method.POST else Connection.Method.GET

          publishProgress("Send login info to forum...")
          Log i(TAG, "Send login info to " + action)

          resp = Jsoup.connect(action)
            .data(getString(R.string.key_form_auth), authKey)
            .data(getString(R.string.key_form_referrer), chatUrl)
            .data(getString(R.string.key_form_name), name)
            .data(getString(R.string.key_form_pass), pass)
            .data(getString(R.string.key_form_remember), if (remember) "1" else "0")
            .data(getString(R.string.key_form_anon), if (anon) "1" else "0")
            .method(method).timeout(timeout).execute()

          Log i(TAG, "Connected to " + action)
          Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)
          assert(chatCookies eq null)
          chatCookies = resp.cookies

          publishProgress("Parse result...")
          Log i(TAG, "Parse result")
          val doc = resp parse()

          // Ищем на странице элемент <p class="message error" ...
          // Если находим, значит форум прислал нам описание ошибки
          doc.getElementsByTag("p").toArray(new Array[Element](1)).
            filter { _.attr("class").equals(getString(R.string.key_form_login_error_class))
            } foreach { res => val msg = res.text
              Log w(TAG, "Login failure, caused: " + msg)
              authKey = ""; chatCookies = null
              return LoginResult(success = false,
                errorStringId = R.string.chat_error_login, errorMsg = msg)
            }

          // Поиск ссылки для выхода
          val link = doc getElementsByAttributeValueStarting("href",
            url + "&do=logout") get 0 attr "href"

          // Из нее берем параметр k - он нужен для составления запросов
          assert(secureHash isEmpty)
          secureHash = Uri parse link getQueryParameter "k"

          enterUser()
          publishProgress("Login success")
          Log i(TAG, "Login successful")
          LoginResult(success = true)

        } catch {
          case e: java.net.SocketTimeoutException =>
            Log w(TAG, "Timeout on login")
            authKey = ""
            LoginResult(success = false,
              errorStringId = R.string.chat_error_network_timeout)

          case e: java.io.IOException =>
            Log e(TAG, "Login failure, caused: \""
              + e.getMessage + "\" by: " + e.getCause.getMessage)
            e printStackTrace()
            authKey = ""
            LoginResult(success = false,
              errorStringId = R.string.chat_error_network,
              errorMsg = e.getMessage)

          case e: IndexOutOfBoundsException =>
            Log e(TAG, "Login failure, caused: " + e.getMessage)
            e printStackTrace()
            authKey = ""; chatCookies = null
            LoginResult(success = false,
              errorStringId = R.string.chat_error_user)
        }
      }
    }

    val performLogout = new Runnable {
      override def run(): Unit =
        guiHandler sendMessage doLogout(enterUrl, getTimeout)

      private def doLogout(baseUrl: String, timeout: Int): GuiMessage = {
        val url = baseUrl + "&do=logout&k=" + secureHash

        try {
          publishProgress("Logout from forum...")
          Log i(TAG, "Logout from " + url)

          val resp = Jsoup.connect(url).cookies(chatCookies).userAgent(getUserAgent)
            .method(Connection.Method.GET).timeout(timeout).execute()

          Log i(TAG, "Connected to " + url)
          Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

          exitUser()
          publishProgress("Logout success")
          Log i(TAG, "Logout successful")
          LogoutResult(result = true)

        } catch { case e: java.io.IOException =>
          Log e(TAG, "Logout failure, caused: " + e.getMessage)
          e printStackTrace()
          LogoutResult(result = false)
        }
      }
    }

    var checkForNewMessages: Runnable = null

    private final class CheckForNewMessages extends Runnable {
      override def run(): Unit = {
        var interval = getMsgInterval
        try {
          doRequest(getTimeout) foreach { messages =>
            guiHandler sendMessage messages }

        } catch {
          case e: java.net.SocketTimeoutException =>
            Log w(TAG, "Timeout on request new messages")
            interval = 1000

          case e: java.io.IOException =>
            Log e(TAG, "Check for new messages failure, caused: \""
              + e.getMessage + "\" by: " + e.getCause.getMessage)
            e printStackTrace()
            exitUser()

        } finally if(this eq checkForNewMessages) {
          workerHandler postDelayed(this, interval)
        }
      }

      private def doRequest(timeout: Int): Option[Messages] = {
        val url = siteUrl + "?s=" + chatCookies.get("session_id") +
          "&app=shoutbox&module=ajax&section=coreAjax" +
          "&secure_key=" + secureHash + "&type=getShouts" +
          (if (lastMsgId.isEmpty) "" else "&lastid=" + lastMsgId)

        Log i(TAG, "Request for new messages from " + url)
        val resp = Jsoup.connect(url).cookies(chatCookies)
          .method(Connection.Method.GET).timeout(timeout).execute()

        Log i(TAG, "Connected to " + url)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        val doc = resp parse(); val body = doc.body.html
        if (body equals getString(R.string.key_body_no_permission)) {
          Log e(TAG, "Obtained no-permission error")
          exitUser()
          val noPermMessage = Message(getString(R.string.key_body_no_permission),
            getString(R.string.key_system_user), "",
            getString(R.string.chat_error_no_permission))
          return Some(Messages(Seq(noPermMessage)))
        } else if (body.isEmpty) {
          Log i(TAG, "There are no new messages on server")
          return None
        }

        val spans = doc getElementsByTag "span" toArray new Array[Element](0)
        val script = doc getElementsByAttributeValue("type", "text/javascript") get 0

        // Все числа в скрипте - номера сообщений
        val ids = "([0-9]+)".r.findAllIn(script.html).toIndexedSeq
        val names = spans filter (!_.getElementsByAttributeValue("itemprop", "name").isEmpty)
        val timestamps = spans filter (!_.getElementsByAttributeValue("class", "right desc").isEmpty)
        val messages = spans filter (!_.getElementsByAttributeValue("class", "shoutbox_text").isEmpty)

        assert((ids.size == names.size) && (names.size == timestamps.size) && (timestamps.size == messages.size))
        val shouts = for (i <- 0 until names.size)
          yield Message(ids(i), names(i).html, timestamps(i).html, messages(i).html)

        lastMsgId = ids.last
        Log i(TAG, "Obtained " + shouts.size + " new messages")
        Some(Messages(shouts))
      }
    }
  }
}

protected[chat] object Chat {
  private[Chat] val TAG = classOf[Chat].getCanonicalName
  private[chat] val siteUrl = "http://pirotehnika-ruhelp.com/index.php"
  private[chat] val enterUrl = siteUrl + "?app=core&module=global&section=login"
  private[chat] val chatUrl = siteUrl + "/shoutbox/"
}