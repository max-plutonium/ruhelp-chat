package com.pirotehnika_ruhelp.chat

import android.content.Context
import android.view.{LayoutInflater, ViewGroup, View}
import android.widget.{TextView, BaseAdapter}

protected[chat] class MessageAdapter(private val context: Context,
  private val messages: Seq[Message]) extends BaseAdapter {

  private val inflater = context.getSystemService(
    Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]

  override def getCount: Int = messages.size
  override def getItem(position: Int): AnyRef = messages(position)
  override def getItemId(position: Int): Long = position

  override def getView(position: Int,
    convertView: View, parent: ViewGroup): View = {

    // Переиспользуем пункты за пределами экрана,
    // чтобы не гонять тяжелый inflate
    var view = convertView
    if(view eq null)
      view = inflater inflate(R.layout.chatlist_item, parent, false)

    // Заполняем View данными из Message
    val m = messages(position)
    view.findViewById(R.id.tvName).asInstanceOf[TextView].setText(m.name)
    view.findViewById(R.id.tvDate).asInstanceOf[TextView].setText(m.timestamp)
    view.findViewById(R.id.tvText).asInstanceOf[TextView].setText(m.text)
    view
  }
}

protected[chat] object MessageAdapter {
  def apply(context: Context, messages: Seq[Message]) =
    new MessageAdapter(context, messages)
}