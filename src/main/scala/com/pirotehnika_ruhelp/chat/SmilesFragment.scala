package com.pirotehnika_ruhelp.chat

import android.os.Bundle
import android.app.Fragment
import android.view.{View, LayoutInflater, ViewGroup}

class SmilesFragment extends Fragment {
  import TypedResource._

  override final def onCreateView(inflater: LayoutInflater,
    container: ViewGroup, savedInstanceState: Bundle): View =
    inflater inflate(TR.layout.smiles_fragment, null)

  override final def onActivityCreated(savedInstanceState: Bundle): Unit = {
    super.onActivityCreated(savedInstanceState)
  }
}