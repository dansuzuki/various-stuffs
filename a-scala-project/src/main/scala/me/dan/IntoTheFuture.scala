package me.dan

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

import ExecutionContext.Implicits.global

object IntoTheFuture extends App {

  var counter: Int = 0

  println("Welcome to the Future")

  def doLongRunningStuff {
    synchronized {
      /**
        * we're doing this because other Threads
        * will access/update counter variable as well
        */
      println("Counter = " + counter)
      counter = counter + 1
    }
    Thread.sleep(2000)
    println("I'm done.")
  }

  // here we execute all futures
  val futures = (1 to 10) map(i => Future(doLongRunningStuff))

  // here we wait for it one-by-one the manual way
  // Await.ready(<the future>, <timeout, here we wait indefinitely>)
  futures foreach(f => Await.ready(f, Duration.Inf))

  println("All futures are already ok.")


  /**
   * what if we don't want to use Await.ready thing?
   * We can use a countdown latch and assign a callback
   * for each future to do the actual countdown
   */

  import java.util.concurrent.CountDownLatch // yes this is from Java
  val countDownLatch = new CountDownLatch(10) // 10 is the number of futures

  // here we fire the futures with callback
  val futureWithCallback = (1 to 10) map(i => {
    // we don't need th synchronized thing here because CountDownLatch class handles that for us
    Future(doLongRunningStuff) onComplete(u => countDownLatch.countDown())
    // btw, there is onSuccess and onFailure too
  })

  // no we wait until countDownLatch become 0
  countDownLatch.await()

  /**
   * so what's the difference of using Await.ready one by one and CountDownLatch?
   * In real world, every future completes at different time so using Await.ready
   * will block at different interval, so not a good choice if you need to know if
   * one future already finishes. Plus, semantically, its better to maximise the
   * callback facilities to do checkpoints and/or coordination.
   */
}
