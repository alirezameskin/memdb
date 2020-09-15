package memdb.schema

import org.scalatest.flatspec._
import org.scalatest.matchers._

import scala.collection.immutable.TreeMap

class NonUniqueIndexSpec extends AnyFlatSpec with should.Matchers {
  case class UserEntity(id: Long, name: String, email: String)

  case object IdIndexIdentifier extends IndexIdentifier[UserEntity, Long]
  case object EmailIndexIdentifier extends IndexIdentifier[UserEntity, String]

  val idKeyExtractor: UserEntity => Long = _.id
  val emailExtractor: UserEntity => String = _.email

  "upsert method" should "insert the entity if it doesn't exist" in {
    val entities: Seq[UserEntity] = (0L until 10L).map(id =>  UserEntity(id, s"Entity ${id}", s"user-${id}@test.com"))
    val emptyIndex = NonUniqueIndex[UserEntity, String, Long](EmailIndexIdentifier, emailExtractor, idKeyExtractor, TreeMap.empty)

    val index = entities.foldRight(emptyIndex){ (entity, index) => index.upsert(entity)}

    val newUser = UserEntity(100, "New User", "user-100@test.com")
    val newIndex = index.upsert(newUser)

    newIndex.all.size shouldBe 11
    newIndex.first("user-100@test.com") shouldBe Some(newUser)
  }

  it should "update the existing entity with the same key" in {
    val entities: Seq[UserEntity] = (0L until 10L).map(id =>  UserEntity(id, s"Entity ${id}", s"user-${id}@test.com"))
    val emptyIndex = NonUniqueIndex[UserEntity, String, Long](EmailIndexIdentifier, emailExtractor, idKeyExtractor, TreeMap.empty)

    val index = entities.foldRight(emptyIndex){ (entity, index) => index.upsert(entity)}

    val newUser = UserEntity(6, "User 6 Updated", "user-6@test.com")
    val newIndex = index.upsert(newUser)

    newIndex.all.size shouldBe 10
    newIndex.first("user-6@test.com") shouldBe Some(newUser)
  }

}
