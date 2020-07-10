# MemDB

In-Memory database, inspired by [go-memdb](https://github.com/hashicorp/go-memdb).

```scala

import cats.Traverse
import cats.effect.{ExitCode, IO, IOApp, Timer}
import cats.implicits._
import memdb._
import memdb.annotation.{entity, id, index}

import scala.concurrent.duration._

@entity case class Person(@id id: Long, @index("name", false) name: String)
@entity case class Department(@id id: Long, name: String)

object Demo extends IOApp {

  def readRecords(db: MemDB[IO])(implicit timer: Timer[IO]) =
    timer.sleep(1.seconds) *> db.readOnly { txn =>
      for {
        rows <- txn.all[Person, "name"]
        _    <- IO(println("All Persons sorted by name: (using name index)" + rows))

        rows <- txn.all[Person]
        _    <- IO(println("All Persons (using primary index): " + rows))

        u2 <- txn.first[Person, Long, "id"](10)
        _  <- IO(println(s"Person id (10): ${u2}"))

        users <- txn.range[Person, Long, "id"](5, 11)
        _     <- IO(println(s"Query by range ${users}"))

      } yield ()
    }

  def updateRecords(db: MemDB[IO])(implicit timer: Timer[IO]) =
    db.transaction { txn =>
      for {
        _ <- txn.upsert(Person(1, "Person 1"))
        _ <- txn.upsert(Person(10, "User 2"))
        _ <- txn.upsert(Person(5, "A User"))
        _ <- txn.upsert(Department(100L, "Department 100"))
        _ <- txn.upsert(Department(1L, "Department 10"))
      } yield ()
    }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      memdb <- MemDB[IO]()
      f1    <- updateRecords(memdb).start
      f2    <- readRecords(memdb).start

      _ <- Traverse[List].traverse(List(f1, f2))(_.join)
    } yield ExitCode.Success
}

```


### Generated code by `@entity` macro

```scala

import memdb.annotation._
@entity case class Person(@id id: Long, @index("name", false) name: String)

//Generated code by @entity macro
object Person {
  case object IdIndex extends memdb.IndexIdentifier
  case object NameIndex extends memdb.IndexIdentifier

  implicit val idIndex: memdb.UniqueIndex[Person, Long] = 
    memdb.UniqueIndex[Person, Long](IdIndex, _.id, scala.collection.immutable.TreeMap.empty[Long, Person])

  implicit val nameIndex: memdb.NonUniqueIndex[Person, String, Long] =
    memdb.NonUniqueIndex[Person, String, Long](
      NameIndex,
      _.name,
      _.id,
      scala.collection.immutable.TreeMap.empty[String, Map[Long, Person]]
    )

  implicit val schema: memdb.TableSchema[Person] =
    memdb.TableSchema[Person](idIndex, List(nameIndex))

  implicit val idIndexSelector: memdb.IndexSelectorByTypeName[Person, Long, "id"] =
    new memdb.IndexSelectorByTypeName[Person, Long, "id"] {
      override val index: memdb.Index.Aux[Person, Long] = idIndex
    }

  implicit val idIndexNameSelector: memdb.IndexSelectorByName[Person, "id"] =
    new memdb.IndexSelectorByName[Person, "id"] {
      override val index: memdb.Index[Person] = idIndex
    }

  implicit val nameIndexSelector : memdb.IndexSelectorByTypeName[Person, String, "name"] =
    new memdb.IndexSelectorByTypeName[Person, String, "name"] {
      override val index: memdb.Index.Aux[Person, String] = nameIndex
    }

  implicit val nameIndexNameSelector: memdb.IndexSelectorByName[Person, "name"] =
    new memdb.IndexSelectorByName[Person, "name"] {
      override val index: memdb.Index[Person] = nameIndex
    }
}
```