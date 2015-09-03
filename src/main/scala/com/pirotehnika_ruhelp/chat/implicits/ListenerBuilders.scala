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
