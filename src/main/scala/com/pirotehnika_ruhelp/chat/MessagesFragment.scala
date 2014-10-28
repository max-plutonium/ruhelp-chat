package com.pirotehnika_ruhelp.chat

import android.os.Bundle
import android.app.Fragment
import android.text.Html
import android.view.ContextMenu.ContextMenuInfo
import android.view._
import android.widget.AdapterView

class MessagesFragment extends Fragment {
  import TypedResource._
  import implicits.ListenerBuilders._
  private val messageBuffer = collection.mutable.ArrayBuffer[Message]()
  private lazy val listAdapter = new MessageAdapter(getActivity, messageBuffer)
  private lazy val lstMessages = getView findView TR.lstMessages

  var appendTextCallback: Option[String => Unit] = None

  override final def onCreateView(inflater: LayoutInflater,
    container: ViewGroup, savedInstanceState: Bundle): View =
    inflater inflate(TR.layout.messages_fragment, null)

  override final def onActivityCreated(savedInstanceState: Bundle): Unit = {
    lstMessages setAdapter listAdapter
    lstMessages setItemsCanFocus true
    lstMessages setOnCreateContextMenuListener {
      (menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) => {
        val minfo = menuInfo.asInstanceOf[AdapterView.AdapterContextMenuInfo]
        val n = Html.fromHtml(messageBuffer(minfo.position).name).toString

        if(n != getString(R.string.key_system_user)) {
          getActivity.getMenuInflater.inflate(R.menu.chatlist_item, menu)
          val mi = menu findItem R.id.chatlistmenu_itemname
          assert(mi ne null)
          mi setTitle s"@$n " setOnMenuItemClickListener ((item: MenuItem) => {
              appendTextCallback foreach(_(item.getTitle.toString))
              true
            })
          ()
        }
      }}
    super.onActivityCreated(savedInstanceState)
  }

  final def appendMessages(seq: Seq[Message]) = {
    messageBuffer ++= seq
    listAdapter notifyDataSetChanged()
    lstMessages smoothScrollToPosition listAdapter.getCount
  }
}
