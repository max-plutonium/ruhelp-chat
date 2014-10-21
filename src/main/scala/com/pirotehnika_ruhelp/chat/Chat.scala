package com.pirotehnika_ruhelp.chat

import android.app.ProgressDialog
import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.preference.PreferenceManager
import android.text.Html
import android.util.Log
import android.view.ContextMenu.ContextMenuInfo
import android.view.{MenuItem, ContextMenu, View}
import android.view.inputmethod.InputMethodManager
import android.widget.{AdapterView, TextView, Toast}

protected[chat] abstract class Chat
  (private val activity: TypedActivity) extends NetworkWorker {
  private[chat] lazy val prefs = PreferenceManager getDefaultSharedPreferences activity
  private[chat] val guiHandler: GuiHandler = new GuiHandler(activity)

  import Chat._
  import implicits.ListenerBuilders._

  final def start() = guiHandler sendMessage StartChat

  final def login() = {
    guiHandler startProgress(R.string.chat_login_progress_title, 5)
    workerHandler post performLogin
    guiHandler.loginOrLogout = true
  }

  final def logout() = {
    guiHandler startProgress(R.string.chat_logout_progress_title, 2)
    workerHandler post performLogout
    guiHandler.loginOrLogout = true
  }

  final def isUserEntered = userEntered
  final def isLoginOrLogout = guiHandler.loginOrLogout

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

  private[chat] final def getString(resId: Int) = activity getString resId

  private[chat] final def isNetworkAvailable: Boolean = {
    val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    if(cm eq null) false
    val netInfo = cm.getAllNetworkInfo
    if(netInfo eq null) false
    netInfo exists (ni => if (ni.isConnected) {
      Log d(TAG, ni.getTypeName + " connection found")
      true } else false)
  }

  private[chat] sealed trait MessageForGui
  private[chat] case object StartChat extends MessageForGui
  private[chat] case class UpdateProgress(message: String) extends MessageForGui
  private[chat] case class Messages(seq: Seq[Message]) extends MessageForGui
  private[chat] case class Members(total: Int, guests: Int,
    members: Int, anons: Int, seq: Option[Seq[Member]]) extends MessageForGui
  private[chat] case class PostError(errorStringId: Int) extends MessageForGui

  private[chat] class GuiHandler(private val context: Context) extends Handler {
    private val arrayList = collection.mutable.ArrayBuffer[Message]()
    private lazy val listAdapter = MessageAdapter(context, arrayList)
    private lazy val lstChat = activity findView TR.lstChat
    private lazy val tvMessage = activity findView TR.tvMessage
    private lazy val btnPost = activity findView TR.btnPost
    private var progressDialog: ProgressDialog = null
    private[Chat] var loginOrLogout = false
    private var messagePending = false

    def sendMessage(msg: MessageForGui) = super.sendMessage(obtainMessage(1, msg))

    final def startProgress(titleId: Int, steps: Int) = {
      progressDialog = new ProgressDialog(context)
      progressDialog setTitle titleId
      progressDialog setMessage ""
      progressDialog setProgressStyle ProgressDialog.STYLE_HORIZONTAL
      progressDialog setProgress 0
      progressDialog setMax steps
      progressDialog show()
    }

    private final def startSpinnerProgress(message: String) = {
      progressDialog = new ProgressDialog(context)
      progressDialog setMessage message
      progressDialog setProgressStyle ProgressDialog.STYLE_SPINNER
      progressDialog show()
    }

    private final def updateProgress(message: String) = {
      progressDialog setProgress progressDialog.getProgress + 1
      progressDialog setMessage message
    }

    private final def stopProgress() = {
      progressDialog dismiss()
      progressDialog = null
    }

    private final def startAutoLogin() = {
      if(workerHandler ne null) {
        tryAutoLogin()
        startSpinnerProgress(getString(R.string.chat_login_progress_title))
        loginOrLogout = true
      } else
        sendMessage(StartChat)
    }

    private final def hideKeyboard(): Unit = {
      val imManager = context.getSystemService(
        Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
      imManager.hideSoftInputFromWindow(tvMessage.getWindowToken, 0)
    }

    override def handleMessage(message: android.os.Message): Unit = {
      super.handleMessage(message)
      message.obj match {
        case StartChat =>
          lstChat setAdapter listAdapter
          btnPost setOnClickListener((v: View) => {
              val text = tvMessage.getText.toString
              if (!text.trim.isEmpty) {
                postMessage(text)
                v setEnabled false
                messagePending = true
              }
              hideKeyboard()
            })

          lstChat setItemsCanFocus true

          lstChat setOnCreateContextMenuListener(
            (menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) => {
              val minfo = menuInfo.asInstanceOf[AdapterView.AdapterContextMenuInfo]
              val n = Html.fromHtml(arrayList(minfo.position).name).toString

              if(n != getString(R.string.key_system_user)) {
                activity.getMenuInflater.inflate(R.menu.chatlist_item, menu)
                val mi = menu findItem R.id.chatlistmenu_itemname
                assert(mi ne null)
                mi setTitle s"@$n " setOnMenuItemClickListener ((item: MenuItem) => {
                  tvMessage.append(item.getTitle); true }) setShowAsAction 0
              }
            })

          if(!prefs.getString(getString(R.string.key_user_name), "").isEmpty)
            startAutoLogin()

        case UpdateProgress(msg) => updateProgress(msg)

        case Members(total, guests, members, anons, seq) =>
          val r = 0
          r + 2

        case Messages(seq) =>
          if(loginOrLogout) {
            if("entered" equals seq(0).id)
              btnPost setEnabled true
            else if("not entered" equals seq(0).id)
              btnPost setEnabled false
            stopProgress()
            loginOrLogout = false
          }
          arrayList ++= seq
          listAdapter notifyDataSetChanged()
          lstChat smoothScrollToPosition listAdapter.getCount
          if(messagePending) {
            messagePending = false
            btnPost setEnabled true
            tvMessage.setText("", TextView.BufferType.NORMAL)
          }

        case PostError(errorId) =>
          btnPost setEnabled true
          Toast makeText(context, errorId, Toast.LENGTH_LONG) show()
      }
    }
  }
}

protected[chat] object Chat {
  import works._
  private[Chat] val TAG = classOf[Chat].getCanonicalName
  private[chat] def apply(activity: TypedActivity): Chat =
    new Chat(activity) with Login with Logout
      with ObtainMembers with ObtainMessages
      with PostMessage
}