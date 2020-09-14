package memdb.annotation

import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

class EntityClassMacros(val c: Context) {

  import c.universe._

  case class DefinedIndex(name: String, colName: String, typ: Tree, unique: Boolean, extractFunc: Tree)
  case class PrimaryIndex(name: String, typ: Tree, extractFunc: Tree)

  def generateCompanionObject(annottees: c.Expr[Any]*): c.Expr[Any] =
    annottees.map(_.tree) match {
      case (classDef: ClassDef) :: Nil                           => modify(classDef, q"object ${classDef.name.toTermName} {}")
      case (classDef: ClassDef) :: (companion: ModuleDef) :: Nil => modify(classDef, companion)
      case _ :: Nil =>
        c.abort(c.enclosingPosition, "@entity can only be applied to case classes")
    }

  def modify(classDef: ClassDef, moduleDef: Tree) = {

    val primaries = getPrimaryIndexes(classDef)
    val indexes   = getIndexes(classDef)

    val primary = primaries match {
      case i :: Nil => i
      case Nil      => c.abort(c.enclosingPosition, "Each entity class should have one id")
      case _        => c.abort(c.enclosingPosition, "Entity classes can not have more than one id")
    }

    val (companionWithId, primaryId) = injectPrimaryRelatedObjects(classDef, moduleDef, primary)

    val result = indexes.foldLeft((companionWithId, List.empty[TermName])) {
      case (old, index) =>
        val res = injectIndexRelatedObjects(classDef, old._1, primary, index)
        (res._1, old._2.appended(res._2))
    }

    val companion = injectTableSchema(classDef, result._1, primaryId, result._2)

    c.Expr(
      q"""
        $classDef
        $companion
      """
    )
  }

  def getPrimaryIndexes(classDef: ClassDef): Seq[PrimaryIndex] =
    classDef.impl.body
      .map {
        case q"$mods val $name:$typ = ${_}" =>
          val result = mods.asInstanceOf[c.universe.Modifiers].annotations.filter { ann =>
            val typed = c.typecheck(ann)
            typed.tpe.typeSymbol.fullName match {
              case "memdb.annotation.id" => true
              case _                     => false
            }
          }

          if (result.size == 1) Some((name.toString, typ)) else None

        case _ => None
      }
      .filter(_.isDefined)
      .map(_.get)
      .map { r =>
        val name = TermName(r._1)
        PrimaryIndex(r._1, tq"${r._2}", q"((c: ${classDef.name.toTypeName}) => c.${name})")
      }

  def getIndexes(classDef: ClassDef) =
    classDef.impl.body
      .flatMap {
        case q"$mods val $name:$typ = ${_}" =>
          mods.asInstanceOf[c.universe.Modifiers].annotations.map { ann =>
            val typed = c.typecheck(ann)
            typed.tpe.typeSymbol.fullName match {
              case "memdb.annotation.index" =>
                typed match {
                  case q"new ${_}(${Literal(Constant(indexName: String))}, ${Literal(Constant(unique: Boolean))})" =>
                    Some(
                      DefinedIndex(
                        indexName,
                        name.toString,
                        typ,
                        unique,
                        q"((c: ${classDef.name.toTypeName}) => c.${name})"
                      )
                    )
                  case _ => None
                }
              case _ => None
            }
          }
        case _ => List.empty
      }
      .filter(_.isDefined)
      .map(_.get)

  def modifyCompanionObject(
    classDef: ClassDef,
    companion: ModuleDef,
    primary: PrimaryIndex,
    indexes: List[DefinedIndex]
  ) = {}

  def injectPrimaryRelatedObjects(classDef: ClassDef, moduleDef: Tree, primary: PrimaryIndex) = {

    val indexIdentifier = TermName(s"_memdb_primary_${primary.name}Index")
    val indexName       = TermName("_memdb_idIndex")
    val idType          = primary.typ
    val idFunc          = primary.extractFunc

    val q"$mods object $name extends ..$bases { ..$body }" = moduleDef

    val companion = q"""
        $mods object $name extends ..$bases {
          ..$body
          
          case object ${indexIdentifier} extends memdb.schema.IndexIdentifier
          
          implicit val ${indexName}: memdb.schema.UniqueIndex[${classDef.name.toTypeName}, ${idType}] =
            memdb.schema.UniqueIndex[${classDef.name.toTypeName}, ${idType}](
              ${indexIdentifier},
              ${idFunc},
              scala.collection.immutable.TreeMap.empty[${idType}, ${classDef.name.toTypeName}]
            )
                    
                  
          implicit val _memdb_idIndexSelector: memdb.schema.IndexSelectorByTypeName[${classDef.name.toTypeName}, ${idType}, "id"] = 
            new memdb.schema.IndexSelectorByTypeName[${classDef.name.toTypeName}, ${idType}, "id"] {
              override val index: memdb.schema.Index.Aux[${classDef.name.toTypeName}, ${idType}] = _memdb_idIndex
            }
                    
          implicit val _memdb_idIndexNameSelector: memdb.schema.IndexSelectorByName[${classDef.name.toTypeName}, "id"] = 
            new memdb.schema.IndexSelectorByName[${classDef.name.toTypeName}, "id"] {
              override val index: memdb.schema.Index[${classDef.name.toTypeName}] = _memdb_idIndex
            }
        }
        """

    (companion, indexName)
  }

  def injectIndexRelatedObjects(classDef: ClassDef, moduleDef: Tree, primary: PrimaryIndex, index: DefinedIndex) = {

    val idType                  = primary.typ
    val indexIdentifier         = TermName(s"_memdb_index_${index.name}Index")
    val indexSelectorByName     = TermName(s"_memdb_index_select_${index.name}")
    val indexSelectorByNameType = TermName(s"_memdb_index_select_type_${index.name}")
    val indexObject             = TermName(s"_memdb_${index.name}Index")
    val indexType               = index.typ
    val indexExtractor          = index.extractFunc
    val primaryExtractor        = primary.extractFunc

    val indexImplType = if (index.unique) {
      q"""
          implicit val ${indexObject}: memdb.schema.UniqueIndex[${classDef.name.toTypeName}, ${indexType}] =
            memdb.schema.UniqueIndex[${classDef.name.toTypeName}, ${indexType}](
              ${indexIdentifier},
              ${indexExtractor},
              scala.collection.immutable.TreeMap.empty[${indexType}, ${classDef.name.toTypeName}]
            )
      """
    } else {
      q"""
          implicit val ${indexObject}: memdb.schema.NonUniqueIndex[${classDef.name.toTypeName}, ${indexType}, ${idType}] =
            memdb.schema.NonUniqueIndex[${classDef.name.toTypeName}, ${indexType}, ${idType}](
              ${indexIdentifier},
              ${indexExtractor},
              ${primaryExtractor},
              scala.collection.immutable.TreeMap.empty[${indexType}, Map[${idType}, ${classDef.name.toTypeName}]]
            )
         """
    }

    val q"$mods object $name extends ..$bases { ..$body }" = moduleDef

    val companion = q"""
        $mods object $name extends ..$bases {
          ..$body
          
          case object ${indexIdentifier} extends memdb.schema.IndexIdentifier
          
          $indexImplType                   
          
          implicit val ${indexSelectorByName}: memdb.schema.IndexSelectorByTypeName[${classDef.name.toTypeName}, ${indexType}, ${index.name}] = 
            new memdb.schema.IndexSelectorByTypeName[${classDef.name.toTypeName}, ${indexType}, ${index.name}] {
              override val index: memdb.schema.Index.Aux[${classDef.name.toTypeName}, ${indexType}] = ${indexObject}
            }
                    
          implicit val ${indexSelectorByNameType}: memdb.schema.IndexSelectorByName[${classDef.name.toTypeName}, ${index.name}] = 
            new memdb.schema.IndexSelectorByName[${classDef.name.toTypeName}, ${index.name}] {
              override val index: memdb.schema.Index[${classDef.name.toTypeName}] = ${indexObject}
            }
        }
        """

    (companion, indexObject)
  }

  def injectTableSchema(classDef: ClassDef, moduleDef: Tree, primary: TermName, indexes: List[TermName]) = {

    val indexesDef =
      if (indexes.isEmpty) q"List.empty[memdb.schema.Index[${classDef.name.toTypeName}]]" else q"List(..${indexes})"

    val q"$mods object $name extends ..$bases { ..$body }" = moduleDef

    q"""
      $mods object $name extends ..$bases {
      
      ..$body
          
      implicit val _memdb_TableSchema: memdb.schema.TableSchema[${classDef.name.toTypeName}] = 
        memdb.schema.TableSchema[${classDef.name.toTypeName}](${primary}, ${indexesDef})
      }
    """
  }

}
