package com.pirotehnika_ruhelp.chat

import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.{View, LayoutInflater, ViewGroup}

class SmilesFragment extends Fragment {
  import TypedResource._
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater inflate(TR.layout.smiles_fragment, null)
  }

  override def onActivityCreated(savedInstanceState: Bundle): Unit = {
    super.onActivityCreated(savedInstanceState)
  }

  override def onStart(): Unit = {
    super.onStart()
  }

  override def onResume(): Unit = {
    super.onResume()
  }

  override def onPause(): Unit = {
    super.onPause()
  }

  override def onStop(): Unit = {
    super.onStop()
  }

  override def onDestroyView(): Unit = {
    super.onDestroyView()
  }

  override def onDestroy(): Unit = {
    super.onDestroy()
  }

  override def onDetach(): Unit = {
    super.onDetach()
  }
}