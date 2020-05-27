package com.eed3si9n.fix

import metaconfig.generic.Surface
import metaconfig.{ConfDecoder, Configured}
import scalafix.v1._

import scala.meta._

case class NoInferConfig(
  disabledTypes: List[String] = List(
    "scala.Any",
    "scala.AnyVal",
    "java.io.Serializable",
    "scala.Serializable",
    "scala.Product"
  )
)

object NoInferConfig {
  def default: NoInferConfig = NoInferConfig()
  implicit val surface: Surface[NoInferConfig] =
    metaconfig.generic.deriveSurface[NoInferConfig]
  implicit val decoder: ConfDecoder[NoInferConfig] =
    metaconfig.generic.deriveDecoder(default)
}

/**
  * Provides linting to forbid specific type inference.
  */
final case class NoInfer(config: NoInferConfig)
    extends SemanticRule("NoInfer") {

  def this() = this(config = NoInferConfig())

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf
      .getOrElse("NoInfer")(this.config)
      .map { newConfig =>
        NoInfer(newConfig)
      }

  override def isLinter: Boolean = true

  override def fix(implicit doc: SemanticDocument): Patch = {
    checkSynthetics(doc).map(Patch.lint).asPatch
  }

  private def checkSynthetics(
    implicit doc: SemanticDocument
  ): Seq[Diagnostic] = {
    val terms = doc.tree collect {
      case term: Term => term
    }
    terms flatMap { term =>
      val syms = term.synthetics flatMap {
        case TypeApplyTree(_, typeArguments) =>
          typeArguments flatMap extractSymbols
        case semtree =>
          semtree.symbol.toList
      }
      syms collect {
        case disabledSymbol(sym) => createLintMessage(sym, term.pos)
      }
    }
  }

  private def createLintMessage(symbol: Symbol, pos: Position): Diagnostic = {
    val message =
      s"${symbol.displayName} was inferred, but it's forbidden by NoInfer"
    val id = s"${symbol.displayName}"
    Diagnostic(id, message, pos)
  }

  private def extractSymbols(tpe: SemanticType): List[Symbol] =
    tpe match {
      case TypeRef(prefix, symbol, typeArguments) =>
        symbol :: extractSymbols(prefix) ::: (typeArguments flatMap extractSymbols)
      case SingleType(prefix, symbol) =>
        symbol :: extractSymbols(prefix)
      case ThisType(symbol) => symbol :: Nil
      case SuperType(prefix, symbol) =>
        symbol :: extractSymbols(prefix)
      case ConstantType(_) =>
        Nil
      case IntersectionType(types) =>
        types flatMap extractSymbols
      case UnionType(types) =>
        types flatMap extractSymbols
      case WithType(types) =>
        types flatMap extractSymbols
      case StructuralType(tpe, _) =>
        extractSymbols(tpe)
      case AnnotatedType(_, tpe) =>
        extractSymbols(tpe)
      case ExistentialType(tpe, _) =>
        extractSymbols(tpe)
      case UniversalType(typeParameters, tpe) =>
        (typeParameters map { _.symbol }) ::: extractSymbols(tpe)
      case ByNameType(tpe) =>
        extractSymbols(tpe)
      case RepeatedType(tpe) =>
        extractSymbols(tpe)
      case _ => Nil
    }

  private val disabledSymbol: SymbolMatcher =
    SymbolMatcher.normalized(config.disabledTypes: _*)
}
