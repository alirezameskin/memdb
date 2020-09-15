package memdb.schema

import scala.collection.immutable.TreeMap

final case class UniqueIndex[T, K](identifier: IndexIdentifier[T, K], toKey: T => K, items: TreeMap[K, T]) extends Index[T] {
  override type KEY = K

  override def upsert(row: T): UniqueIndex[T, K] = {
    val nItems = this.items.updated(toKey(row), row)

    this.copy(items = nItems)
  }

  override def all: Iterable[T] =
    items.values

  override def first(k: K): Option[T] =
    items.get(k)

  override def range(from: K, until: K): Iterable[T] =
    items.range(from, until).values

  override def delete(row: T): UniqueIndex[T, K] = {
    val nItems = items.removed(toKey(row))
    this.copy(items = nItems)
  }
}
