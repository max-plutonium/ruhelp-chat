
import android.Keys._

android.Plugin.androidBuild

organization := "com.pirotehnika-ruhelp"

name := "ruhelp_chat"

version := "0.5"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "org.jsoup" % "jsoup" % "1.7.3",
  "com.google.android" % "support-v4" % "r7"
)

packageName := "com.pirotehnika_ruhelp.chat"

platformTarget := "android-14"

minSdkVersion := "7"

mergeManifests := true

typedResources := true

typedResourcesIgnores in Android ++= Seq(
  "org.holoeverywhere", "com.slidingmenu.lib", "com.actionbarsherlock"
)

proguardScala := true
