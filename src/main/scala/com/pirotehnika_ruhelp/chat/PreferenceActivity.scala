package com.pirotehnika_ruhelp.chat

import android.os.Bundle
import android.preference.{PreferenceActivity => AndroidPrefActivity}

class PreferenceActivity extends AndroidPrefActivity {
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    addPreferencesFromResource(R.xml.preferences)
  }
}
