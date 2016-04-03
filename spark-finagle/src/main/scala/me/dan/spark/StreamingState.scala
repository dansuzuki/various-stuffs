package me.dan.spark

import org.apache.spark.SparkConf

import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import akka.actor.Actor
import org.apache.spark.streaming.receiver.ActorHelper
import akka.actor.Props
import org.apache.spark.SparkEnv
import org.apache.spark.rdd.PairRDDFunctions
import org.apache.spark.rdd.EmptyRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

class CustomActor extends Actor with ActorHelper {
  def receive = {
    case data: String => {
      //println(">>> received a message: \"" + data + "\"")
      store(data)

    }
  }
}

object CustomActor {
  val name: String = "CustomActor"
}

object StreamingState extends App {

  val sparkConf = new SparkConf()
    .setAppName("Streaming State")
    .setMaster("local[*]")
    .set("spark.akka.logLifecycleEvents", "true")
    .set("spark.logConf", "true")
  val checkpointPath = "./tmp/" + this.getClass.getCanonicalName
  def createStreamingContext(): StreamingContext = new StreamingContext(sparkConf, Seconds(10))

  // entry point here
  run()

  def run() {

    val ssc = StreamingContext.getOrCreate(checkpointPath, createStreamingContext)
    ssc.sparkContext.setCheckpointDir(checkpointPath + "/for_rdd")

    var storeRDD = ssc.sparkContext.emptyRDD[(String, Int)].map(e => e).persist(StorageLevel.MEMORY_AND_DISK_SER_2)
    val lines = ssc.actorStream[String](Props[CustomActor], CustomActor.name)
    val words = lines.flatMap(_.split(" "))
    val pairs = words.map(word => (DateOps.timeInHHmm(), 1)) //.window(Seconds(300))
    val wordsPerMinute = pairs.reduceByKey(_ + _)

    wordsPerMinute.transform(batchRDD => {
      /** dispose previous data */
      storeRDD.unpersist(false)

      /** reference minute */
      val startHHmm = DateOps.pastHHmm(3)
      
      storeRDD = batchRDD
        /** join with the delta data */
        .fullOuterJoin(storeRDD)
        /** update state goes here */
        .mapValues(vv => vv._1.getOrElse(0) + vv._2.getOrElse(0))
        /** window filtering goes here */
        .filter(_._1.compareTo(startHHmm) > 0)

      /** now persist */
      storeRDD.persist(StorageLevel.MEMORY_AND_DISK_SER_2)
      storeRDD.checkpoint()
      storeRDD
    }).print(10)

    //wordsPerMinute.print(10)
    /*
    val updated = wordCounts.updateStateByKey(updateFunc).window(Seconds(300)) // 5 minutes
    updated.print(20)
		*/
    ssc.start()
    ActorSender(SparkEnv.get.actorSystem, ssc.sparkContext.getConf)
    ssc.stop(true, true)

  }
  def updateFunc(newValues: Seq[Int], state: Option[Int]): Option[Int] = {
    Some(newValues.sum + state.getOrElse(0))
  }
}