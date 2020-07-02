package memdb

import cats.effect.IO
import cats.effect.concurrent.Ref
import cats.implicits._

trait Txn {
  val dbRef: Ref[IO, Database]
}

trait ReadTxn extends Txn {

  def first[T, K, N](key: K)(implicit is: IndexSelectorByTypeName[T, K, N]): IO[Option[T]]

  def all[T, N](implicit is: IndexSelectorByName[T, N]): IO[Iterable[T]]

  def all[T](implicit S: TableSchema[T]): IO[Iterable[T]]

  def range[T, K, N](from: K, until: K)(implicit is: IndexSelectorByTypeName[T, K, N]): IO[Iterable[T]]

}

trait WriteTxn {
  def upsert[T](row: T)(implicit S: TableSchema[T]): IO[Unit]
  def delete[T](row: T)(implicit S: TableSchema[T]): IO[Unit]
}

class ReadAndWriteTxn(override val dbRef: Ref[IO, Database]) extends ReadOnlyTxn(dbRef) with WriteTxn {

  override def upsert[T](row: T)(implicit S: TableSchema[T]): IO[Unit] =
    dbRef.update { db =>
      S.indexes
        .appended(S.primary)
        .foldLeft(db)((d, idx) => d.upsert(idx, row))
    }

  override def delete[T](row: T)(implicit S: TableSchema[T]): IO[Unit] =
    dbRef.update { db =>
      S.indexes.appended(S.primary).foldLeft(db)((d, idx) => d.delete(idx, row))
    }
}

object ReadAndWriteTxn {
  def apply(db: Database): IO[ReadAndWriteTxn] =
    for {
      ref <- Ref.of[IO, Database](db)
    } yield new ReadAndWriteTxn(ref)
}

class ReadOnlyTxn(val dbRef: Ref[IO, Database]) extends ReadTxn {

  override def first[T, K, N](key: K)(implicit is: IndexSelectorByTypeName[T, K, N]): IO[Option[T]] =
    for {
      db  <- dbRef.get
      res <- db.first(is.index.identifier, key).pure[IO]
    } yield res

  override def range[T, K, N](from: K, until: K)(implicit is: IndexSelectorByTypeName[T, K, N]): IO[Iterable[T]] =
    for {
      db  <- dbRef.get
      res <- db.range(is.index.identifier, from, until).pure[IO]
    } yield res

  override def all[T, N](implicit is: IndexSelectorByName[T, N]): IO[Iterable[T]] =
    for {
      db  <- dbRef.get
      res <- db.all(is.index.identifier).pure[IO]
    } yield res

  override def all[T](implicit S: TableSchema[T]): IO[Iterable[T]] =
    for {
      db  <- dbRef.get
      res <- db.all(S.primary.identifier).pure[IO]
    } yield res
}
