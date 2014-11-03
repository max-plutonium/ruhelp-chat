package com.pirotehnika_ruhelp.chat

import android.os.Bundle
import android.app.Fragment
import android.view.ViewGroup.LayoutParams
import android.view.{View, LayoutInflater, ViewGroup}
import android.widget.{AdapterView, LinearLayout}

class SmilesFragment extends Fragment {
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
