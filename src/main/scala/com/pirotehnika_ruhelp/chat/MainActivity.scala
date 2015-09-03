/*
 * Copyright (C) 2014-2015 Max Plutonium <plutonium.max@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE ABOVE LISTED COPYRIGHT HOLDER(S) BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name(s) of the above copyright
 * holders shall not be used in advertising or otherwise to promote the
 * sale, use or other dealings in this Software without prior written
 * authorization.
 */
package com.pirotehnika_ruhelp.chat

import android.app.{ProgressDialog, AlertDialog}
import android.content.{Context, DialogInterface, Intent}
import android.net.ConnectivityManager
import android.os
import android.os._
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.{AttributeSet, Log}
import android.view.View.MeasureSpec
import android.view.inputmethod.InputMethodManager
import android.view.{Menu, MenuItem}
import android.widget._

class MeasureLayout(context: Context, attributeSet: AttributeSet)
  extends LinearLayout(context, attributeSet) {
  private var keyboardVisible = true

  var onKeyboardVisibleCallback: Option[Boolean => Unit] = None

  override protected def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val propHeight = MeasureSpec.getSize(heightMeasureSpec)
    val actualHeight = getHeight

    if(actualHeight > propHeight && !keyboardVisible) {
      keyboardVisible = true
      onKeyboardVisibleCallback foreach(_(keyboardVisible))
    } else if(actualHeight < propHeight && keyboardVisible) {
      keyboardVisible = false
      onKeyboardVisibleCallback foreach(_(keyboardVisible))
    }

    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }
}

class MainActivity extends android.support.v4.app.FragmentActivity {
  private val TAG = classOf[MainActivity].getCanonicalName
  import implicits.ListenerBuilders._

  private lazy val prefs = PreferenceManager getDefaultSharedPreferences this
  private lazy val fragMessages = getSupportFragmentManager.
    findFragmentById(R.id.fragMessages).asInstanceOf[MessagesFragment]
  private lazy val fragPostForm = getSupportFragmentManager.
    findFragmentById(R.id.fragPostForm).asInstanceOf[PostFormFragment]
  private val fragSmiles = new SmilesFragment

  private var loginOrLogout = false
  private var messagePending = false
  private var progressDialog: ProgressDialog = _

  override protected final def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    Chat.instance = this
    Chat.handler = new GuiHandler
    Chat.networker = NetworkWorker(this)

    setContentView(R.layout.main)
    val lytMain = findViewById(R.id.lytMain).asInstanceOf[MeasureLayout]
    lytMain.onKeyboardVisibleCallback = Some { visible => if(visible && fragSmiles.isAdded)
          getSupportFragmentManager beginTransaction() remove fragSmiles commit()
        ()
      }

    fragMessages.appendTextCallback = Some(fragPostForm appendText _)

    fragPostForm.onSmilesCallback = Some { smilesShown =>
        val trans = getSupportFragmentManager.beginTransaction()
        if (smilesShown) {
          trans.add(R.id.lytSmiles, fragSmiles).addToBackStack(null)
          hideKeyboard()
        } else
          trans remove fragSmiles
        trans.commit()
        ()
      }

    fragPostForm.postMessageCallback = Some { text =>
        Chat.networker postMessage text
        messagePending = true
        hideKeyboard()
      }

    fragSmiles.onSmileSelectedCallback = Some(fragPostForm appendText _)
    fragSmiles.onHideCallback = Some(() => fragPostForm setSmilesStateHidden())

    if(!prefs.getString(getString(R.string.key_user_name), "").isEmpty)
      Chat.handler postDelayed(new Runnable {
        override def run(): Unit = startAutoLogin()
      }, 100)
  }

  override final def onCreateOptionsMenu(menu: Menu) = {
    getMenuInflater inflate(R.menu.main, menu)
    val mi = menu findItem R.id.menu_prefs
    assert(mi ne null)
    mi setIntent new Intent(this, classOf[PreferenceActivity])
    super.onCreateOptionsMenu(menu)
  }

  override final def onPrepareOptionsMenu(menu: Menu) = {
    val mi = menu findItem R.id.menu_signing
    assert(mi ne null)
    mi setTitle(if(Chat.networker.userEntered) R.string.chat_menu_sign_off
      else R.string.chat_menu_sign_on)
    mi setOnMenuItemClickListener getLoginListener
    mi setEnabled !loginOrLogout
    super.onPrepareOptionsMenu(menu)
  }

  private final def getLoginListener: MenuItem.OnMenuItemClickListener =
    if(isNetworkAvailable)
      if(Chat.networker.userEntered)
        (item: MenuItem) => {
          startProgress(R.string.chat_logout_progress_title, 2)
          Chat.networker logout()
          loginOrLogout = true
          true
        }
      else
        (item: MenuItem) => {
          startProgress(R.string.chat_login_progress_title, 5)
          Chat.networker login()
          loginOrLogout = true
          true
        }

    // Нет сети
    else (item: MenuItem) => {
      val builder = new AlertDialog.Builder(this)
      builder setTitle R.string.chat_login_alert_title
      builder setMessage R.string.chat_login_alert_msg

      builder.setPositiveButton(R.string.chat_login_alert_yes,
        (dialog: DialogInterface, which: Int) =>
          startActivity(new Intent(Settings.ACTION_SETTINGS)))

      builder setNegativeButton(R.string.chat_login_alert_no,
        (dialog: DialogInterface, which: Int) =>
          Toast makeText(MainActivity.this,
            R.string.chat_login_alert_on_cancel,
            Toast.LENGTH_LONG) show())

      builder create() show()
      true
    }

  private final def hideKeyboard() {
    getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
      .hideSoftInputFromWindow(getCurrentFocus.getWindowToken, 0)
  }

  private final def isNetworkAvailable: Boolean = {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    if(cm eq null) false
    val netInfo = cm.getAllNetworkInfo
    if(netInfo eq null) false
    netInfo exists (ni => if (ni.isConnected) {
      Log d(TAG, ni.getTypeName + " connection found")
      true } else false)
  }

  private final def startSpinnerProgress(message: String) {
    progressDialog = new ProgressDialog(this)
    progressDialog setMessage message
    progressDialog setProgressStyle ProgressDialog.STYLE_SPINNER
    progressDialog show()
  }

  private final def startProgress(titleId: Int, steps: Int) {
    progressDialog = new ProgressDialog(this)
    progressDialog setTitle titleId
    progressDialog setMessage ""
    progressDialog setProgressStyle ProgressDialog.STYLE_HORIZONTAL
    progressDialog setProgress 0
    progressDialog setMax steps
    progressDialog show()
  }

  private final def updateProgress(message: String) {
    progressDialog setProgress progressDialog.getProgress + 1
    progressDialog setMessage message
  }

  private final def stopProgress() {
    progressDialog dismiss()
    progressDialog = null
  }

  private final def startAutoLogin() {
    Chat.networker tryAutoLogin()
    startSpinnerProgress(getString(R.string.chat_login_progress_title))
    loginOrLogout = true
  }

  private final class GuiHandler extends GuiWorker {
    import Chat.gui

    override def handleMessage(msg: os.Message) {
      super.handleMessage(msg); msg.obj match {
        case UpdateProgress(m) => updateProgress(m)
        case Members(total, guests, members, anons, seq) =>
        case PostError(errorId) => fragPostForm onPostError errorId
        case Messages(seq) =>
          if(loginOrLogout) {
            if("entered" equals seq(0).id) {
              fragPostForm onUserEnter()
              Chat.networker.obtainSmiles onSuccess {
                case seq: Seq[Smile] => fragSmiles setupSmiles seq
              }
            } else if("not entered" equals seq(0).id)
              fragPostForm onUserExit()
            stopProgress()
            loginOrLogout = false
          }

          fragMessages appendMessages seq

          if(messagePending) {
            messagePending = false
            fragPostForm onMessagePosted()
          }
      }
    }
  }
}
