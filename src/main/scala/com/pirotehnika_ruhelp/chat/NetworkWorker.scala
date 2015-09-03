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
  def login(): Unit
  def logout(): Unit
  def userEntered: Boolean
  def tryAutoLogin(): Unit
  def postMessage(text: String): Unit
  def downloadDrawable(url: String): Future[Drawable]
  def obtainSmiles: Future[Seq[Smile]]
}

private[chat] object NetworkWorker {
  def apply(context: Context) = works.NetWork(context)
}
