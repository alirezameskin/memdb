package memdb.schema

trait IndexSelectorByTypeName[T, K, N] {
  type N
  val index: Index.Aux[T, K]
}
