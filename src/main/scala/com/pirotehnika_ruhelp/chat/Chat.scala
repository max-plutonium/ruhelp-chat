package com.pirotehnika_ruhelp.chat

import java.util.concurrent.Executor

import android.app.Activity

import scala.concurrent.ExecutionContext

object Chat {
  import java.util.concurrent.{Executors => JExecutors}
  var instance: Activity = _
  var handler: GuiWorker = _
  var networker: NetworkWorker = _
  val networkSched = JExecutors newSingleThreadScheduledExecutor()

  implicit val network: ExecutionContext = ExecutionContext fromExecutor new Executor {
    override def execute(command: Runnable) = networkSched execute command
  }

  implicit val gui: ExecutionContext = ExecutionContext fromExecutor new Executor {
    override def execute(runnable: Runnable) = handler.post(runnable)
  }
}
