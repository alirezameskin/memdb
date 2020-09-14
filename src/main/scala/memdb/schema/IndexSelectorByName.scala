package memdb.schema

trait IndexSelectorByName[T, N] {
  type N
  val index: Index[T]
}
