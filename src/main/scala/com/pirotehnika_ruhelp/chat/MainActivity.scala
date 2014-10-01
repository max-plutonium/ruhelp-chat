package com.pirotehnika_ruhelp.chat

import android.app.{ProgressDialog, AlertDialog, Activity}
import android.content.{DialogInterface, Context, Intent}
import android.net.{Uri, ConnectivityManager}
import android.os._
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.{Menu, MenuItem}
import android.widget._
import org.jsoup.nodes.Element
import org.jsoup.{Connection, Jsoup}

class MainActivity extends Activity {
  private var signingWorker: SigningWorker = null
  private var workerStarted = false
  private val guiHandler: Handler = new GuiHandler
  private var workerHandler: Handler = null
  private val workerThread: HandlerThread =
    new HandlerThread("Chat Worker") {
      override protected def onLooperPrepared() =
        workerHandler = new ChatHandler(getLooper)

      override def start() = super.start()

      override def quit = {
        val ret = super.quit()
        try { join() }
        catch { case e: InterruptedException => }
        ret
      }
    }

  import MainActivity._
  private lazy val prefs = PreferenceManager getDefaultSharedPreferences this
  private lazy val lstChat = findViewById(R.id.lstChat).asInstanceOf[ListView]
  private val arrayList = collection.mutable.ArrayBuffer[Message]()
  private lazy val listAdapter = MessageAdapter(this, arrayList)

  override protected def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    self = this
    lstChat setAdapter listAdapter
  }

  override protected def onResume() = {
    super.onResume()
    if(!workerStarted) {
      workerThread start()
      workerStarted = true
    }
  }

  override protected def onPause() = {
    super.onPause()
  }

  override def onCreateOptionsMenu(menu: Menu) = {
    getMenuInflater inflate(R.menu.main, menu)
    val mi = menu findItem R.id.menu_prefs
    assert(mi ne null)
    mi setIntent new Intent(this, classOf[PreferenceActivity])
    super.onCreateOptionsMenu(menu)
  }

  private def getLoginListener: MenuItem.OnMenuItemClickListener = {
    if(isNetworkAvailable)
      if(userEntered) new MenuItem.OnMenuItemClickListener {
          override def onMenuItemClick(item: MenuItem): Boolean = {
            assert(signingWorker eq null)
            signingWorker = new LogoutWorker(secureHash, chatCookies)
            signingWorker execute()
            true
          }
        }
      else new MenuItem.OnMenuItemClickListener {
          override def onMenuItemClick(item: MenuItem): Boolean = {
            assert(signingWorker eq null)
            signingWorker = new LoginWorker(enterUrl, chatUrl)
            signingWorker execute()
            true
          }
        }

    // Нет сети
    else new MenuItem.OnMenuItemClickListener {
        override def onMenuItemClick(item: MenuItem): Boolean = {
          val builder = new AlertDialog.Builder(MainActivity.this)
          builder setTitle R.string.chat_login_alert_title
          builder setMessage R.string.chat_login_alert_msg

          builder.setPositiveButton(R.string.chat_login_alert_yes,
            new DialogInterface.OnClickListener {
              override def onClick(dialog: DialogInterface, which: Int) =
                startActivity(new Intent(Settings.ACTION_SETTINGS))
            })

          builder setNegativeButton(R.string.chat_login_alert_no,
            new DialogInterface.OnClickListener {
              override def onClick(dialog: DialogInterface, which: Int) =
                Toast makeText(MainActivity.this,
                  R.string.chat_login_alert_on_cancel, Toast.LENGTH_LONG) show()
            })

          builder create() show()
          true
        }
      }
  }

  override def onPrepareOptionsMenu(menu: Menu): Boolean = {
    val mi = menu findItem R.id.menu_signing
    assert(mi ne null)
    mi setTitle(if(userEntered) R.string.chat_menu_sign_off
      else R.string.chat_menu_sign_on)
    mi setOnMenuItemClickListener getLoginListener
    mi setEnabled null == signingWorker
    super.onCreateOptionsMenu(menu)
  }

  case class Messages(seq: Seq[Message])
  private val NoPermMessage = new Messages(Seq(Message("", "", "", "")))

  private class GuiHandler extends Handler {
    override def handleMessage(message: android.os.Message) = {
      super.handleMessage(message)
      message.obj match {
        case null =>
        case NoPermMessage =>
          Toast makeText(MainActivity.this, R.string.chat_error_no_permission,
            Toast.LENGTH_LONG) show()
          userEntered = false
        case Messages(seq) =>
          arrayList ++= seq
          listAdapter notifyDataSetChanged()
          lastMsgId = arrayList.last.id
      }
      if(userEntered) workerHandler sendMessageDelayed(
        workerHandler.obtainMessage(1, ChatHandler.GetShouts(lastMsgId)), 10000)
    }
  }

  private object ChatHandler {
    case class GetShouts(lastId: String)
    case object UserExit
  }

  private class ChatHandler(looper: Looper) extends Handler(looper) {
    import ChatHandler._

    override def handleMessage(message: android.os.Message) = {
      super.handleMessage(message)
      message.obj match {
        case GetShouts(id) =>
          try {
            requestNewShouts(id) match { case Some(msg) =>
              guiHandler sendMessage guiHandler.obtainMessage(1, msg)
            case None =>
              guiHandler sendMessage guiHandler.obtainMessage(1, null)
            }

          } catch { case e: java.net.SocketTimeoutException =>
              Log w(TAG, "Timeout on request new messages")
              sendMessageDelayed(obtainMessage(1, GetShouts(id)), 3300)
          }
        case UserExit => removeMessages(1, GetShouts)
      }
    }

    private def requestNewShouts(lastId: String): Option[Messages] = {
      val url = siteUrl + "?s=" + chatCookies.get("session_id") +
        "&app=shoutbox&module=ajax&section=coreAjax&secure_key=" +
        secureHash + "&type=getShouts" + (if(lastId.isEmpty) "" else "&lastid=" + lastId)
      Log i(TAG, "Request for new messages on " + url)
      val resp = Jsoup.connect(url).cookies(chatCookies)
        .method(Connection.Method.GET).execute()

      Log i(TAG, "Connected to " + url)
      Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

      val doc = resp parse(); val body = doc.body.html
      if(body equals getString(R.string.key_body_no_permission)) {
        Log e(TAG, "Obtained no-permission error")
        return Some(NoPermMessage)
      } else if(body.isEmpty) {
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
      Some(Messages(shouts))
    }
  }

  // See http://piotrbuda.eu/2012/12/scala-and-android-asynctask-implementation-problem.html
  private abstract class SigningWorker(private val steps: Int, private val titleId: Int)
    extends AsyncTask[AnyRef, AnyRef, AnyRef] {

    private val progressDialog = new ProgressDialog(MainActivity.this)

    override protected def onPreExecute(): Unit = {
      super.onPreExecute()
      assert(signingWorker ne null)
      progressDialog setTitle titleId
      progressDialog setMessage ""
      progressDialog setProgressStyle ProgressDialog.STYLE_HORIZONTAL
      progressDialog setMax steps
      progressDialog show()
    }

    override protected def onPostExecute(result: AnyRef): Unit = {
      super.onPostExecute(result)
      progressDialog dismiss()
      signingWorker = null
    }

    override protected final def onProgressUpdate(values: AnyRef*): Unit = {
      val message = values.head.asInstanceOf[String]
      assert(message ne null)
      progressDialog setProgress progressDialog.getProgress + 1
      progressDialog setMessage message
      super.onProgressUpdate(values: _*)
    }

    override protected def onCancelled(result: AnyRef): Unit = super.onCancelled(result)
    override protected def onCancelled(): Unit = super.onCancelled()
    override protected def doInBackground(params: AnyRef*): AnyRef
  }

  private class LoginWorker(val loginUrl: String, val chatUrl: String)
    extends SigningWorker(5, R.string.chat_login_progress_title) {

    private val userName = prefs getString(getString(R.string.key_user_name), "")
    private val userPass = prefs getString(getString(R.string.key_user_pass), "")
    private val userRemember = prefs getBoolean(getString(R.string.key_user_remember), false)
    private val userAnon = prefs getBoolean(getString(R.string.key_user_anon), false)

    override protected def doInBackground(params: AnyRef*): LoginResult = {
      var resp: Connection.Response = null
      var authKey = ""

      try {
        publishProgress("Connect to forum...")
        Log i(TAG, "Request login info from " + loginUrl)
        resp = Jsoup.connect(loginUrl)
          .method(Connection.Method.GET).execute()

        publishProgress("Connected. Parse login form...")
        Log i(TAG, "Connected to " + loginUrl)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        val form = resp parse() getElementsByTag "form" forms() get 0
        authKey = form getElementsByAttributeValue("name",
          getString(R.string.key_form_auth)) `val`()
        val action = if (form.hasAttr("action"))
          form.absUrl("action") else form.baseUri()
        val method = if (form.attr("method").toUpperCase.equals("POST"))
          Connection.Method.POST else Connection.Method.GET

        publishProgress("Send login info to forum...")
        Log i(TAG, "Send login info to " + action)

        resp = Jsoup.connect(action)
          .data(getString(R.string.key_form_auth), authKey)
          .data(getString(R.string.key_form_referrer), chatUrl)
          .data(getString(R.string.key_form_name), userName)
          .data(getString(R.string.key_form_pass), userPass)
          .data(getString(R.string.key_form_remember), if (userRemember) "1" else "0")
          .data(getString(R.string.key_form_anon), if (userAnon) "1" else "0")
          .method(method).execute()

        Log i(TAG, "Connected to " + action)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

      } catch {
        case e: java.io.IOException =>
          Log e(TAG, "Login failure, caused: " + e.getMessage)
          e.printStackTrace()
          return LoginResult(null, null, null)
        case e: ExceptionInInitializerError =>
          Log.wtf(TAG, "Jsoup initialisation error", e)
          e.printStackTrace()
          return LoginResult(null, "", null)
      }

      try {
        publishProgress("Parse result...")
        Log i(TAG, "Parse result")
        val doc = resp parse()
        val pTags = doc getElementsByTag "p" toArray new Array[Element](0)
        val res = pTags find (!_.getElementsByAttributeValue("class",
              getString(R.string.key_form_login_error_class)).isEmpty)

        if(res.isDefined) {
          val msg = res.get.text
          Log e(TAG, "Login failure, caused: " + msg)
          return LoginResult("", msg, null)
        }

        // Поиск ссылки для выхода
        val link = doc getElementsByAttributeValueStarting("href",
          enterUrl + "&do=logout") get 0 attr "href"

        // Параметр k нужен для составления запросов
        val secureHash = Uri parse link getQueryParameter "k"

        publishProgress("Login success")
        Log i(TAG, "Login successful")
        LoginResult(authKey, secureHash, resp.cookies)

      } catch { case e: IndexOutOfBoundsException =>
        Log e(TAG, "Login failure, caused: " + e.getMessage)
        e.printStackTrace()
        LoginResult("", null, null)
      }
    }

    override protected def onPostExecute(result: AnyRef): Unit = {
      super.onPostExecute(result)
      userEntered = result.asInstanceOf[LoginResult] match {
        case LoginResult(null, null, null) =>
          Toast makeText(MainActivity.this, R.string.chat_error_network,
            Toast.LENGTH_LONG) show()
          false
        case LoginResult(a, null, null) if a.isEmpty =>
          Toast makeText(MainActivity.this, R.string.chat_error_user,
            Toast.LENGTH_LONG) show()
          false
        case LoginResult(null, b, null) if b.isEmpty =>
          Toast makeText(MainActivity.this, "Parser fatal error",
            Toast.LENGTH_LONG) show()
          false
        case LoginResult(a, errorString, null) if a.isEmpty =>
          Toast makeText(MainActivity.this, getString(R.string.chat_error_login)
            + "\n" + errorString, Toast.LENGTH_LONG) show()
          false
        case LoginResult(a, sh, c) =>
          authKey = a; secureHash = sh; chatCookies = c
          Toast makeText(MainActivity.this, R.string.chat_user_login,
            Toast.LENGTH_LONG) show()
          if(workerHandler ne null) workerHandler sendMessage
            workerHandler.obtainMessage(1, ChatHandler.GetShouts(lastMsgId))
          true
      }
    }
  }

  private class LogoutWorker(val secHash: String, val cookies: java.util.Map[String, String])
    extends SigningWorker(2, R.string.chat_logout_progress_title) {

    override protected def onPreExecute(): Unit = {
      super.onPreExecute()
      workerHandler sendMessage
        workerHandler.obtainMessage(1, ChatHandler.UserExit)
    }

    override protected def doInBackground(params: AnyRef*): AnyRef = {
      val logoutUrl = enterUrl + "&do=logout&k=" + secHash

      try {
        publishProgress("Logout from forum...")
        Log i(TAG, "Logout from " + logoutUrl)

        val resp = Jsoup.connect(logoutUrl).cookies(cookies)
          .method(Connection.Method.GET).execute()

        Log i(TAG, "Connected to " + logoutUrl)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        publishProgress("Logout success")
        Log i(TAG, "Logout successful")
        AnyRef

      } catch { case e: java.io.IOException =>
        Log e(TAG, "Logout failure, caused: " + e.getMessage)
        e.printStackTrace()
        null
      }
    }

    override protected def onPostExecute(result: AnyRef): Unit = {
      super.onPostExecute(result)
      userEntered = result match {
        case null =>
          Toast makeText(MainActivity.this, R.string.chat_error_network,
            Toast.LENGTH_LONG) show()
          true
        case AnyRef =>
          authKey = ""; secureHash = ""; chatCookies = null
          Toast makeText(MainActivity.this, R.string.chat_user_logout,
            Toast.LENGTH_LONG) show()
          guiHandler removeMessages(1, ChatHandler.GetShouts)
          false
      }
    }
  }
}

object MainActivity {
  private[MainActivity] val TAG = classOf[MainActivity].getCanonicalName
  private[MainActivity] var self: MainActivity = null

  private[chat] val siteUrl = "http://pirotehnika-ruhelp.com/index.php"
  private[chat] val enterUrl = siteUrl + "?app=core&module=global&section=login"
  private[chat] val chatUrl = siteUrl + "/shoutbox/"

  private[MainActivity] case class LoginResult(authKey: String,
    secureHash: String, cookies: java.util.Map[String, String])
  private[MainActivity] var authKey = ""
  private[MainActivity] var secureHash = ""
  private[MainActivity] var chatCookies: java.util.Map[String, String] = null
  private[MainActivity] var userEntered = false
  var lastMsgId = ""

  private[chat] def isNetworkAvailable: Boolean = {
    val cm = self.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    if(cm eq null) false
    val netInfo = cm.getAllNetworkInfo
    if(netInfo eq null) false
    netInfo exists (ni => if (ni.isConnected) {
      Log d(TAG, ni.getTypeName + " connection found")
      true } else false)
  }
}
