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

  private lazy val prefs = PreferenceManager getDefaultSharedPreferences activity
  private var userEntered = false
  private var loginOrLogout = false
  private val guiHandler: GuiHandler = new GuiHandler(activity)
  private var workerHandler: NetworkHandler = null
  private val workerThread: HandlerThread =
    new HandlerThread("Chat Worker") {
      override protected def onLooperPrepared() =
        workerHandler = new NetworkHandler(getLooper)
    }

  workerThread start()

  import Chat._

  final def start() = guiHandler sendMessage GuiHandler.StartChat
  final def login() = guiHandler sendMessage GuiHandler.Login
  final def logout() = guiHandler sendMessage GuiHandler.Logout
  final def isUserEntered = userEntered
  final def isLoginOrLogout = loginOrLogout

  final def isNetworkAvailable: Boolean = {
    val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    if(cm eq null) false
    val netInfo = cm.getAllNetworkInfo
    if(netInfo eq null) false
    netInfo exists (ni => if (ni.isConnected) {
      Log d(TAG, ni.getTypeName + " connection found")
      true } else false)
  }

  private object GuiHandler {
    sealed trait GuiMessage
    case object StartChat extends GuiMessage
    case object Login extends GuiMessage
    case class UpdateProgress(message: String) extends GuiMessage
    case class LoginResult(authKey: String, secureHash: String,
      cookies: java.util.Map[String, String]) extends GuiMessage
    case object Logout extends GuiMessage
    case class LogoutResult(result: Boolean) extends GuiMessage
    case class Messages(seq: Seq[Message]) extends GuiMessage
    val NoPermMessage = new Messages(Seq(Message("", "", "", "")))
  }

  private class GuiHandler(private val context: Context) extends Handler {
    import GuiHandler._
    private val arrayList = collection.mutable.ArrayBuffer[Message]()
    private lazy val listAdapter = MessageAdapter(context, arrayList)
    private lazy val lstChat = activity.findViewById(R.id.lstChat).asInstanceOf[ListView]
    private lazy val progressDialog = new ProgressDialog(context)

    def sendMessage(msg: GuiMessage) = super.sendMessage(obtainMessage(1, msg))

    protected def startProgress(titleId: Int, steps: Int) = {
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

    protected def stopProgress() = progressDialog dismiss()

    override def handleMessage(message: android.os.Message) = {
      super.handleMessage(message)
      message.obj match {
        case null =>
        case StartChat => lstChat setAdapter listAdapter
        case Login =>
          startProgress(R.string.chat_login_progress_title, 5)
          val userName = prefs getString(activity getString R.string.key_user_name, "")
          val userPass = prefs getString(activity getString R.string.key_user_pass, "")
          val userRemember = prefs getBoolean(activity getString R.string.key_user_remember, false)
          val userAnon = prefs getBoolean(activity getString R.string.key_user_anon, false)
          workerHandler sendMessage NetworkHandler.Login(userName, userPass, userRemember, userAnon)
          loginOrLogout = true

        case UpdateProgress(msg) => updateProgress(msg)
        case result: LoginResult => stopProgress()
          onLoginResult(result); loginOrLogout = false
        case Logout =>
          workerHandler sendMessage NetworkHandler.Logout
          loginOrLogout = true
        case LogoutResult(result) =>
          onLogoutResult(result); loginOrLogout = false

        case NoPermMessage =>
          Toast makeText(activity, R.string.chat_error_no_permission,
            Toast.LENGTH_LONG) show()
          userEntered = false

        case Messages(seq) =>
          arrayList ++= seq
          listAdapter notifyDataSetChanged()
          lstChat smoothScrollToPosition listAdapter.getCount
      }
      if(userEntered)
        workerHandler sendMessageDelayed(NetworkHandler.GetShouts, 10000)
    }

    private def onLoginResult(result: LoginResult) = {
      userEntered = result.asInstanceOf[LoginResult] match {
        case LoginResult(null, null, null) =>
          Toast makeText(activity, R.string.chat_error_network,
            Toast.LENGTH_LONG) show()
          false
        case LoginResult(a, null, null) if a.isEmpty =>
          Toast makeText(activity, R.string.chat_error_user,
            Toast.LENGTH_LONG) show()
          false
        case LoginResult(null, b, null) if b.isEmpty =>
          Toast makeText(activity, "Parser fatal error",
            Toast.LENGTH_LONG) show()
          false
        case LoginResult(a, errorString, null) if a.isEmpty =>
          Toast makeText(activity, activity.getString(R.string.chat_error_login)
            + "\n" + errorString, Toast.LENGTH_LONG) show()
          false
        case LoginResult(_, _, _) =>
          Toast makeText(activity, R.string.chat_user_login,
            Toast.LENGTH_LONG) show()
          true
      }
    }

    private def onLogoutResult(result: Boolean) = {
      userEntered = result match {
        case false =>
          Toast makeText(activity, R.string.chat_error_network,
            Toast.LENGTH_LONG) show()
          true
        case true =>
          Toast makeText(activity, R.string.chat_user_logout,
            Toast.LENGTH_LONG) show()
          false
      }
    }
  }

  private object NetworkHandler {
    sealed trait NetworkMessage
    case class Login(name: String, pass: String,
      remember: Boolean, anon: Boolean) extends NetworkMessage
    case object Logout extends NetworkMessage
    case object GetShouts extends NetworkMessage
  }

  private class NetworkHandler(looper: Looper) extends Handler(looper) {
    import NetworkHandler._
    private var authKey = ""
    private var secureHash = ""
    private var chatCookies: java.util.Map[String, String] = null
    private var lastMsgId = ""

    def sendMessage(msg: NetworkMessage) = super.sendMessage(obtainMessage(1, msg))

    def sendMessageDelayed(msg: NetworkMessage, delay: Long) =
      super.sendMessageDelayed(obtainMessage(1, msg), delay)

    protected def publishProgress(msg: String) =
      guiHandler sendMessage new GuiHandler.UpdateProgress(msg)

    override def handleMessage(message: android.os.Message) = {
      super.handleMessage(message)
      message.obj match {
        case Login(name, pass, remember, anon) =>
          guiHandler sendMessage performLogin(name, pass, remember, anon)
        case Logout =>
          guiHandler sendMessage performLogout()
          removeMessages(1, GetShouts)

        case GetShouts =>
          try {
            requestNewShouts() match {
              case Some(msg) => guiHandler sendMessage msg
              case None => guiHandler sendMessage null
            }
          } catch { case e: java.net.SocketTimeoutException =>
              Log w(TAG, "Timeout on request new messages")
              sendMessageDelayed(GetShouts, 3300)
          }
      }
    }

    private def performLogin(name: String, pass: String,
      remember: Boolean, anon: Boolean): GuiHandler.GuiMessage = {
      var resp: Connection.Response = null

      try {
        publishProgress("Connect to forum...")
        Log i(TAG, "Request login info from " + enterUrl)
        resp = Jsoup.connect(enterUrl)
          .method(Connection.Method.GET).execute()

        publishProgress("Connected. Parse login form...")
        Log i(TAG, "Connected to " + enterUrl)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        val form = resp parse() getElementsByTag "form" forms() get 0
        authKey = form getElementsByAttributeValue("name",
          activity.getString(R.string.key_form_auth)) `val`()
        val action = if (form.hasAttr("action"))
          form.absUrl("action") else form.baseUri()
        val method = if (form.attr("method").toUpperCase.equals("POST"))
          Connection.Method.POST else Connection.Method.GET

        publishProgress("Send login info to forum...")
        Log i(TAG, "Send login info to " + action)

        resp = Jsoup.connect(action)
          .data(activity.getString(R.string.key_form_auth), authKey)
          .data(activity.getString(R.string.key_form_referrer), chatUrl)
          .data(activity.getString(R.string.key_form_name), name)
          .data(activity.getString(R.string.key_form_pass), pass)
          .data(activity.getString(R.string.key_form_remember), if (remember) "1" else "0")
          .data(activity.getString(R.string.key_form_anon), if (anon) "1" else "0")
          .method(method).execute()

        Log i(TAG, "Connected to " + action)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)
        chatCookies = resp.cookies

      } catch {
        case e: java.io.IOException =>
          Log e(TAG, "Login failure, caused: " + e.getMessage)
          e.printStackTrace()
          return GuiHandler.LoginResult(null, null, null)
        case e: ExceptionInInitializerError =>
          Log.wtf(TAG, "Jsoup initialisation error", e)
          e.printStackTrace()
          return GuiHandler.LoginResult(null, "", null)
      }

      try {
        publishProgress("Parse result...")
        Log i(TAG, "Parse result")
        val doc = resp parse()
        val pTags = doc getElementsByTag "p" toArray new Array[Element](0)
        val res = pTags find (!_.getElementsByAttributeValue("class",
          activity.getString(R.string.key_form_login_error_class)).isEmpty)

        if(res.isDefined) {
          val msg = res.get.text
          Log e(TAG, "Login failure, caused: " + msg)
          return GuiHandler.LoginResult("", msg, null)
        }

        // Поиск ссылки для выхода
        val link = doc getElementsByAttributeValueStarting("href",
          enterUrl + "&do=logout") get 0 attr "href"

        // Параметр k нужен для составления запросов
        secureHash = Uri parse link getQueryParameter "k"

        publishProgress("Login success")
        Log i(TAG, "Login successful")
        GuiHandler.LoginResult(authKey, secureHash, chatCookies)

      } catch { case e: IndexOutOfBoundsException =>
        Log e(TAG, "Login failure, caused: " + e.getMessage)
        e.printStackTrace()
        GuiHandler.LoginResult("", null, null)
      }
    }

    private def performLogout(): GuiHandler.GuiMessage = {
      val logoutUrl = enterUrl + "&do=logout&k=" + secureHash

      try {
        publishProgress("Logout from forum...")
        Log i(TAG, "Logout from " + logoutUrl)

        val resp = Jsoup.connect(logoutUrl).cookies(chatCookies)
          .method(Connection.Method.GET).execute()

        Log i(TAG, "Connected to " + logoutUrl)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        authKey = ""; secureHash = ""; chatCookies = null
        publishProgress("Logout success")
        Log i(TAG, "Logout successful")
        GuiHandler.LogoutResult(result = true)

      } catch { case e: java.io.IOException =>
        Log e(TAG, "Logout failure, caused: " + e.getMessage)
        e printStackTrace()
        GuiHandler.LogoutResult(result = false)
      }
    }

    private def requestNewShouts(): Option[GuiHandler.Messages] = {
      val url = siteUrl + "?s=" + chatCookies.get("session_id") +
        "&app=shoutbox&module=ajax&section=coreAjax&secure_key=" +
        secureHash + "&type=getShouts" + (if (lastMsgId.isEmpty) "" else "&lastid=" + lastMsgId)
      Log i(TAG, "Request for new messages on " + url)
      val resp = Jsoup.connect(url).cookies(chatCookies)
        .method(Connection.Method.GET).execute()

      Log i(TAG, "Connected to " + url)
      Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

      val doc = resp parse(); val body = doc.body.html
      if (body equals activity.getString(R.string.key_body_no_permission)) {
        Log e(TAG, "Obtained no-permission error")
        return Some(GuiHandler.NoPermMessage)
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

      Log i(TAG, "Obtained " + ids.size + " new messages")
      lastMsgId = ids.last
      Some(GuiHandler.Messages(shouts))
    }
  }
}

protected[chat] object Chat {
  private[Chat] val TAG = classOf[Chat].getCanonicalName
  private[chat] val siteUrl = "http://pirotehnika-ruhelp.com/index.php"
  private[chat] val enterUrl = siteUrl + "?app=core&module=global&section=login"
  private[chat] val chatUrl = siteUrl + "/shoutbox/"
}