package com.pirotehnika_ruhelp.chat

import android.os.Bundle
import android.app.ListFragment
import android.view.ContextMenu.ContextMenuInfo
import android.view._
import android.widget.AdapterView

class MessagesFragment extends ListFragment {
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
          mi setTitle s"@$n " setOnMenuItemClickListener {
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
