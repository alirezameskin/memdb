import cats.effect.IO
import cats.effect.concurrent.Ref

trait Txn {
  val db: Ref[IO, Database]
}

trait ReadTxn extends Txn {
  def first[C](col: String, v: Any)(implicit S: TableSchema[C]): IO[Option[C]]
  def all[C](implicit S: TableSchema[C]): IO[Seq[C]]
}

trait ReadWriteTxn extends ReadTxn {
  def insert[C](row: C)(implicit S: TableSchema[C]): IO[Unit]
}
