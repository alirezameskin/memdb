package memdb.annotation

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.language.experimental.macros

@compileTimeOnly("enable macro paradise to expand macro annotations")
class entity extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro EntityClassMacros.generateCompanionObject
}
