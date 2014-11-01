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