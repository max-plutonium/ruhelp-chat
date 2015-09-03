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

import android.os.Bundle
import android.view.ContextMenu.ContextMenuInfo
import android.view._
import android.widget.AdapterView

class MessagesFragment extends android.support.v4.app.ListFragment {
  import implicits.ListenerBuilders._
  private val messageBuffer = collection.mutable.ArrayBuffer[Message]()
  private lazy val listAdapter = new MessageAdapter(getActivity, messageBuffer)

  var appendTextCallback: Option[String => Unit] = None

  override final def onActivityCreated(savedInstanceState: Bundle) {
    getListView setOnCreateContextMenuListener {
      (menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) =>
        val minfo = menuInfo.asInstanceOf[AdapterView.AdapterContextMenuInfo]
        val n = messageBuffer(minfo.position).name.toString

        if(n != getString(R.string.key_system_user)) {
          getActivity.getMenuInflater.inflate(R.menu.chatlist_item, menu)
          val mi = menu findItem R.id.chatlistmenu_itemname
          assert(mi ne null)
          mi setTitle s"@$n" setOnMenuItemClickListener {
            (item: MenuItem) =>
              appendTextCallback foreach(_(item.getTitle.toString))
              true
            }
          ()
        }
      }
    super.onActivityCreated(savedInstanceState)
  }

  final def appendMessages(seq: Seq[Message]) {
    if(null eq getListAdapter)
      setListAdapter(listAdapter)
    messageBuffer ++= seq
    listAdapter notifyDataSetChanged()
    getListView smoothScrollToPosition listAdapter.getCount
  }
}
