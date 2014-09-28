package com.pirotehnika_ruhelp.chat

import android.app.Activity
import android.content.{Context, Intent}
import android.net.{ConnectivityManager, Uri}
import android.os.Bundle
import android.util.Log
import android.view.{MenuItem, Menu, View}
import android.widget.{Toast, Button, TextView}

class MainActivity extends Activity with View.OnClickListener {
  private lazy val textView = findViewById(R.id.textView).asInstanceOf[TextView]
  private lazy val btnOk = findViewById(R.id.btnOk).asInstanceOf[Button]
  private lazy val btnCancel = findViewById(R.id.btnCancel).asInstanceOf[Button]
  import MainActivity._

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    btnOk setOnClickListener this
    btnCancel setOnClickListener this
    self = this
  }

  override def onClick(v: View): Unit = v.getId match {
    case R.id.btnOk => startActivity(new Intent(this, classOf[RegisterActivity]))
    case R.id.btnCancel => finish()
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater inflate(R.menu.main, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean =
    item.getItemId match {
      case R.id.menu_login => startActivity(new Intent(this, classOf[RegisterActivity]))
        super.onOptionsItemSelected(item)
    }
}

object MainActivity {
  protected[MainActivity] val TAG = classOf[MainActivity].getCanonicalName
  protected[MainActivity] var self: MainActivity = null
  protected[chat] val siteUrl = Uri parse "http://pirotehnika-ruhelp.com/index.php"
  protected[chat] val enterUrl = Uri withAppendedPath(siteUrl, "?app=core&module=global&section=login")
  protected[chat] val chatUrl = Uri withAppendedPath(siteUrl, "/shoutbox/")

  protected[chat] def isNetworkAvailable: Boolean = {
    val cm = self.getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
    if(cm eq null) false
    val netInfo = cm.getAllNetworkInfo
    if(netInfo eq null) false
    netInfo exists (ni => if (ni.isConnected) {
      Log d(TAG, ni.getTypeName + " connection found")
      true } else false)
  }
}
