package memdb.transaction

import memdb.schema.TableSchema

trait WriteTxn[F[_]] extends Txn[F] {
  def upsert[T](row: T)(implicit S: TableSchema[T]): F[Unit]
  def delete[T](row: T)(implicit S: TableSchema[T]): F[Unit]
}
