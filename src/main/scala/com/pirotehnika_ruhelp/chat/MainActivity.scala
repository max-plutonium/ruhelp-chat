package com.pirotehnika_ruhelp.chat

import java.io.IOException
import java.util.Map

import android.app.Activity
import android.content.{Context, Intent}
import android.net.{ConnectivityManager, Uri}
import android.os.{AsyncTask, Bundle}
import android.preference.PreferenceManager
import android.util.Log
import android.view.MenuItem.OnMenuItemClickListener
import android.view.{Menu, MenuItem, View}
import android.widget.{Button, TextView, Toast}
import org.jsoup.{Connection, Jsoup}

class MainActivity extends Activity {
  import com.pirotehnika_ruhelp.chat.MainActivity._
  private lazy val textView = findViewById(R.id.chatText).asInstanceOf[TextView]
  private lazy val prefs = PreferenceManager getDefaultSharedPreferences this
  private var worker: Worker = null

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

  override def onPrepareOptionsMenu(menu: Menu): Boolean = {
    val mi = menu findItem R.id.menu_signing
    assert(mi ne null)
    if(userEntered) {
      mi setTitle R.string.chat_menu_sign_off
      mi setOnMenuItemClickListener new OnMenuItemClickListener {
        override def onMenuItemClick(item: MenuItem): Boolean = {
          finish() // TODO
          true
        }
      }
    } else {
      mi setTitle R.string.chat_menu_sign_on
      mi setOnMenuItemClickListener new OnMenuItemClickListener {
        override def onMenuItemClick(item: MenuItem): Boolean = {
          worker = new Worker(enterUrl, chatUrl)
          worker execute()
          true
        }
      }
    }

    mi setEnabled(worker == null)
    super.onCreateOptionsMenu(menu)
  }

  // See http://piotrbuda.eu/2012/12/scala-and-android-asynctask-implementation-problem.html
  private class Worker(val loginUrl: Uri, var chatUrl: Uri)
    extends AsyncTask[AnyRef, Void, LoginResult] {

    val userName = prefs getString(getString(R.string.key_user_name), "")
    val userPass = prefs getString(getString(R.string.key_user_pass), "")
    val userRemember = prefs getBoolean(getString(R.string.key_user_remember), false)
    val userAnon = prefs getBoolean(getString(R.string.key_user_anon), false)

    override protected def onPreExecute(): Unit = {
      super.onPreExecute()
      assert(worker ne null)
    }

    override protected def doInBackground(params: AnyRef*): LoginResult = {
      var resp: Connection.Response = null

      try {
        Log i(TAG, "Request login info from " + loginUrl)
        resp = Jsoup.connect(loginUrl toString)
          .method(Connection.Method.GET).execute()
        Log i(TAG, "Connected to " + loginUrl)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        val form = resp parse() getElementsByTag "form" forms() get 0
        val authKey = form getElementsByAttributeValue("name",
          getString(R.string.key_form_auth)) `val`()

        val action = if (form.hasAttr("action"))
          form.absUrl("action") else form.baseUri()

        val method = if (form.attr("method").toUpperCase.equals("POST"))
          Connection.Method.POST else Connection.Method.GET

        Log i(TAG, "Send login info to " + action)
        resp = Jsoup.connect(action)
          .data(getString(R.string.key_form_auth), authKey)
          .data(getString(R.string.key_form_referrer), chatUrl toString)
          .data(getString(R.string.key_form_name), userName)
          .data(getString(R.string.key_form_pass), userPass)
          .data(getString(R.string.key_form_remember), if (userRemember) "1" else "0")
          .data(getString(R.string.key_form_anon), if (userAnon) "1" else "0")
          .method(method).execute()

        Log i(TAG, "Connected to " + action)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)
        Log i(TAG, "Login successful")
        LoginResult(authKey, resp.cookies)

      } catch { case e: IOException =>
          Log e(TAG, "Login failure, caused: " + e.getMessage)
          e.printStackTrace()
          LoginResult(null, null)
      }
    }

    override protected def onPostExecute(result: LoginResult): Unit = {
      userEntered = result match {
        case LoginResult(null, null) =>
          Toast makeText(MainActivity.this, R.string.chat_error_network, Toast.LENGTH_LONG) show()
          false
        case LoginResult(a, c) =>
          authKey = a
          cookies = c
          Toast makeText(MainActivity.this, R.string.chat_user_login, Toast.LENGTH_LONG) show()
          true
      }
      worker = null
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
