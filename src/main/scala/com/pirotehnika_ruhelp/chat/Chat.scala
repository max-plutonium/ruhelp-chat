package com.pirotehnika_ruhelp.chat

import android.os.Handler
import java.util.concurrent.Executor

import scala.concurrent.ExecutionContext

class Chat extends android.app.Application {
  import Chat._

  override final def onCreate(): Unit = {
    super.onCreate()
    instance = this
    handler = new Handler()
  }
}

object Chat {
  import java.util.concurrent.{Executors => JExecutors}
  var instance: Chat = _
  var handler: Handler = _
  val networkSched = JExecutors newSingleThreadScheduledExecutor()
  val network: ExecutionContext = ExecutionContext fromExecutor new Executor {
    override def execute(command: Runnable) = networkSched execute command
  }
  val gui: ExecutionContext = ExecutionContext fromExecutor new Executor {
    override def execute(runnable: Runnable) = handler.post(runnable)
  }
}
