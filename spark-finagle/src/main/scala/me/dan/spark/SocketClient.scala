package me.dan.spark

import java.io._

import java.net.{ InetAddress, ServerSocket, Socket, SocketException }
import java.util.Random

object SocketClient extends App {

  try {
    
    val ia = InetAddress.getLocalHost
    val socket = new Socket(ia, 9999)
    val out = new ObjectOutputStream(
      new DataOutputStream(socket.getOutputStream()))
    val in = new DataInputStream(socket.getInputStream())

    while (true) {
      out.write("the quick brown fox jumps over the lazy dog".getBytes)
      out.flush()
      println("wrote to socket...")
      Thread.sleep(5000)
    }

    out.close()
    in.close()
    socket.close()
  } catch {
    case e: IOException =>
      e.printStackTrace()
  }

}