package me.dan

import org.apache.spark._

object AScalaObject extends App {

	println("Hello there runner...")

	val builder = AnAvroSchema.newBuilder()


	val avroObject = builder
		.setColumn1("hello there")
		.setColumn2(1).build;

	println(avroObject.toString)


	val conf = new SparkConf()
	conf.setAppName(this.getClass.getCanonicalName)
		.setMaster("local[*]")

	val sc = new SparkContext(conf)

	val l = (1 to 1000)
	val sum = sc.parallelize(l).reduce(_+_)

	println(sum)
	sc.stop


}
