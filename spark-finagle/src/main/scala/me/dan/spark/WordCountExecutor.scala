package me.dan.spark


import collection.mutable.{HashMap, SynchronizedMap}

import com.twitter.util.{Await, Future, FuturePool, Promise}

import java.util.concurrent.{Future => JFuture, _}

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.{PairRDDFunctions, RDD}



object WordCountExecutor {

  lazy val executor = {
    val execs = Executors.newFixedThreadPool(8)
    sys.addShutdownHook({
      execs.shutdown
    })
    execs
  }
  lazy val pool = FuturePool(executor)

  case class Content(id: Int, content: String)
  abstract class Status(val name: String)
  case object Failed extends Status("failed")
  case object Invalid extends Status("invalid")
  case object Running extends Status("running")
  case object Successful extends Status("successful")

  private var counter: Int = 0

  lazy val sc: SparkContext = {
    val conf = new SparkConf
    conf.setAppName("WordCounter").setMaster("local[*]")
    new SparkContext(conf)
  }

  private val statuses = new HashMap[Int, Status] with SynchronizedMap[Int, Status] { }


  def clear(id: Int) { statuses remove(id) }

  def close {
    sc.stop
  }

  def id: Int = synchronized {
    counter = counter + 1
    counter
  }

  def newContent(content: Content) {
    import java.io.{BufferedWriter, File, FileWriter}
    val file = new File("/tmp/word_count/input/in_" + content.id + ".txt")
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(content.content)
    bw.close()
  }


  /** FIXME: use native instead */
  def output(id: Int) =
    sc.textFile("/tmp/word_count/out_" + id).collect.toList



  def process(jobId: Int, contentIds: List[Int]) {
    statuses(jobId) = Running
    val promise = new Promise[(Int, List[Int])]
    val future = pool { promise map(kv => run(kv._1, kv._2)) }
    future.onSuccess { n => statuses(jobId) = Successful }
    future.onFailure { case e: Exception => statuses(jobId) = Failed }
    promise setValue((jobId, contentIds))
  }

  def run(jobId: Int, contentIds: List[Int]) {
    Future.value({
      val rdd: RDD[String] = contentIds map(id => sc.textFile("/tmp/word_count/in_" + id + ".txt")) reduce(_ union _)
      rdd
        .flatMap(_.split("\\s+"))
        .map((_, 1L))
        .reduceByKey(_ + _)
        .map(kv => kv._1 + "|" + kv._2)
        .saveAsTextFile("/tmp/word_count/out_" + id)
    })
  }

  def status(id: Int) = statuses getOrElse(id, Invalid)

}
