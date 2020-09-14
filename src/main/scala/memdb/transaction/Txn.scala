package memdb.transaction

import cats.effect.concurrent.Ref
import memdb.Database

trait Txn[F[_]] {
  val dbRef: Ref[F, Database]
}
