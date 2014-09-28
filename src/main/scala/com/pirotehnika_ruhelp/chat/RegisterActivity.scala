package com.pirotehnika_ruhelp.chat

import android.app.Activity
import android.content.{Context, SharedPreferences}
import android.net.Uri
import android.os.{AsyncTask, Bundle}
import android.util.Log
import android.view.View
import android.widget.{CheckBox, EditText, Button}
import org.jsoup.{Connection, Jsoup}

class RegisterActivity extends Activity with View.OnClickListener {
  private lazy val txtName  = findViewById(R.id.txtName)  .asInstanceOf[EditText]
  private lazy val txtPass  = findViewById(R.id.txtPass)  .asInstanceOf[EditText]
  private lazy val btnEnter = findViewById(R.id.btnEnter) .asInstanceOf[Button]
  private lazy val chkAnon  = findViewById(R.id.chkAnon)  .asInstanceOf[CheckBox]
  private lazy val chkRememberMe = findViewById(R.id.chkRememberMe) .asInstanceOf[CheckBox]

  private def userName = txtName.getText.toString
  private def userPass = txtPass.getText.toString
  private def rememberMe: Boolean = chkRememberMe.isChecked
  private def anonymous: Boolean = chkAnon.isChecked
  private var authKey: String = ""
  private var sessionId: String = ""

  private var worker: Worker = null
  import RegisterActivity._

  private def saveData(): Unit = {
    val prefs = getPreferences(Context.MODE_PRIVATE)
    val editor = prefs.edit()
    editor putString(USER_NAME, userName)
    editor putString(USER_PASS, userPass)
    editor putBoolean(USER_REMEMBER, rememberMe)
    editor putBoolean(USER_ANON, anonymous)
    editor putString(USER_KEY, authKey)
    editor putString(USER_ID, sessionId)
    editor.commit
  }

  private def loadData(): Unit = {
    val prefs = getPreferences(Context.MODE_PRIVATE)
    txtName setText(prefs getString(USER_NAME, ""))
    txtPass setText(prefs getString(USER_PASS, ""))
    chkAnon setChecked(prefs getBoolean(USER_ANON, false))
    chkRememberMe setChecked(prefs getBoolean(USER_REMEMBER, false))
    authKey = prefs getString(USER_KEY, "")
    sessionId = prefs getString(USER_ID, "")
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.registration)
    btnEnter setOnClickListener this
    loadData()
    if(!MainActivity.isNetworkAvailable) {
      btnEnter setText R.string.network_unawail
      btnEnter setEnabled false
    }
  }

  override def onClick(v: View): Unit = v.getId match {
    case R.id.btnEnter => if(worker eq null) {
      saveData()
      worker = new Worker(MainActivity.enterUrl, MainActivity.chatUrl)
      worker execute()
    }
  }

  // See http://piotrbuda.eu/2012/12/scala-and-android-asynctask-implementation-problem.html
  private class Worker(val loginUrl: Uri, var chatUrl: Uri)
    extends AsyncTask[AnyRef, Void, String] {

    override protected def onPreExecute(): Unit = {
      super.onPreExecute()
      assert(worker ne null)
      txtName setEnabled false
      txtPass setEnabled false
      btnEnter setEnabled false
      chkAnon setEnabled false
      chkRememberMe setEnabled false
    }

    override protected def doInBackground(params: AnyRef*): String = {
      var resp: Connection.Response = null

      try {
        Log i(TAG, "Connecting to " + loginUrl)
        resp = Jsoup.connect(loginUrl toString)
          .method(Connection.Method.GET).execute()
        Log i(TAG, "Connected to " + loginUrl)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        val form = resp parse() getElementsByTag "form" forms() get 0
        val authKey = form getElementsByAttributeValue("name", USER_KEY) `val`()

        val action = if (form.hasAttr("action"))
          form.absUrl("action")
        else form.baseUri()

        val method = if (form.attr("method").toUpperCase.equals("POST"))
          Connection.Method.POST
        else Connection.Method.GET

        Log i(TAG, "Send registration info to " + action)
        resp = Jsoup.connect(action)
          .data(USER_KEY, authKey)
          .data("referer", chatUrl toString)
          .data(USER_NAME, userName)
          .data(USER_PASS, userPass)
          .data(USER_REMEMBER, if (rememberMe) "1" else "0")
          .data(USER_ANON, if (anonymous) "1" else "0")
          .method(method).execute()

        Log i(TAG, "Connected to " + action)
        Log i(TAG, "Status code [" + resp.statusCode + "] - " + resp.statusMessage)

        val cookies = resp.cookies
        sessionId = cookies.get(USER_ID)
        val body = resp.body

      } catch {
        case e: Throwable => e printStackTrace()
      }

      authKey
    }

    override protected def onPostExecute(result: String): Unit = {
      txtName setEnabled true
      txtPass setEnabled true
      btnEnter setEnabled true
      chkAnon setEnabled true
      chkRememberMe setEnabled true
      worker = null
      authKey = result
      super.onPostExecute(result)
    }
  }
}

object RegisterActivity {
  protected[RegisterActivity] val TAG = classOf[RegisterActivity].getCanonicalName
  protected[chat] val USER_KEY = "auth_key"
  protected[chat] val USER_NAME = "ips_username"
  protected[chat] val USER_PASS = "ips_password"
  protected[chat] val USER_REMEMBER = "rememberMe"
  protected[chat] val USER_ANON = "anonymous"
  protected[chat] val USER_ID = "session_id"
}
