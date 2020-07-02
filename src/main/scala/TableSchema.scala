import scala.collection.immutable.List

trait TableSchema[T] {
  def name: String
}

object TableSchema {
  def apply[T](implicit scm: TableSchema[T]): TableSchema[T] = scm
}

trait Table[T] {
  def schema: TableSchema[T]
  def items: List[T]
}

object Table {
  def empty[T: TableSchema]: Table[T] = new Table[T]() {
    override def schema: TableSchema[T] = implicitly[TableSchema[T]]
    override def items: List[T] = List.empty
  }
}
