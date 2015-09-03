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

import java.io.{FileOutputStream, FileInputStream, File}

import android.graphics.{Bitmap, Canvas}
import android.graphics.drawable.{Drawable, BitmapDrawable}
import android.net.Uri

private[chat] class ChatDrawable (private var drawable: Option[Drawable] = None)
  extends BitmapDrawable(Chat.instance.getResources) {

  drawable foreach(d => setBounds(d.getBounds))

  override def draw(canvas: Canvas) = drawable foreach (_.draw(canvas))

  final def setDrawable(d: Drawable) {
    drawable = Some(d)
    setBounds(drawable.get.getBounds)
  }
}

private[chat] object ChatDrawable {
  def apply(uri: Uri) = {
    val dirPathToSave = Chat.instance.getExternalCacheDir.getAbsolutePath +
      (if(isSmile(uri)) "/smiles/" else "/pics/")
    val pathToSave = dirPathToSave + uri.getLastPathSegment

    val dir = new File(dirPathToSave)
    if(!dir.exists()) dir.mkdir()

    val f = new File(pathToSave)
    if(f.exists())
      getLocalDrawable(uri.toString, f)
    else
      getRemoteDrawable(uri.toString, f)
  }

  private def prepare(d: BitmapDrawable) = if(d ne null) {
      val bitmap = d.getBitmap
      d.setBounds(0, 0, bitmap.getWidth * 2, bitmap.getHeight * 2)
      d
    } else null

  private def isSmile(picUri: Uri): Boolean = {
    import collection.JavaConversions.asJavaCollection
    val seq = Seq("public", "style_emoticons", "default")
    picUri.getPathSegments.containsAll(seq)
  }

  private def getLocalDrawable(source: String, f: File) = {
    val is = new FileInputStream(f)
    val res = Drawable.createFromStream(is, null).asInstanceOf[BitmapDrawable]
    is.close()
    if(res eq null) // Файл поврежден, надо заменить
      getRemoteDrawable(source, f)
    else
      prepare(res)
  }

  private def getRemoteDrawable(source: String, f: File) = {
    import Chat.gui
    f.createNewFile()
    val res = new ChatDrawable()
    Chat.networker.downloadDrawable(source) onSuccess {
      case d: BitmapDrawable =>
        val os = new FileOutputStream(f)
        d.getBitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
        os.close()
        res.setDrawable(prepare(d))
        res.invalidateSelf()
    }
    res
  }
}
