package me.dan


/**
 * Demo of utilizing scala less verbosity
 * in coding to implement detailed
 * strongly type codes
 */
object StrongType extends App {

  /** supported types of a field */
  abstract class ColumnType(val name: String)
  final object IntColumn extends ColumnType("Int")
  final object StringColumn extends ColumnType("String")

  /** metadata of the source class */
  final case class Table(val name: String)
  final case class Field(val name: String, val columnType: ColumnType)

  /** the actual signature of a source class */
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


  /** what if we have some implicit helpers to construct the source class */
  sealed trait CreateTable {
    val tbl: Table
  }

  object DBTableSourceImplicits {
    import scala.collection.mutable.ArrayBuffer

    /** an implicit function convert automatically
        tuple of String, ColumnType to Field */
    implicit def tupleToField(kv: (String, ColumnType)) = Field(kv._1, kv._2)

    /** from a string, we can start off the object composition */
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

  /** expose the implicit functions */
  import DBTableSourceImplicits._
  /** let's create the object */
  val mySampleTDBTable = "implicit_table" << ("id", IntColumn) << ("name", StringColumn) build

  /** then trying to describe it */
  mySampleTDBTable.describe


  /** or better yet as a function */
  def createTableSource(name: String)(flds: (String, ColumnType)*) = new DBTableSource {
    val table = Table(name)
    val fields = flds.toArray.map(kv => Field(kv._1, kv._2))
  }
  /** let's create the object */
  val createdTableSource = createTableSource("functional_table")(
      "id" -> IntColumn,
      "name" -> StringColumn
    )

  /** then trying to describe it */
  createdTableSource describe

  /**
   * With this, we were able to implement detailed facilities without much of
   * verbosity in coding since scala tookout unnecessary syntax
   */


}
