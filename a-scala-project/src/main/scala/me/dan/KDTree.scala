package me.dan

//import me.dan.scala.Geo

import scala.math.BigDecimal
import scala.reflect.ClassTag
import scala.util.Try
import sext._

object KDTree {
  abstract class Dimension
  class XDimension extends Dimension
  class YDimension extends Dimension

  val XDim = new XDimension
  val YDim = new YDimension

  abstract class Bias
  object NE extends Bias // north east
  object SE extends Bias // south east
  object NW extends Bias // north west
  object SW extends Bias // south west

  type DD = (Double, Double)

  // lower left, upper right
  case class Box(ll: DD,  ur: DD) {
    val lr: DD = (ur._1, ll._2)
    val ul: DD = (ll._1, ur._2)
    val distance: Double = Geo.haversine(ll._2, ll._1, ur._2, ur._1)
  }

  case class Node(
    box: Box,
    left: Option[Node],
    right: Option[Node],
    points: Option[List[DD]]
  )

  final val emptyListDD = List[DD]()

  /**
   * possible points under a square selection
   * DD = (lat, long) OR (y, x)
   */
  def pointsUnderSelection(n: Node)(sw: DD, ne: DD): List[DD] = {
    def intersect(b: Box): Boolean =
      Geo.intersect(sw._1, sw._2, ne._1, ne._2, b.ll._2, b.ll._1, b.ur._2, b.ur._1)

    /** check left and right regions */
    def checkLeftAndRight(n: Node, sw: DD, ne: DD): List[DD] = {
      val left = n.left.get
      val right = n.right.get

      val fromLeft =
        if(intersect(left.box)) traverse(left, sw, ne)
        else emptyListDD

      val fromRight =
        if(intersect(right.box)) traverse(right, sw, ne)
        else emptyListDD

      fromLeft ++ fromRight
    }

    /** traverse via recursive to the next point */
    def traverse(n: Node, sw: DD, ne: DD): List[DD] = {
      if(n.points != None) n.points.get
      else checkLeftAndRight(n, sw, ne)
    }

    /** actual entry point is here */
    if(!intersect(n.box)) emptyListDD
    else checkLeftAndRight(n, sw, ne)
  }

  /**
   * x = longitude
   * y = latitude
   * ((minx, miny), (maxx, maxy))
   */
  final val minMaxCoord = ((180.0, 90.0), (-180.0, -90.0))

  /**
   * entry point
   */
  def apply(list: List[DD]): Node = {
      val sz = list.size
      val dim: Dimension = XDim
      val boxCoord = minMaxXY(list)
      newNode(dim, boxCoord, sz, 10000, list)
  }

  /**
   * returns minimum (x, y), maximum (x, y)
   */
  def minMaxXY(list: List[DD]): (DD, DD) = {
    list.foldLeft(minMaxCoord)((a, c) => {
      val (minx, miny) = a._1
      val (maxx, maxy) = a._2

      val newMinX = if(c._1 < minx) c._1 else minx
      val newMinY = if(c._2 < miny) c._2 else miny
      val newMaxX = if(c._1 > maxx) c._1 else maxx
      val newMaxY = if(c._2 > maxy) c._2 else maxy
      ((newMinX, newMinY), (newMaxX, newMaxY))
    })
  }

  /**
   * functions to do depending on the dimension being used
   */
  def atXorY[T: ClassTag](atX: => T, atY: => T)(dim: Dimension) : T = {
    dim match {
      case _: XDimension => atX
      case _: YDimension => atY
    }
  }

  def swapDim = atXorY(YDim, XDim) _

  /**
   * just a helper function to return Some(T) on true, None on false
   */
  def ifElseNone[T: ClassTag](cond: Boolean)(ret: => T): Option[T] = {
    if(cond) Some(ret)
    else None
  }

  /**
   * recursive entry
   */
  def newNode(dim: Dimension,
    boxCoord: (DD, DD),
    sz: Int,
    max: Int,
    ldd: List[DD]): Node = {

    val sorted = ldd.sortBy(dd => atXorY(dd._1, dd._2)(dim))
    val box = Box(boxCoord._1, boxCoord._2)
    val asParent: Boolean = if(box.distance > max && sz > 4) true else false
    def ifParent[T: ClassTag](ret: => T) = ifElseNone[T](asParent)(ret)

    val leftSize = if((sz / 2) > 0) sz / 2 else 1
    val rightSize = sz - leftSize

    val l: List[DD] = sorted.take(leftSize)
    val r: List[DD] = sorted.takeRight(rightSize)

    // left's upper right point
    val lur: Option[DD] = ifParent[DD]({
      atXorY[DD](
        { (l.last._1, boxCoord._2._1) },
        { (boxCoord._2._1, l.last._2) }
      )(dim)
    })

    // rights's lower left point
    val rll = ifParent {
      atXorY(
        (l.last._1, boxCoord._1._2),
        (boxCoord._1._1, l.last._2)
      )(dim)
    }

    val lbox: Option[(DD, DD)] = ifParent { (boxCoord._1, lur.get) }
    val rbox: Option[(DD, DD)] = ifParent { (rll.get, boxCoord._2) }

    val nextDim = swapDim(dim)

    Node(box,
      ifParent { newNode(nextDim, lbox.get, leftSize, max, l)},
      ifParent { newNode(nextDim, rbox.get, rightSize, max, r)},
      if(!asParent) Some(ldd) else None
    )
  }



  @throws(classOf[Exception])
  def main(args: Array[String]) {
    val toDbl = (d: String) => Try[Double](d.toDouble).getOrElse(0.0)

    val stream = this.getClass.getResourceAsStream("/philippine_random_points.txt")
    val lines = scala.io.Source.fromInputStream( stream ).getLines

    val points = lines
      .map(l => l.split(","))
      .map(f => (toDbl(f(3)), toDbl(f(1))))
      .filter(_ match {
        case (0.0, 0.0) => false
        case _ => true
      }).toList
    points.take(5).foreach(println)

    val tree = KDTree(points)

    val pointA = (args(0).toDouble, args(1).toDouble)
    val dist = args(2).toDouble

    val selCircle = Geo.Circle(pointA._1, pointA._2, dist)
    println(selCircle)
    val doesIntersect = Geo.intersect(selCircle) _
    val selSquare = Geo.selectGrid(pointA._1, pointA._2)(dist)
    println(selSquare)

    println("kdtree approach...")
    val pts = pointsUnderSelection(tree)(selSquare.sw, selSquare.ne)
    println("false positives...")
    pts.foreach(println)
    println("----")
    pts.foreach(kv => {
      val dist = Geo.haversine(kv._2, kv._1, selCircle.lat, selCircle.long)
      println(dist + " < " + selCircle.radius + " = " + (dist < selCircle.radius))
    })
    println("actual...")
    pts.filter(xy => doesIntersect(xy._2, xy._1)).foreach(xy => println((xy._2, xy._1)))


    println("heuristic approach...")
    points.filter(xy => doesIntersect(xy._2, xy._1)).foreach(xy => println((xy._2, xy._1)))
  }


}
