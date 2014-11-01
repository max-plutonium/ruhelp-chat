package com.pirotehnika_ruhelp.chat

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.app.Fragment
import android.view.ViewGroup.LayoutParams
import android.view.{View, LayoutInflater, ViewGroup}
import android.widget.{ImageView, TableRow, TableLayout}

class SmilesFragment extends Fragment {
  private val smilesList = collection.mutable.ArrayBuffer[Smile]()

  override final def onCreateView(inflater: LayoutInflater,
    container: ViewGroup, savedInstanceState: Bundle): View = {
    val lytRoot = new TableLayout(getActivity)
    lytRoot setLayoutParams new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    val iv = new ImageView(getActivity)
    iv.setImageDrawable(Drawable.createFromPath(getActivity.getExternalCacheDir +
      "/smiles/unsure.png"))
    val row = new TableRow(getActivity)
    row.addView(iv)
    lytRoot.addView(row)
    lytRoot
  }

  override final def onActivityCreated(savedInstanceState: Bundle) {
    super.onActivityCreated(savedInstanceState)
  }

  final def setupSmiles(smiles: Seq[Smile]) {
    smilesList.clear()
    smilesList ++= smiles
  }
}