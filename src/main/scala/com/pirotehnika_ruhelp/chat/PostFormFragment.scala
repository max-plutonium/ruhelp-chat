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

import android.os.Bundle
import android.text.{Editable, TextWatcher}
import android.view.{LayoutInflater, ViewGroup, View}
import android.widget.Toast

class PostFormFragment extends android.support.v4.app.Fragment {
  import TypedResource._
  import implicits.ListenerBuilders._
  private lazy val btnSmiles = getView findView TR.btnSmiles
  private lazy val tvMessage = getView findView TR.tvMessage
  private lazy val btnPost = getView findView TR.btnPost
  private var smilesShown = false

  var onSmilesCallback: Option[Boolean => Unit] = None
  var postMessageCallback: Option[String => Unit] = None

  override final def onCreateView(inflater: LayoutInflater,
    container: ViewGroup, savedInstanceState: Bundle): View =
    inflater inflate(TR.layout.postform_fragment, null)

  override final def onActivityCreated(savedInstanceState: Bundle) {
    btnSmiles setOnClickListener((v: View) => toggleSmilesState())

    tvMessage addTextChangedListener new TextWatcher {
      override def beforeTextChanged(s: CharSequence,
        start: Int, count: Int, after: Int) { }

      override def onTextChanged(s: CharSequence,
        start: Int, before: Int, count: Int) { }

      override def afterTextChanged(s: Editable) {
        if(s.toString.trim.isEmpty) {
          btnPost setEnabled false
          btnPost setBackgroundResource R.drawable.ic_send_grey600_36dp
        } else {
          btnPost setEnabled true
          btnPost setBackgroundResource R.drawable.ic_send_white_36dp
        }
      }
    }

    btnPost setOnClickListener { (v: View) =>
        val text = tvMessage.getText.toString
        if (!text.trim.isEmpty) {
          onSendMessage()
          postMessageCallback foreach(_(text))
        }
      }

    super.onActivityCreated(savedInstanceState)
  }

  private final def toggleSmilesState() {
    smilesShown = !smilesShown
    btnSmiles setBackgroundResource {
      if(smilesShown) R.drawable.ic_check_white_36dp
      else R.drawable.ic_mood_white_36dp
    }
    onSmilesCallback foreach(_(smilesShown))
  }

  final def setSmilesStateHidden() = if(smilesShown) toggleSmilesState()

  final def onUserEnter() {
    btnSmiles setEnabled true
    btnSmiles setBackgroundResource R.drawable.ic_mood_white_36dp
    tvMessage setEnabled true
    if(!tvMessage.getText.toString.trim.isEmpty) {
      btnPost setEnabled true
      btnPost setBackgroundResource R.drawable.ic_send_white_36dp
    }
  }

  final def onUserExit() {
    setSmilesStateHidden()
    btnSmiles setEnabled false
    btnSmiles setBackgroundResource R.drawable.ic_mood_grey600_36dp
    tvMessage setEnabled false
    btnPost setEnabled false
    btnPost setBackgroundResource R.drawable.ic_send_grey600_36dp
  }

  private final def onSendMessage() {
    setSmilesStateHidden()
    btnSmiles setEnabled false
    btnSmiles setBackgroundResource R.drawable.ic_mood_grey600_36dp
    tvMessage setEnabled false
    btnPost setEnabled false
    btnPost setBackgroundResource R.drawable.ic_send_grey600_36dp
  }

  final def onMessagePosted() {
    btnSmiles setEnabled true
    btnSmiles setBackgroundResource R.drawable.ic_mood_white_36dp
    tvMessage setEnabled true
    tvMessage setText ""
  }

  final def onPostError(errorId: Int) {
    btnSmiles setEnabled true
    btnSmiles setBackgroundResource R.drawable.ic_mood_white_36dp
    tvMessage setEnabled true
    btnPost setEnabled true
    btnPost setBackgroundResource R.drawable.ic_send_white_36dp
    Toast makeText(getActivity, errorId, Toast.LENGTH_LONG) show()
  }

  final def appendText(text: String) {
    val curText = tvMessage.getText.toString
    val textBefore = curText substring(0, tvMessage.getSelectionStart)
    val textAfter = curText substring tvMessage.getSelectionStart

    if(textBefore.isEmpty) {
      tvMessage setText s"$text $textAfter"
      tvMessage setSelection text.length + 1
    } else {
      tvMessage setText s"$textBefore $text $textAfter"
      tvMessage setSelection textBefore.length + text.length + 2
    }

    tvMessage setFocusable true
  }
}
