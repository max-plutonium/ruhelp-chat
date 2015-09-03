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
import android.view.ViewGroup.LayoutParams
import android.view.{View, LayoutInflater, ViewGroup}
import android.widget.{AdapterView, LinearLayout}

class SmilesFragment extends android.support.v4.app.Fragment {
  import TypedResource._
  import implicits.ListenerBuilders._
  private val smilesBuffer = collection.mutable.ArrayBuffer[Smile]()
  private lazy val gridAdapter = new SmilesAdapter(getActivity, smilesBuffer)
  private lazy val lytSmiles = getActivity findView TR.lytSmiles
  private val visibleParams = new LinearLayout.LayoutParams(
    LayoutParams.MATCH_PARENT, 300)
  private val nonVisibleParams = new LinearLayout.LayoutParams(
    LayoutParams.MATCH_PARENT, 0)

  var onSmileSelectedCallback: Option[String => Unit] = None
  var onHideCallback: Option[() => Unit] = None

  override final def onCreateView(inflater: LayoutInflater,
    container: ViewGroup, savedInstanceState: Bundle): View =
    inflater inflate(TR.layout.smiles_fragment, null)

  override final def onActivityCreated(savedInstanceState: Bundle) {
    val gvSmiles = getView findView TR.gvSmiles
    gvSmiles setAdapter gridAdapter
    gvSmiles setOnItemClickListener { (parent: AdapterView[_],
      view: View, position: Int, id: Long) =>
        onSmileSelectedCallback foreach(_(smilesBuffer(position).code))
      }
    super.onActivityCreated(savedInstanceState)
  }

  override final def onStart() {
    lytSmiles.setLayoutParams(visibleParams)
    super.onStart()
  }

  override final def onStop() {
    lytSmiles.setLayoutParams(nonVisibleParams)
    onHideCallback foreach(_())
    super.onStop()
  }

  final def setupSmiles(smiles: Seq[Smile]) {
    smilesBuffer.clear()
    smilesBuffer ++= smiles
    if(isVisible)
      gridAdapter notifyDataSetChanged()
  }
}
