import scala.annotation.StaticAnnotation

sealed trait IndexAnnotation extends StaticAnnotation {
  def name: String
  def unique: Boolean
}
case class index(name: String, unique: Boolean = false) extends IndexAnnotation
case class id() extends IndexAnnotation {
  override def name: String = "id"
  override def unique: Boolean = true
}
