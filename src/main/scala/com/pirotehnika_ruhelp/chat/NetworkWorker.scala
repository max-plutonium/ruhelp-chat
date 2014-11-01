package com.pirotehnika_ruhelp.chat

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.text.Spanned

import scala.concurrent.Future

private[chat] case class Message(id: String, name: Spanned, timestamp: Spanned, text: Spanned)

private[chat] case class Member(name: String, href: String, lastTime: String)

private[chat] case class Smile(code: String, url: String)

private[chat] sealed trait MessageForGui
private[chat] case class UpdateProgress(message: String) extends MessageForGui
private[chat] case class Messages(seq: Seq[Message]) extends MessageForGui
private[chat] case class Members(total: Int, guests: Int,
  members: Int, anons: Int, seq: Option[Seq[Member]]) extends MessageForGui
private[chat] case class PostError(errorStringId: Int) extends MessageForGui

private[chat] trait GuiWorker extends Handler {
  final def sendMessage(msg: MessageForGui) = super.sendMessage(obtainMessage(1, msg))
}

private[chat] trait NetworkWorker {
  protected val context: Context
  protected val gui: GuiWorker
  def login(): Unit
  def logout(): Unit
  def userEntered: Boolean
  def tryAutoLogin(): Unit
  def postMessage(text: String): Unit
  def downloadDrawable(url: String): Future[Drawable]
  def obtainSmiles: Future[Seq[Smile]]
}

private[chat] object NetworkWorker {
  def apply(context: Context, gui: GuiWorker) = works.NetWork(context, gui)
}
