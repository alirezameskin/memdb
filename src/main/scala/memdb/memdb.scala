package memdb

import cats.effect.concurrent.{Ref, Semaphore}
import cats.effect.{Concurrent, IO, Resource}

trait MemDB {
  def readOnly[A](f: ReadTxn => IO[A]): IO[A]
  def transaction[A](f: WriteTxn with ReadTxn => IO[A]): IO[A]
}

object MemDB {

  def apply()(implicit C: Concurrent[IO]): IO[MemDB] =
    for {
      sem <- Semaphore[IO](1)
      db  <- Ref.of(Database.empty)
    } yield new MemDBImpl(db, sem)
}

class MemDBImpl(private val dbRef: Ref[IO, Database], private val lock: Semaphore[IO]) extends MemDB {

  override def transaction[A](f: WriteTxn with ReadTxn => IO[A]): IO[A] =
    Resource.make[IO, Unit](lock.acquire)(_ => lock.release).use[IO, A] { _ =>
      for {
        db  <- dbRef.get
        txn <- ReadAndWriteTxn(db)
        res <- f(txn)
        db  <- txn.dbRef.get
        _   <- dbRef.set(db)
      } yield res
    }

  override def readOnly[A](f: ReadTxn => IO[A]): IO[A] =
    for {
      db  <- dbRef.get
      txn <- ReadAndWriteTxn(db)
      res <- f(txn)
    } yield res
}
