package memdb.transaction

import memdb.schema.{IndexSelectorByName, IndexSelectorByTypeName, TableSchema}

trait ReadTxn[F[_]] extends Txn[F] {

  def first[T, K, N](key: K)(implicit is: IndexSelectorByTypeName[T, K, N]): F[Option[T]]

  def all[T, N](implicit is: IndexSelectorByName[T, N]): F[Iterable[T]]

  def all[T](implicit S: TableSchema[T]): F[Iterable[T]]

  def range[T, K, N](from: K, until: K)(implicit is: IndexSelectorByTypeName[T, K, N]): F[Iterable[T]]
}
