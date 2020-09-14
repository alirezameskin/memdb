package memdb.schema

trait Index[T] {
  type KEY
  val identifier: IndexIdentifier
  val toKey: T => KEY
  def upsert(row: T): Index[T]
  def delete(row: T): Index[T]
  def first(k: KEY): Option[T]
  def all: Iterable[T]
  def range(from: KEY, until: KEY): Iterable[T]
}

object Index {
  type Aux[T, K0] = Index[T] { type KEY = K0 }
}
