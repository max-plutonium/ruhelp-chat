package com.pirotehnika_ruhelp.chat

import java.io.IOException
import java.util.Map

import android.app.{ProgressDialog, AlertDialog, Activity}
import android.content.{DialogInterface, Context, Intent}
import android.net.ConnectivityManager
import android.os.{AsyncTask, Bundle}
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.MenuItem.OnMenuItemClickListener
import android.view.{Menu, MenuItem}
import android.widget.{TextView, Toast}
import org.jsoup.nodes.Element
import org.jsoup.{Connection, Jsoup}

class MainActivity extends Activity {
  import com.pirotehnika_ruhelp.chat.MainActivity._
  private lazy val textView = findViewById(R.id.chatText).asInstanceOf[TextView]
  private lazy val prefs = PreferenceManager getDefaultSharedPreferences this
  private var worker: LoginWorker = null

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    self = this
  }

  override def onResume(): Unit = {
    super.onResume()
  }

  override def onCreateOptionsMenu(menu: Menu) = {
    getMenuInflater inflate(R.menu.main, menu)
    val mi = menu findItem R.id.menu_prefs
    assert(mi ne null)
    mi setIntent new Intent(this, classOf[PreferenceActivity])
    super.onCreateOptionsMenu(menu)
  }

  private def getLoginListener: OnMenuItemClickListener = {
    if(isNetworkAvailable)
      if(userEntered) new OnMenuItemClickListener {
          override def onMenuItemClick(item: MenuItem): Boolean = {
            finish() // TODO
            true
          }
        }
      else new OnMenuItemClickListener {
          override def onMenuItemClick(item: MenuItem): Boolean = {
            worker = new LoginWorker(enterUrl, chatUrl)
            worker execute()
            true
          }
        }

    // Нет сети
    else new OnMenuItemClickListener {
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
                  R.string.chat_login_alert_oncancel, Toast.LENGTH_LONG) show()
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
    mi setEnabled(worker == null)
    super.onCreateOptionsMenu(menu)
  }

  // See http://piotrbuda.eu/2012/12/scala-and-android-asynctask-implementation-problem.html
  private class LoginWorker(val loginUrl: String, var chatUrl: String)
    extends AsyncTask[AnyRef, AnyRef, LoginResult] {

    private val progressDialog = new ProgressDialog(MainActivity.this)
    private val userName = prefs getString(getString(R.string.key_user_name), "")
    private val userPass = prefs getString(getString(R.string.key_user_pass), "")
    private val userRemember = prefs getBoolean(getString(R.string.key_user_remember), false)
    private val userAnon = prefs getBoolean(getString(R.string.key_user_anon), false)

    override protected def onPreExecute(): Unit = {
      super.onPreExecute()
      assert(worker ne null)
//      progressDialog setTitle "Progress"
      progressDialog setMessage ""
      progressDialog setProgressStyle ProgressDialog.STYLE_HORIZONTAL
      progressDialog setMax 5
      progressDialog show()
    }

    override protected def onProgressUpdate(values: AnyRef*): Unit = {
      val message = values.head.asInstanceOf[String]
      progressDialog setProgress progressDialog.getProgress + 1
      progressDialog setMessage message
      super.onProgressUpdate(values: _*)
    }

    override protected def onPostExecute(result: LoginResult): Unit = {
      progressDialog dismiss()
      userEntered = result match {
        case LoginResult(null, null, null) =>
          Toast makeText(MainActivity.this, R.string.chat_error_network,
            Toast.LENGTH_LONG) show()
          false
        case LoginResult(a, l, c) =>
          authKey = a; logoutLink = l; cookies = c
          Toast makeText(MainActivity.this, R.string.chat_user_login,
            Toast.LENGTH_LONG) show()
          true
      }
      worker = null
      super.onPostExecute(result)
    }

    override protected def doInBackground(params: AnyRef*): LoginResult = {
      var resp: Connection.Response = null

      try {
        publishProgress("Connect to forum...")
        Log i(TAG, "Request login info from " + loginUrl)
        resp = Jsoup.connect(loginUrl)
          .method(Connection.Method.GET).execute()

        publishProgress("Connected. Parse login form...")
        Log i(TAG, "Connected to " + loginUrl)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        val form = resp parse() getElementsByTag "form" forms() get 0
        val authKey = form getElementsByAttributeValue("name",
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

        publishProgress("Parse result...")
        Log i(TAG, "Connected to " + action)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        // Поиск и сохранение ссылки для выхода
        val link = resp parse() getElementsByAttributeValueStarting("href",
          enterUrl + "&do=logout") get 0 attr "href"

        publishProgress("Login success")
        Log i(TAG, "Login successful")
        LoginResult(authKey, link, resp.cookies)

      } catch { case e: IOException =>
          Log e(TAG, "Login failure, caused: " + e.getMessage)
          e.printStackTrace()
          LoginResult(null, null, null)
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

  private[MainActivity] case class LoginResult(authKey: String, logoutLink: String, cookies: Map[String, String])
  private[MainActivity] var authKey = ""
  private[MainActivity] var logoutLink = ""
  private[MainActivity] var cookies: Map[String, String] = null
  private[MainActivity] var userEntered = false

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
