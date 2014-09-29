package com.pirotehnika_ruhelp.chat

import java.net.UnknownHostException

import android.app.Activity
import android.content.{Context, Intent}
import android.net.{ConnectivityManager, Uri}
import android.os.{AsyncTask, Bundle}
import android.preference.PreferenceManager
import android.util.Log
import android.view.{MenuItem, Menu, View}
import android.widget.{Toast, Button, TextView}
import java.util.Map
import org.jsoup.{Jsoup, Connection}

class MainActivity extends Activity with View.OnClickListener {
  private lazy val textView = findViewById(R.id.chatText).asInstanceOf[TextView]
  private lazy val btnEnter = findViewById(R.id.chatEnter).asInstanceOf[Button]
  private lazy val btnExit = findViewById(R.id.chatExit).asInstanceOf[Button]
  private lazy val prefs = PreferenceManager getDefaultSharedPreferences this
  private var worker: Worker = null
  import MainActivity._

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    btnEnter setOnClickListener this
    btnExit setOnClickListener this
    self = this
  }

  override def onResume(): Unit = {
    if(isNetworkAvailable) btnEnter setText R.string.chat_enter
    else btnEnter setText R.string.chat_nonetwork
    super.onResume()
  }

  override def onClick(v: View): Unit = v.getId match {
    case R.id.chatEnter => if(worker eq null) {
      worker = new Worker(enterUrl, chatUrl)
      worker execute()
    }
    case R.id.chatExit => finish() // TODO
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater inflate(R.menu.main, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean =
    item.getItemId match {
      case R.id.menu_prefs => startActivity(new Intent(this, classOf[PreferenceActivity]))
        super.onOptionsItemSelected(item)
    }

  // See http://piotrbuda.eu/2012/12/scala-and-android-asynctask-implementation-problem.html
  private class Worker(val loginUrl: Uri, var chatUrl: Uri)
    extends AsyncTask[AnyRef, Void, LoginResult] {

    val userName = prefs getString(getString(R.string.pref_user_name), "")
    val userPass = prefs getString(getString(R.string.pref_user_pass), "")
    val userRemember = prefs getBoolean(getString(R.string.pref_user_remember), false)
    val userAnon = prefs getBoolean(getString(R.string.pref_user_anon), false)

    override protected def onPreExecute(): Unit = {
      super.onPreExecute()
      assert(worker ne null)
      btnEnter setEnabled false
    }

    override protected def doInBackground(params: AnyRef*): LoginResult = {
      var resp: Connection.Response = null

      try {
        Log i(TAG, "Connecting to " + loginUrl)
        resp = Jsoup.connect(loginUrl toString)
          .method(Connection.Method.GET).execute()
        Log i(TAG, "Connected to " + loginUrl)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        val form = resp parse() getElementsByTag "form" forms() get 0
        val authKey = form getElementsByAttributeValue("name",
          getString(R.string.form_key)) `val`()

        val action = if (form.hasAttr("action"))
          form.absUrl("action") else form.baseUri()

        val method = if (form.attr("method").toUpperCase.equals("POST"))
          Connection.Method.POST else Connection.Method.GET

        Log i(TAG, "Send registration info to " + action)
        resp = Jsoup.connect(action)
          .data(getString(R.string.form_key), authKey)
          .data(getString(R.string.form_referrer), chatUrl toString)
          .data(getString(R.string.form_name), userName)
          .data(getString(R.string.form_pass), userPass)
          .data(getString(R.string.form_remember), if (userRemember) "1" else "0")
          .data(getString(R.string.form_anon), if (userAnon) "1" else "0")
          .method(method).execute()

        Log i(TAG, "Connected to " + action)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        val body = resp.body
        LoginResult(authKey, resp.cookies)

      } catch {
        case e: UnknownHostException => e.printStackTrace()
          LoginResult(null, null)
        case e: Throwable => e.printStackTrace()
          LoginResult(null, null)
      }
    }

    override protected def onPostExecute(result: LoginResult): Unit = {
      result match {
        case LoginResult(null, null) =>
          Toast makeText(MainActivity.this, R.string.chat_error_network, Toast.LENGTH_LONG) show()
        case LoginResult(a, c) =>
          authKey = a
          cookies = c
      }
      worker = null
      btnEnter setEnabled true
      super.onPostExecute(result)
    }
  }
}

object MainActivity {
  private[MainActivity] val TAG = classOf[MainActivity].getCanonicalName
  private[MainActivity] var self: MainActivity = null

  private[chat] val siteUrl = Uri parse "http://pirotehnika-ruhelp.com/index.php"
  private[chat] val enterUrl = Uri withAppendedPath(siteUrl, "?app=core&module=global&section=login")
  private[chat] val chatUrl = Uri withAppendedPath(siteUrl, "/shoutbox/")

  private[MainActivity] case class LoginResult(authKey: String, cookies: Map[String, String])
  private[MainActivity] var authKey = ""
  private[MainActivity] var cookies: Map[String, String] = null

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
