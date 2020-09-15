package memdb

import memdb.schema.{IndexIdentifier, UniqueIndex}
import org.scalatest.flatspec._
import org.scalatest.matchers._

import scala.collection.immutable.TreeMap

class DatabaseSpec extends AnyFlatSpec with should.Matchers {
  case class BookEntity(id: Long, title: String)
  case class UserEntity(id: Long, email: String, name: String)

  case object USER_INDEX_ID extends IndexIdentifier[UserEntity, Long]

  "first method" should "return None when it doesn't have the index" in {
    val index = UniqueIndex[UserEntity, Long](USER_INDEX_ID, _.id, TreeMap.empty)
    val emptyDb = Database.empty

    val db = (0L until 10L)
      .map(id =>  UserEntity(id, s"Entity ${id}", s"user-${id}@test.com"))
      .foldRight(emptyDb){(entity, db) =>
        db.upsert(index, entity)
      }

    db.first(USER_INDEX_ID, 100L) shouldBe None
  }

  "first method" should "return entity when it exists in the requested Index" in {
    val index = UniqueIndex[UserEntity, Long](USER_INDEX_ID, _.id, TreeMap.empty)
    val emptyDb = Database.empty

    val db = (0L until 10L)
      .map(id =>  UserEntity(id, s"Entity ${id}", s"user-${id}@test.com"))
      .foldRight(emptyDb){(entity, db) =>
        db.upsert(index, entity)
      }

    db.first(USER_INDEX_ID, 5L) shouldBe Some(UserEntity(5, "Entity 5", "user-5@test.com"))
  }
}
