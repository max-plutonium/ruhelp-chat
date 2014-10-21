package com.pirotehnika_ruhelp.chat
package implicits

import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnCreateContextMenuListener
import android.view.{ContextMenu, View, MenuItem}
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener

private[chat] object ListenerBuilders {
  implicit def menuItemClickListenerBuilder
    (f: MenuItem => Boolean) = new MenuItem.OnMenuItemClickListener {
        override def onMenuItemClick(item: MenuItem): Boolean = f(item)
      }

  implicit def viewClickListenerBuilder
    (f: View => Unit) = new View.OnClickListener {
        override def onClick(v: View): Unit = f(v)
      }

  implicit def viewOnCreateContextMenuListenerBuilder
    (f: (ContextMenu, View, ContextMenuInfo) => Unit) =
      new OnCreateContextMenuListener { override def onCreateContextMenu
        (menu: ContextMenu, v: View, menuInfo: ContextMenuInfo): Unit = f(menu, v, menuInfo)
      }

  implicit def dialogInterfaceClickListenerBuilder
    (f: (DialogInterface, Int) => Unit) = new OnClickListener {
      override def onClick(dialog: DialogInterface, which: Int): Unit =
        f(dialog, which)
    }

  implicit def adapterViewItemClickListenerBuilder
    (f: (AdapterView[_], View, Int, Long) => Unit) = new OnItemClickListener {
      override def onItemClick(parent: AdapterView[_], view: View,
        position: Int, id: Long): Unit = f(parent, view, position, id)
    }
}
