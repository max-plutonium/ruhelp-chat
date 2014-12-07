package com.pirotehnika_ruhelp.chat

import java.util.concurrent.Executor

import android.app.Activity
import android.os.{HandlerThread, Handler}

import scala.concurrent.ExecutionContext

object Chat {
  var instance: Activity = _
  var handler: GuiWorker = _
  var networker: NetworkWorker = _
  var networkHandler: Handler = _
  private val networkThread = new HandlerThread("Network Thread") {
    override def onLooperPrepared() {
      super.onLooperPrepared()
      networkHandler = new Handler(getLooper)
    }
  }

  networkThread.start()

  implicit val gui: ExecutionContext = ExecutionContext fromExecutor new Executor {
    override def execute(runnable: Runnable) = handler.post(runnable)
  }
}
