package memdb

import cats.Monad
import cats.effect.concurrent.{Ref, Semaphore}
import cats.effect.{Concurrent, Resource, Sync}
import cats.implicits._

trait MemDB[F[_]] {
  def readOnly[A](f: ReadTxn[F] => F[A]): F[A]
  def transaction[A](f: WriteTxn[F] with ReadTxn[F] => F[A]): F[A]
}

object MemDB {

  def apply[F[_]: Monad]()(implicit C: Concurrent[F]): F[MemDB[F]] =
    for {
      sem <- Semaphore[F](1)
      db  <- Ref.of[F, Database](Database.empty)
    } yield new MemDBImpl(db, sem)
}

class MemDBImpl[F[_]: Monad: Sync](private val dbRef: Ref[F, Database], private val lock: Semaphore[F])
    extends MemDB[F] {

  override def transaction[A](f: WriteTxn[F] with ReadTxn[F] => F[A]): F[A] =
    Resource.make[F, Unit](lock.acquire)(_ => lock.release).use[F, A] { _ =>
      for {
        db  <- dbRef.get
        txn <- ReadAndWriteTxn(db)
        res <- f(txn)
        db  <- txn.dbRef.get
        _   <- dbRef.set(db)
      } yield res
    }

  override def readOnly[A](f: ReadTxn[F] => F[A]): F[A] =
    for {
      db  <- dbRef.get
      txn <- ReadAndWriteTxn(db)
      res <- f(txn)
    } yield res
}
