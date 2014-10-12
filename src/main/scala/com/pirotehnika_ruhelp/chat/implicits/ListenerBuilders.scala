package com.pirotehnika_ruhelp.chat
package implicits

import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.view.{View, MenuItem}

private[chat] object ListenerBuilders {
  implicit def menuItemClickListenerBuilder
    (f: MenuItem => Boolean) = new MenuItem.OnMenuItemClickListener {
        override def onMenuItemClick(item: MenuItem): Boolean = f(item)
      }

  implicit def viewClickListenerBuilder
    (f: View => Unit) = new View.OnClickListener {
        override def onClick(v: View): Unit = f(v)
      }

  implicit def dialogInterfaceClickListenerBuilder
    (f: (DialogInterface, Int) => Unit) = new OnClickListener {
      override def onClick(dialog: DialogInterface, which: Int): Unit =
        f(dialog, which)
    }
}
