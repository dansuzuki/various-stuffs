package me.dan.finagle


import com.twitter.util.{Await, Future, FuturePool, Promise}
import java.util.concurrent.{Future => JFuture, _}


object Test extends App {
  val executor = Executors.newFixedThreadPool(8)
  val pool = FuturePool(executor)

  def delayedRun(n: Int): Int = {
    Thread.sleep(10000)
    n + 1
  }

  println("creating a promise...")
  val promise = new Promise[Int]
  println("creating the future on the promise")
  val future = pool { promise map(delayedRun) }
  println("future = " + future.toString)
  println("we'll set a value on the promise...")
  promise setValue(100)
  println("A value have already been sent and we'll setup the callback")
  future.onSuccess(n => println("result is " + n))
  println("Callback ok and now we'll wait for the future...")

  Await.ready(future)
  Thread.sleep(1)
  println("ending... shutting down executor service")
  executor.shutdown()
  System.exit(0)
}
