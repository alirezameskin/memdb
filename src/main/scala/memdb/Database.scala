package memdb

import memdb.schema.{Index, IndexIdentifier}

class Database(val indexes: Map[IndexIdentifier, Index[_]]) {

  def first[T, K](index: IndexIdentifier, key: K): Option[T] =
    for {
      idx <- indexes.get(index)
      res <- idx.first(key.asInstanceOf[idx.KEY])
    } yield res.asInstanceOf[T]

  def range[T, K](index: IndexIdentifier, from: K, until: K): Iterable[T] =
    indexes
      .get(index)
      .map { idx =>
        idx
          .range(from.asInstanceOf[idx.KEY], until.asInstanceOf[idx.KEY])
          .asInstanceOf[Iterable[T]]
      }
      .getOrElse(Iterable.empty[T])

  def all[T](index: IndexIdentifier): Iterable[T] =
    indexes
      .get(index)
      .map(_.all.asInstanceOf[Iterable[T]])
      .getOrElse(Iterable.empty[T])

  def upsert[T](index: Index[T], row: T): Database = {
    val idxs = this.indexes.updatedWith(index.identifier) {
      case Some(idx) => Some(idx.asInstanceOf[Index[T]].upsert(row).asInstanceOf[Index[_]])
      case None      => Some(index.upsert(row))
    }

    new Database(idxs)
  }

  def delete[T](index: Index[T], row: T): Database = {
    val idxs = this.indexes.updatedWith(index.identifier) {
      _.map { idx =>
        idx.asInstanceOf[Index[T]].delete(row).asInstanceOf[Index[_]]
      }
    }

    new Database(idxs)
  }
}

object Database {
  def empty: Database = new Database(Map.empty)
}
