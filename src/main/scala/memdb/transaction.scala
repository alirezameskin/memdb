package memdb

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._

trait Txn[F[_]] {
  val dbRef: Ref[F, Database]
}

trait ReadTxn[F[_]] extends Txn[F] {

  def first[T, K, N](key: K)(implicit is: IndexSelectorByTypeName[T, K, N]): F[Option[T]]

  def all[T, N](implicit is: IndexSelectorByName[T, N]): F[Iterable[T]]

  def all[T](implicit S: TableSchema[T]): F[Iterable[T]]

  def range[T, K, N](from: K, until: K)(implicit is: IndexSelectorByTypeName[T, K, N]): F[Iterable[T]]

}

trait WriteTxn[F[_]] {
  def upsert[T](row: T)(implicit S: TableSchema[T]): F[Unit]
  def delete[T](row: T)(implicit S: TableSchema[T]): F[Unit]
}

class ReadAndWriteTxn[F[_]: Sync](override val dbRef: Ref[F, Database]) extends ReadOnlyTxn(dbRef) with WriteTxn[F] {

  override def upsert[T](row: T)(implicit S: TableSchema[T]): F[Unit] =
    dbRef.update { db =>
      S.indexes
        .appended(S.primary)
        .foldLeft(db)((d, idx) => d.upsert(idx, row))
    }

  override def delete[T](row: T)(implicit S: TableSchema[T]): F[Unit] =
    dbRef.update { db =>
      S.indexes.appended(S.primary).foldLeft(db)((d, idx) => d.delete(idx, row))
    }
}

object ReadAndWriteTxn {
  def apply[F[_]: Sync](db: Database): F[ReadAndWriteTxn[F]] =
    for {
      ref <- Ref.of[F, Database](db)
    } yield new ReadAndWriteTxn(ref)
}

class ReadOnlyTxn[F[_]: Sync](val dbRef: Ref[F, Database]) extends ReadTxn[F] {

  override def first[T, K, N](key: K)(implicit is: IndexSelectorByTypeName[T, K, N]): F[Option[T]] =
    for {
      db  <- dbRef.get
      res <- db.first(is.index.identifier, key).pure[F]
    } yield res

  override def range[T, K, N](from: K, until: K)(implicit is: IndexSelectorByTypeName[T, K, N]): F[Iterable[T]] =
    for {
      db  <- dbRef.get
      res <- db.range(is.index.identifier, from, until).pure[F]
    } yield res

  override def all[T, N](implicit is: IndexSelectorByName[T, N]): F[Iterable[T]] =
    for {
      db  <- dbRef.get
      res <- db.all(is.index.identifier).pure[F]
    } yield res

  override def all[T](implicit S: TableSchema[T]): F[Iterable[T]] =
    for {
      db  <- dbRef.get
      res <- db.all(S.primary.identifier).pure[F]
    } yield res
}

object ReadOnlyTxn {
  def apply[F[_]: Sync](db: Database): F[ReadOnlyTxn[F]] =
    for {
      ref <- Ref.of[F, Database](db)
    } yield new ReadOnlyTxn[F](ref)
}
