package memdb

import memdb.schema.{IndexIdentifier, UniqueIndex}
import org.scalatest.flatspec._
import org.scalatest.matchers._

import scala.collection.immutable.TreeMap

class DatabaseSpec extends AnyFlatSpec with should.Matchers {
  case class BookEntity(id: Long, title: String)
  case class UserEntity(id: Long, email: String, name: String)

  case object USER_INDEX_ID extends IndexIdentifier[UserEntity, Long]
  case object BOOK_INDEX_ID extends IndexIdentifier[BookEntity, Long]

  val userIndex = UniqueIndex[UserEntity, Long](USER_INDEX_ID, _.id, TreeMap.empty)
  val bookIndex = UniqueIndex[BookEntity, Long](BOOK_INDEX_ID, _.id, TreeMap.empty)

  "first method" should "return None when it doesn't have the index" in {
    val db = fillDb(Database.empty)

    db.first(USER_INDEX_ID, 100L) shouldBe None
  }

  it should "return entity when it exists in the requested Index" in {
    val db = fillDb(Database.empty)

    db.first(USER_INDEX_ID, 5L) shouldBe Some(UserEntity(5, "Entity 5", "user-5@test.com"))
  }

  "range method" should "return entities inside a range when there is any" in {
    val db = fillDb(Database.empty)

    val result = db.range[BookEntity, Long](BOOK_INDEX_ID, 5, 100).toList

    result.size shouldBe 5
    result(0) shouldBe BookEntity(5, "Book title 5")
    result(1) shouldBe BookEntity(6, "Book title 6")
    result(2) shouldBe BookEntity(7, "Book title 7")
    result(3) shouldBe BookEntity(8, "Book title 8")
    result(4) shouldBe BookEntity(9, "Book title 9")
  }

  it should "return empty result when there is no entity inside a range" in {
    val db = fillDb(Database.empty)

    val result = db.range[BookEntity, Long](BOOK_INDEX_ID, 10, 100).toList

    result.size shouldBe 0
  }

  "all method" should "return all entities in the index in order" in {
    val db = fillDb(Database.empty)

    val result = db.all(BOOK_INDEX_ID).toList

    result.size shouldBe 10
    result(0) shouldBe BookEntity(0, "Book title 0")
    result(1) shouldBe BookEntity(1, "Book title 1")
    result(2) shouldBe BookEntity(2, "Book title 2")
    result(3) shouldBe BookEntity(3, "Book title 3")
    result(4) shouldBe BookEntity(4, "Book title 4")
    result(5) shouldBe BookEntity(5, "Book title 5")
  }

  it should "return empty result when there is no entity inside a range" in {
    val db = fillWithUsers(Database.empty)

    val result = db.range[BookEntity, Long](BOOK_INDEX_ID, 10, 100).toList

    result.size shouldBe 0
  }

  it should "return empty result when there is no index inside for an entity" in {
    val db = fillWithUsers(Database.empty)

    val result = db.range[BookEntity, Long](BOOK_INDEX_ID, 0, 10).toList

    result.size shouldBe 0
  }

  "delete method" should "return remove entity from an index" in {
    val withAllEntities = fillDb(Database.empty)

    val db = withAllEntities.delete(bookIndex, BookEntity(5, "Book 5"))

    db.first(BOOK_INDEX_ID, 5L) shouldBe None
    db.range[BookEntity, Long](BOOK_INDEX_ID, 5, 100).size shouldBe 4
  }

  "upsert method" should "insert a new entity if it does not exist" in {
    val oldDb = fillDb(Database.empty)

    val db = oldDb.upsert(bookIndex, BookEntity(20, "Book title 20"))

    db.first(BOOK_INDEX_ID, 20L) shouldBe Some(BookEntity(20, "Book title 20"))
    db.range[BookEntity, Long](BOOK_INDEX_ID, 5, 100).size shouldBe 6
  }

  it should "update the existing entity if it already exist" in {
    val oldDb = fillDb(Database.empty)

    oldDb.first(BOOK_INDEX_ID, 6L) shouldBe Some(BookEntity(6, "Book title 6"))

    val db = oldDb.upsert(bookIndex, BookEntity(6, "New title 6"))

    db.first(BOOK_INDEX_ID, 6L) shouldBe Some(BookEntity(6, "New title 6"))
  }

  def fillDb(db: Database): Database =
    (fillWithUsers _).andThen(fillWithBooks)(db)

  def fillWithUsers(db: Database): Database =
    (0L until 10L)
      .map(id => UserEntity(id, s"Entity ${id}", s"user-${id}@test.com"))
      .foldRight(db) { (entity, db) =>
        db.upsert(userIndex, entity)
      }

  def fillWithBooks(db: Database): Database =
    (0L until 10L)
      .map(id => BookEntity(id, s"Book title ${id}"))
      .foldRight(db) { (entity, db) =>
        db.upsert(bookIndex, entity)
      }
}
