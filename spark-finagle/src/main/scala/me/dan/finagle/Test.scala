package me.dan.finagle


import com.twitter.util.{Await, Future, Promise}

object Test extends App {

  def delayedRun(n: Int): Future[Int] = {
    Future.value({
      Thread.sleep(10000)
      n + 1
    })
  }
  println("creating a promise...")
  val promise = new Promise[Int]
  println("creating the future on the promise")
  val future = promise map(delayedRun)
  println("we'll set a value on the promise...")
  promise setValue(100)
  println("A value have already been sent and we'll setup the callback")
  future.onSuccess(n => println("result is " + n))
  println("Callback ok and now we'll wait for the future...")

  Await.ready(future)

  println("ending...")
}
