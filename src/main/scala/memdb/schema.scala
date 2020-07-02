package memdb

import scala.collection.immutable.{List, TreeMap}

trait IndexIdentifier

sealed trait Index[T] {
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

trait IndexSelectorByTypeName[T, K, N] {
  type N
  val index: Index.Aux[T, K]
}

trait IndexSelectorByName[T, N] {
  type N
  val index: Index[T]
}

final case class TableSchema[T](primary: Index[T], indexes: List[Index[T]] = List.empty)

final case class UniqueIndex[T, K](identifier: IndexIdentifier, toKey: T => K, items: TreeMap[K, T]) extends Index[T] {
  override type KEY = K

  override def upsert(row: T): Index[T] = {
    val nItems = this.items.updated(toKey(row), row)

    this.copy(items = nItems)
  }

  override def all: Iterable[T] = items.values

  override def first(k: K): Option[T] =
    items.get(k)

  override def range(from: K, until: K): Iterable[T] =
    items.range(from, until).values

  override def delete(row: T): Index[T] = {
    val nItems = items.removed(toKey(row))
    this.copy(items = nItems)
  }
}

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
