package com.pirotehnika_ruhelp.chat

import android.app.AlertDialog
import android.content.{DialogInterface, Intent}
import android.os._
import android.provider.Settings
import android.view.{Menu, MenuItem}
import android.widget._

class MainActivity extends TypedActivity {
  import MainActivity._
  import implicits.ListenerBuilders._
  private val chat = new Chat(this)

  override protected def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    self = this
    chat start()
  }

  override def onCreateOptionsMenu(menu: Menu) = {
    getMenuInflater inflate(R.menu.main, menu)
    val mi = menu findItem R.id.menu_prefs
    assert(mi ne null)
    mi setIntent new Intent(this, classOf[PreferenceActivity])
    super.onCreateOptionsMenu(menu)
  }

  private def getLoginListener: MenuItem.OnMenuItemClickListener =
    if(chat.isNetworkAvailable)
      if(chat.isUserEntered)
        (item: MenuItem) => { chat logout(); true }
      else
        (item: MenuItem) => { chat login(); true }

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

  override def onPrepareOptionsMenu(menu: Menu): Boolean = {
    val mi = menu findItem R.id.menu_signing
    assert(mi ne null)
    mi setTitle(if(chat.isUserEntered) R.string.chat_menu_sign_off
      else R.string.chat_menu_sign_on)
    mi setOnMenuItemClickListener getLoginListener
    mi setEnabled !chat.isLoginOrLogout
    super.onCreateOptionsMenu(menu)
  }
}

object MainActivity {
  private[MainActivity] val TAG = classOf[MainActivity].getCanonicalName
  private[MainActivity] var self: MainActivity = null
}
