package memdb.schema

import scala.collection.immutable.List

final case class TableSchema[T](primary: Index[T], indexes: List[Index[T]] = List.empty)
