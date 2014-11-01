package com.pirotehnika_ruhelp.chat

import android.os.Bundle
import android.app.Fragment
import android.view.{LayoutInflater, ViewGroup, View}
import android.widget.{Toast, TextView}

class PostFormFragment extends Fragment {
  import TypedResource._
  import implicits.ListenerBuilders._
  private lazy val btnSmiles = getView findView TR.btnSmiles
  private lazy val tvMessage = getView findView TR.tvMessage
  private lazy val btnPost = getView findView TR.btnPost

  var onSmilesCallback: Option[() => Unit] = None
  var postMessageCallback: Option[String => Unit] = None

  override final def onCreateView(inflater: LayoutInflater,
    container: ViewGroup, savedInstanceState: Bundle): View =
    inflater inflate(TR.layout.postform_fragment, null)

  override final def onActivityCreated(savedInstanceState: Bundle) {
    btnSmiles setOnClickListener((v: View) => onSmilesCallback foreach(_()))
    btnPost setOnClickListener { (v: View) =>
        val text = tvMessage.getText.toString
        if (!text.trim.isEmpty) {
          v setEnabled false
          postMessageCallback foreach(_(text))
        }
      }
    super.onActivityCreated(savedInstanceState)
  }

  final def onUserEnter() = btnPost setEnabled true
  final def onUserExit() = btnPost setEnabled false

  final def onPostMessage() {
    btnPost setEnabled true
    tvMessage.setText("", TextView.BufferType.NORMAL)
  }

  final def onPostError(errorId: Int) {
    btnPost setEnabled true
    Toast makeText(getActivity, errorId, Toast.LENGTH_LONG) show()
  }

  final def appendText(text: String) = tvMessage append text
}
