package memdb.transaction

import cats.implicits._
import cats.effect.Sync
import cats.effect.concurrent.Ref
import memdb.schema.TableSchema
import memdb.Database

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
