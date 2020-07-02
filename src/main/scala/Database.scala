class Database(val tables: Map[String, Table[_]]) {

  def first[C](col: String, v: Any)(implicit S: TableSchema[C]): Option[C] = {
    tables.get(S.name).flatMap { table =>
      table.asInstanceOf[Table[C]].items.headOption
    }
  }

  def all[C](implicit S: TableSchema[C]): Seq[C] = {
    tables
      .get(S.name)
      .map { table =>
        table.asInstanceOf[Table[C]].items
      }
      .getOrElse(List.empty[C])
  }

  def insert[C](row: C)(implicit S: TableSchema[C]): Database = {

    val newTable = tables.get(S.name) match {
      case Some(value) =>
        new Table[C] {
          override def schema: TableSchema[C] = S

          override def items: List[C] =
            row :: value.asInstanceOf[Table[C]].items
        }
      case None =>
        new Table[C] {
          override def schema: TableSchema[C] = S

          override def items: List[C] = List(row)
        }
    }
    new Database(tables + (S.name -> newTable))
  }
}

object Database {
  def empty: Database =
    new Database(Map.empty)
}
