package memdb.transaction

import cats.implicits._
import cats.effect.Sync
import cats.effect.concurrent.Ref
import memdb.Database
import memdb.schema.{IndexSelectorByName, IndexSelectorByTypeName, TableSchema}

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
