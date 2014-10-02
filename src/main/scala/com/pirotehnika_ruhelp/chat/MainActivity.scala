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
  import MainActivity._
  private val chat = new Chat(this)

  override protected def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    chat start()
    self = this
  }

//  override protected def onSaveInstanceState(outState: Bundle) = {
//    super.onSaveInstanceState(outState)
//    outState putString("qwer", "123")
//  }
//
//  override protected def onRestoreInstanceState(savedInstanceState: Bundle) = {
//    super.onRestoreInstanceState(savedInstanceState)
//    val res = savedInstanceState getString("qwer")
//    val res2 = res.substring(1)
//  }

  override def onCreateOptionsMenu(menu: Menu) = {
    getMenuInflater inflate(R.menu.main, menu)
    val mi = menu findItem R.id.menu_prefs
    assert(mi ne null)
    mi setIntent new Intent(this, classOf[PreferenceActivity])
    super.onCreateOptionsMenu(menu)
  }

  private def getLoginListener: MenuItem.OnMenuItemClickListener = {
    if(chat.isNetworkAvailable)
      if(chat.isUserEntered)
        new MenuItem.OnMenuItemClickListener {
          override def onMenuItemClick(item: MenuItem): Boolean = {
            chat logout()
            true
          }
        }
      else new MenuItem.OnMenuItemClickListener {
          override def onMenuItemClick(item: MenuItem): Boolean = {
            chat login()
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
