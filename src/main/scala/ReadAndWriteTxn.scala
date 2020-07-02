import cats.effect.IO
import cats.effect.concurrent.Ref

class ReadAndWriteTxn(val db: Ref[IO, Database]) extends ReadWriteTxn {

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

  override def insert[C](row: C)(implicit S: TableSchema[C]): IO[Unit] = {
    for {
      _ <- db.update(d => d.insert(row))
    } yield ()
  }
}

object ReadAndWriteTxn {
  def apply(db: Database): IO[ReadAndWriteTxn] =
    for {
      ref <- Ref.of[IO, Database](db)
    } yield new ReadAndWriteTxn(ref)
}
