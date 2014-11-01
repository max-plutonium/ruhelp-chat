package com.pirotehnika_ruhelp.chat

import android.content.Context
import android.view.{LayoutInflater, ViewGroup, View}
import android.widget.BaseAdapter

private[chat] class MessageAdapter(private val context: Context,
  private val messages: Seq[Message]) extends BaseAdapter {
  import TypedResource._

  private val inflater: TypedLayoutInflater = context.getSystemService(
    Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]

  override def getCount: Int = messages.size
  override def getItem(position: Int): AnyRef = messages(position)
  override def getItemId(position: Int): Long = position

  override def getView(position: Int,
    convertView: View, parent: ViewGroup): View = {

    // Переиспользуем пункты за пределами экрана,
    // чтобы не гонять тяжелый inflate
    val view = if(convertView ne null) convertView
      else inflater inflate(TR.layout.chatlist_item, parent, false)

    // Заполняем View данными из Message
    val m = messages(position)
    view.findView(TR.tvName).setText(m.name)
    view.findView(TR.tvDate).setText(m.timestamp)
    view.findView(TR.tvText).setText(m.text)
    view
  }
}
