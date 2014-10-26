package com.pirotehnika_ruhelp.chat

import android.app.{Activity, Fragment}
import android.os.Bundle
import android.view.{View, LayoutInflater, ViewGroup}

class SmilesFragment extends Fragment {
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val ret = inflater inflate(R.layout.smiles_fragment, null)
    ret.setMinimumHeight(100)
    ret.setMinimumWidth(100)
    ret
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