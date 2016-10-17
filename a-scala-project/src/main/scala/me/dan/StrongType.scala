package me.dan



object StrongType extends App {

  abstract class ColumnType(val name: String)
  final object IntColumn extends ColumnType("Int")
  final object StringColumn extends ColumnType("String")

  final case class Table(val name: String)
  final case class Field(val name: String, val columnType: ColumnType)

  trait DBTableSource {
    val table: Table
    val fields: Array[Field]

    def describe {
      println("Table name is: " + table.name)
      println("Fields are: ")
      fields.foreach(fld => {
        println("\t" + fld.name + " of type " + fld.columnType.name)
      })
    }
  }

  /** explicit way of creating the DBTableSource */
  object MySampleDBTable extends DBTableSource {
    val table = Table("my_table")
    val fields = Array(
      Field("id", IntColumn),
      Field("value", StringColumn)
    )
  }

  /** then trying to describe it */
  MySampleDBTable.describe


  /** what if we have some implicit helpers */
  sealed trait CreateTable {
    val tbl: Table
  }

  object DBTableSourceImplicitis {
    import scala.collection.mutable.ArrayBuffer
    implicit def tupleToField(kv: (String, ColumnType)) = Field(kv._1, kv._2)

    implicit def wrapToCreateTable(s: String) = new CreateTable {
      val tbl = Table(s)
      private val flds = new ArrayBuffer[Field]()
      def <<(fld: Field): this.type = {
        flds append(fld)
        this
      }

      def build: DBTableSource = new DBTableSource {
        val table = tbl
        val fields = flds toArray
      }
    }
  }

  import DBTableSourceImplicitis._
  val mySampleTDBTable = "implicit_table" << ("id", IntColumn) << ("name", StringColumn) build

  mySampleTDBTable.describe


  /** --- or better yet --- */
  def createTableSource(name: String)(flds: (String, ColumnType)*) = new DBTableSource {
    val table = Table(name)
    val fields = flds.toArray.map(kv => Field(kv._1, kv._2))
  }

  val createdTableSource = createTableSource("functional_table")(
      "id" -> IntColumn,
      "name" -> StringColumn
    )

  createdTableSource describe

  println("Demo of StrongType")


}
