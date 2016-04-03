package me.dan.spark

import org.apache.spark.SparkEnv
import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import org.apache.spark.SparkConf
import scala.concurrent.Await


/**
 * Warning!!! This is used for testing only.
 */
object ActorSender {

  
  private var _stop:Boolean = false
  
  def isStopped:Boolean = this.synchronized({ this._stop })
  def stop = this.synchronized({this._stop = true})
  
  
  implicit val ec = ExecutionContext.global
  
  def apply(system: ActorSystem, conf: SparkConf) {
    val actorReceiver = system.actorSelection("akka://sparkDriver/user/Supervisor0/CustomActor")
    println("Now ready to send messages!!!")
    while (!isStopped) {
      actorReceiver ! "the quick brown fox jumps over the lazy dog"
      Thread.sleep(500)
    }
    readLine
    stop
  }

}