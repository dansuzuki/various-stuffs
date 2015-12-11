package me.dan.sys

import scala.sys.process._
import com.twitter.util.Future


object ProcessSpawner {
  def apply(cmd:String) : String = {
    val proc = Process(cmd)
    proc.lines.toList.mkString("\n")
  }
}