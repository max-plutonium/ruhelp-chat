package com.pirotehnika_ruhelp.chat

import android.content.Context
import android.graphics.drawable.{BitmapDrawable, Drawable}
import android.net.Uri
import android.text.Html
import android.text.Html.ImageGetter
import android.view.{LayoutInflater, ViewGroup, View}
import android.widget.BaseAdapter

protected[chat] class MessageAdapter(private val context: Context,
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
    val name = Html.fromHtml(m.name)
    val timestamp = Html.fromHtml(m.timestamp)
    val text = Html.fromHtml(m.text, imageGetter, null)
    view.findView(TR.tvName).setText(name)
    view.findView(TR.tvDate).setText(timestamp)
    view.findView(TR.tvText).setText(text)
    view
  }

  private val imageGetter = new ImageGetter {
    import collection.JavaConversions.asJavaCollection
    override def getDrawable(source: String): Drawable = {
      val picUri = Uri parse source
      val seq = Seq("public", "style_emoticons", "default")
      if (picUri.getPathSegments.containsAll(seq)) {
        try {
          val is = context.getAssets.open("smiles/" + picUri.getLastPathSegment)
          if (is eq null) return null
          val drawable = Drawable.createFromStream(is, null).asInstanceOf[BitmapDrawable]
          val bitmap = drawable.getBitmap
          drawable.setBounds(0, 0, bitmap.getWidth * 2, bitmap.getHeight * 2)
          drawable

        } catch { case e: java.io.FileNotFoundException =>
          null
        }
      }
      else null
    }
  }
}

protected[chat] object MessageAdapter {
  def apply(context: Context, messages: Seq[Message]) =
    new MessageAdapter(context, messages)
}
