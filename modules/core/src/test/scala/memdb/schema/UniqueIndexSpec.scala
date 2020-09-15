package memdb.schema

import org.scalatest.flatspec._
import org.scalatest.matchers._

import scala.collection.immutable.TreeMap

class UniqueIndexSpec extends AnyFlatSpec with should.Matchers {
  case class TestEntity(id: Long, name: String)
  case object IdIndexIdentifier extends IndexIdentifier[TestEntity, Long]
  val idKeyExtractor: TestEntity => Long = _.id

  "upsert method" should "insert the entity if it doesn't exist" in {
    val entities = (0L until 10L).map(id => (id, TestEntity(id, s"Entity ${id}"))).toMap
    val beforeInsert = UniqueIndex(IdIndexIdentifier, idKeyExtractor, TreeMap.from(entities))

    val index = beforeInsert.upsert(TestEntity(25L, "Entity 25"))

    index.all.size shouldBe 11
    index.first(25) shouldBe Some(TestEntity(25L, "Entity 25"))
  }

  it should "update the existing entity with the same key" in {
    val entities = (0L until 10L).map(id => (id, TestEntity(id, s"Entity ${id}"))).toMap
    val beforeUpsert = UniqueIndex(IdIndexIdentifier, idKeyExtractor, TreeMap.from(entities))

    beforeUpsert.first(5) shouldBe Some(TestEntity(5L, "Entity 5"))

    val index = beforeUpsert.upsert(TestEntity(5L, "Entity 5 New"))

    index.all.size shouldBe 10
    index.first(5) shouldBe Some(TestEntity(5L, "Entity 5 New"))
  }

  "first method" should "return empty response when there is no record for that key" in {
    val index = UniqueIndex(IdIndexIdentifier, idKeyExtractor, TreeMap.empty[Long, TestEntity])

    index.first(100) shouldBe None
  }

  "all method" should "return all items in the index" in {
    val entities = (0L until 10L).map(id => (id, TestEntity(id, s"Entity ${id}"))).toMap
    val index = UniqueIndex(IdIndexIdentifier, idKeyExtractor, TreeMap.from(entities))

    index.all.size shouldBe 10
  }

  it should "return all items in order by id" in {
    val entities = List(19, 15, 16, 90, 3, 5).map(id => (id.toLong, TestEntity(id, s"Entity ${id}"))).toMap
    val index = UniqueIndex(IdIndexIdentifier, idKeyExtractor, TreeMap.from(entities))

    val result = index.all.toList

    result.size shouldBe 6
    result(0) shouldBe TestEntity(3L, "Entity 3")
    result(1) shouldBe TestEntity(5L, "Entity 5")
    result(2) shouldBe TestEntity(15L, "Entity 15")
    result(3) shouldBe TestEntity(16L, "Entity 16")
    result(4) shouldBe TestEntity(19L, "Entity 19")
    result(5) shouldBe TestEntity(90L, "Entity 90")
  }

  "range method" should "return all items inside a range" in {
    val entities = List(19, 15, 16, 90, 3, 5).map(id => (id.toLong, TestEntity(id, s"Entity ${id}"))).toMap
    val index = UniqueIndex(IdIndexIdentifier, idKeyExtractor, TreeMap.from(entities))

    val result = index.range(10, 20).toList

    result.size shouldBe 3
    result(0) shouldBe TestEntity(15L, "Entity 15")
    result(1) shouldBe TestEntity(16L, "Entity 16")
    result(2) shouldBe TestEntity(19L, "Entity 19")
  }

  "delete method" should "remove item by looking at the key" in {
    val entities = List(19, 15, 16, 90, 3, 5).map(id => (id.toLong, TestEntity(id, s"Entity ${id}"))).toMap
    val beforeDeleting = UniqueIndex(IdIndexIdentifier, idKeyExtractor, TreeMap.from(entities))

    val index = beforeDeleting.delete(TestEntity(15L, "Different text"))

    val result = index.range(10, 20).toList

    result.size shouldBe 2
    result(0) shouldBe TestEntity(16L, "Entity 16")
    result(1) shouldBe TestEntity(19L, "Entity 19")
  }

}
