package memdb.schema

import scala.collection.immutable.TreeMap

final case class NonUniqueIndex[T, K, P](
  identifier: IndexIdentifier,
  toKey: T => K,
  getPrimary: T => P,
  items: TreeMap[K, Map[P, T]]
) extends Index[T] {
  override type KEY = K

  override def upsert(row: T): Index[T] = {
    val id: P = getPrimary(row)

    val nItems = this.items.updatedWith(toKey(row)) {
      case Some(map) => Some(map + (id -> row))
      case None      => Some(Map(id -> row))
    }

    this.copy(items = nItems)
  }

  override def all: Iterable[T] =
    items.valuesIterator.flatMap(_.values).to(Iterable)

  override def first(k: K): Option[T] =
    items.get(k).map(_.head._2)

  override def range(from: K, until: K): Iterable[T] =
    items.range(from, until).valuesIterator.toList.flatMap(_.values)

  override def delete(row: T): Index[T] = {
    val id: P = getPrimary(row)
    val nItems = items.updatedWith(toKey(row)) {
      case Some(map) => Some(map.removed(id))
      case None      => None
    }
    this.copy(items = nItems)
  }
}
