package com.pirotehnika_ruhelp.chat

import android.content.res.Resources
import android.graphics.{Rect, Canvas}
import android.graphics.drawable.{Drawable, BitmapDrawable}

private[chat] class ChatDrawable (res: Resources,
  private var drawable: Option[Drawable] = None) extends BitmapDrawable(res) {

  drawable foreach(d => setBounds(d.getBounds))

  override def draw(canvas: Canvas) = drawable foreach (_.draw(canvas))

  final def setDrawable(d: Drawable) {
    drawable = Some(d)
    setBounds(drawable.get.getBounds)
  }

  override final def setBounds(left: Int, top: Int, right: Int, bottom: Int) = {
    drawable foreach (_.setBounds(left, top, right, bottom))
    super.setBounds(left, top, right, bottom)
  }
}
