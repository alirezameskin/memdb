# MemDB

In-Memory database, inspired by [go-memdb](https://github.com/hashicorp/go-memdb).

```scala

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import memdb._
import memdb.annotation.{entity, id, index}

@entity case class Person(@id id: Long, @index("name", false) name: String)
@entity case class Department(@id id: Long, name: String)

object Demo extends IOApp {

  def readRecords(db: MemDB[IO]) =
     db.readOnly { txn =>
      for {
        rows <- txn.all[Person, "name"]
        _    <- IO(println("All Persons sorted by name: (using name index)" + rows))

        rows <- txn.all[Person]
        _    <- IO(println("All Persons (using primary index): " + rows))

        u2 <- txn.first[Person, Long, "id"](10)
        _  <- IO(println(s"Person id (10): ${u2}"))

        us <- txn.range[Person, Long, "id"](5, 11)
        _  <- IO(println(s"Query by range ${us}"))

      } yield ()
    }

  def updateRecords(db: MemDB[IO]) =
    db.transaction { txn =>
      for {
        _ <- txn.upsert(Person(1, "Person 1"))
        _ <- txn.upsert(Person(10, "User 2"))
        _ <- txn.upsert(Person(5, "Alirerza"))
        _ <- txn.upsert(Department(100L, "Department 100"))
        _ <- txn.upsert(Department(1L, "Department 10"))
      } yield ()
    }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      db <- MemDB.empty[IO]
      _  <- updateRecords(db)
      _  <- readRecords(db)

    } yield ExitCode.Success
}

```


### Generated code by `@entity` macro

```scala

import memdb.annotation._
@entity case class Person(@id id: Long, @index("name", false) name: String)

//Generated code by @entity macro
object Person {
  case object IdIndex extends memdb.schema.IndexIdentifier[Person, Long]
  case object NameIndex extends memdb.schema.IndexIdentifier[Person, String]

  import memdb.schema.Index
  import memdb.schema.UniqueIndex
  import memdb.schema.NonUniqueIndex
  import memdb.schema.TableSchema
  import memdb.schema.IndexSelectorByTypeName
  import memdb.schema.IndexSelectorByName

  implicit val idIndex: UniqueIndex[Person, Long] = 
    UniqueIndex[Person, Long](IdIndex, _.id, scala.collection.immutable.TreeMap.empty[Long, Person])

  implicit val nameIndex: NonUniqueIndex[Person, String, Long] =
    NonUniqueIndex[Person, String, Long](
      NameIndex,
      _.name,
      _.id,
      scala.collection.immutable.TreeMap.empty[String, Map[Long, Person]]
    )

  implicit val schema: TableSchema[Person] =
    TableSchema[Person](idIndex, List(nameIndex))

  implicit val idIndexSelector: IndexSelectorByTypeName[Person, Long, "id"] =
    new IndexSelectorByTypeName[Person, Long, "id"] {
      override val index: Index.Aux[Person, Long] = idIndex
    }

  implicit val idIndexNameSelector: IndexSelectorByName[Person, "id"] =
    new IndexSelectorByName[Person, "id"] {
      override val index: Index[Person] = idIndex
    }

  implicit val nameIndexSelector : IndexSelectorByTypeName[Person, String, "name"] =
    new IndexSelectorByTypeName[Person, String, "name"] {
      override val index: Index.Aux[Person, String] = nameIndex
    }

  implicit val nameIndexNameSelector: IndexSelectorByName[Person, "name"] =
    new IndexSelectorByName[Person, "name"] {
      override val index: Index[Person] = nameIndex
    }
}
```