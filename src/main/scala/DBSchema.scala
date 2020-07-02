class DBSchema[T](val tables: Map[String, TableSchema[T]]) {}

object DBSchema {
  def apply[T](implicit scm: DBSchema[T]): DBSchema[T] = scm
}
