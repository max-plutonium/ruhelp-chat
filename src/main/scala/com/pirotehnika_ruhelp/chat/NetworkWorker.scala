package com.pirotehnika_ruhelp.chat

import android.content.Context
import android.os.Handler

case class Message(id: String, name: String, timestamp: String, text: String)

case class Member(name: String, href: String, lastTime: String)

private[chat] sealed trait MessageForGui
private[chat] case object StartChat extends MessageForGui
private[chat] case class UpdateProgress(message: String) extends MessageForGui
private[chat] case class Messages(seq: Seq[Message]) extends MessageForGui
private[chat] case class Members(total: Int, guests: Int,
  members: Int, anons: Int, seq: Option[Seq[Member]]) extends MessageForGui
private[chat] case class PostError(errorStringId: Int) extends MessageForGui

private[chat] trait GuiWorker extends Handler {
  def sendMessage(msg: MessageForGui) = super.sendMessage(obtainMessage(1, msg))
}

private[chat] trait NetworkWorker {
  protected val context: Context
  protected val gui: GuiWorker
  def ready: Boolean
  def login(): Unit
  def logout(): Unit
  def userEntered: Boolean
  def tryAutoLogin(): Unit
  def postMessage(text: String): Unit
}

private[chat] object NetworkWorker {
  def apply(context: Context, gui: GuiWorker) = works.NetWork(context, gui)
}
