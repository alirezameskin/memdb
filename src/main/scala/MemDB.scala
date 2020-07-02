import cats.effect.concurrent.{Ref, Semaphore}
import cats.effect.{Concurrent, IO}

trait MemDB {
  def readOnly[A](f: ReadTxn => IO[A]): IO[A]
  def transaction[A](f: ReadWriteTxn => IO[A]): IO[A]
}

object MemDB {

  def apply()(implicit C: Concurrent[IO]): IO[MemDB] = {
    for {
      sem <- Semaphore[IO](1)
      db <- Ref.of(Database.empty)
    } yield new MemDBImpl(db, sem)
  }
}
