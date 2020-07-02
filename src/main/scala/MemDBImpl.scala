import cats.effect.concurrent.{Ref, Semaphore}
import cats.effect.{IO, Resource}

class MemDBImpl(private val db: Ref[IO, Database],
                private val lock: Semaphore[IO])
    extends MemDB {

  override def transaction[A](f: ReadWriteTxn => IO[A]): IO[A] = {
    Resource.make[IO, Unit](lock.acquire)(_ => lock.release).use[IO, A] { _ =>
      for {
        database <- db.get
        txn <- ReadAndWriteTxn(database)
        res <- f(txn)
        ndb <- txn.db.get
        _ <- db.set(ndb)
      } yield res
    }
  }

  override def readOnly[A](f: ReadTxn => IO[A]): IO[A] = {
    for {
      database <- db.get
      txn <- ReadAndWriteTxn(database)
      res <- f(txn)
    } yield res
  }
}
