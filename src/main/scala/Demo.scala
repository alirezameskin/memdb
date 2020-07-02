import cats.Traverse
import cats.effect.{ExitCode, IO, IOApp, Timer}
import cats.implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Demo extends IOApp {

  sealed trait BaseTable

  case class Person(@id id: Long, @index("name") name: String) extends BaseTable

  case class Department(name: String) extends BaseTable

  object Person {
    implicit val schema: TableSchema[Person] = new TableSchema[Person] {
      override def name: String = "persons"
    }
  }

  // Needed for getting a Concurrent[IO] instance
  implicit val ctx = IO.contextShift(ExecutionContext.global)
  // Needed for `sleep`

  def readonly(db: MemDB)(implicit timer: Timer[IO]) =
    timer.sleep(4.seconds) *> db.readOnly { x =>
      for {
        recs <- x.all[Person]
        _ <- IO(println(recs))
      } yield ()

    }

  def program(db: MemDB, num: Int)(implicit timer: Timer[IO]): IO[String] =
    db.transaction { txn =>
      for {
        _ <- timer.sleep(2.seconds)
        _ <- txn.insert(Person(10, s"Person ${num}"))
        _ <- txn.insert(Person(20, s"Alireza ${num}"))
        _ <- if (num == 4) IO.raiseError(new Exception("dd")) else IO.pure()
      } yield {

        Thread.currentThread().getName
      }
    }

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      memdb <- MemDB()
      t1 <- program(memdb, 1).start
      t2 <- program(memdb, 2).start
      t3 <- program(memdb, 4).start
      t4 <- readonly(memdb).start

      v <- Traverse[List].traverse(List(t1, t2, t3, t4))(_.join.attempt)
      _ <- IO(println(v))
    } yield ExitCode.Success
  }
}
