import cats.effect.IO
import cats.effect.concurrent.Ref

class ReadOnlyTxn(val db: Ref[IO, Database]) extends ReadTxn {

  override def first[C](col: String,
                        v: Any)(implicit S: TableSchema[C]): IO[Option[C]] = {
    for {
      database <- db.get
    } yield database.first(col, v)
  }

  override def all[C](implicit S: TableSchema[C]): IO[Seq[C]] = {
    for {
      database <- db.get
    } yield database.all
  }
}
