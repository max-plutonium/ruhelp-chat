package com.pirotehnika_ruhelp.chat

import android.os.Bundle
import android.app.Fragment
import android.view.{View, LayoutInflater, ViewGroup}

class SmilesFragment extends Fragment {
  import TypedResource._
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater inflate(TR.layout.smiles_fragment, null)
  }

  override def onActivityCreated(savedInstanceState: Bundle): Unit = {
    super.onActivityCreated(savedInstanceState)
  }
}