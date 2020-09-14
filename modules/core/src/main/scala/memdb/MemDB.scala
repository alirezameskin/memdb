package memdb

import cats.Monad
import cats.effect.Concurrent
import cats.effect.concurrent.{Ref, Semaphore}
import cats.implicits._
import memdb.transaction.{ReadTxn, WriteTxn}

trait MemDB[F[_]] {
  def readOnly[A](f: ReadTxn[F] => F[A]): F[A]
  def transaction[A](f: WriteTxn[F] with ReadTxn[F] => F[A]): F[A]
}

object MemDB {

  def empty[F[_]: Monad](implicit C: Concurrent[F]): F[MemDB[F]] =
    for {
      sem <- Semaphore[F](1)
      db  <- Ref.of[F, Database](Database.empty)
    } yield new MemDBImpl(db, sem)
}
