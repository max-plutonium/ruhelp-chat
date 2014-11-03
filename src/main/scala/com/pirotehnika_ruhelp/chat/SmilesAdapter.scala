package com.pirotehnika_ruhelp.chat

import android.content.Context
import android.net.Uri
import android.view.{LayoutInflater, ViewGroup, View}
import android.widget.BaseAdapter

private[chat] class SmilesAdapter(private val context: Context,
  private val smiles: Seq[Smile]) extends BaseAdapter {
  import TypedResource._

  private val inflater: TypedLayoutInflater = context.getSystemService(
    Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]

  override def getCount: Int = smiles.size
  override def getItem(position: Int): AnyRef = smiles(position)
  override def getItemId(position: Int): Long = position

  override def getView(position: Int,
    convertView: View, parent: ViewGroup): View = {

    // Переиспользуем пункты за пределами экрана,
    // чтобы не гонять тяжелый inflate
    val view = if(convertView ne null) convertView
      else inflater inflate(TR.layout.smile_item, parent, false)

    val drawable = ChatDrawable(Uri parse smiles(position).url)
    view findView TR.ivSmile setImageDrawable drawable
    view
  }
}
