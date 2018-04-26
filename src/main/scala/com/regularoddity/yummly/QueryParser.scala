/**
  * Defines the grammar of the query language of the Yummly exercise and the object to parse the actual query.
  *
  * @author Emmanuel Eytan
  */

package com.regularoddity.yummly

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{NoPosition, Position, Positional, Reader}


case class ParserLexerError(message: String) extends YummlyException


sealed trait QueryParserAST extends Positional
sealed trait QueryItemAST extends QueryParserAST


/**
  * An individual token, part of a stored collection.
  * @param token The value of the token.
  */
case class Token(token: String) extends QueryItemAST

/**
  * A pending "or" or "and" operation.
  * @param content The pending operation.
  */
case class Group(content: QueryParserAST) extends QueryItemAST

/**
  * A pending "and" (intersect) operation.
  * @param arg1 The first argument of the intersection.
  * @param arg2 The second argument of the intersection.
  */
case class And(arg1: QueryItemAST, arg2: QueryItemAST) extends QueryParserAST

/**
  * A pending "or" (join) operation.
  * @param arg1 The first argument of the join.
  * @param arg2 The second argument of the join.
  */
case class Or(arg1: QueryItemAST, arg2: QueryItemAST) extends QueryParserAST


object QueryParser extends Parsers {
  override type Elem = ParserToken

  class ParserTokenReader(tokens: Seq[ParserToken]) extends Reader[ParserToken] {
    override def first: ParserToken = tokens.head
    override def atEnd: Boolean = tokens.isEmpty
    override def pos: Position = NoPosition
    override def rest: Reader[ParserToken] = new ParserTokenReader(tokens.tail)
  }

  def orStatement: Parser[QueryParserAST] = positioned {
    (item ~ OrToken() ~ item) ^^ {
      case (input1: QueryItemAST) ~ OrToken() ~ (input2: QueryItemAST) => Or(input1, input2)
    }
  }

  def andStatement: Parser[QueryParserAST] = positioned {
    (item ~ AndToken() ~ item) ^^ {
      case (input1: QueryItemAST) ~ AndToken() ~ (input2: QueryItemAST) => And(input1, input2)
    }
  }
  def either: Parser[QueryParserAST] = {
    orStatement | andStatement
  }

  def group: Parser[Group] = {
    (OpenParenToken() ~ either ~ CloseParenToken()) ^^ {
      case _ ~ either ~ _ => Group(either)
    }
  }

  def item: Parser[QueryItemAST] = {
    token | group
  }

  private def token: Parser[Token] = positioned {
    accept("InputToken", { case tokenroot @ InputToken(token) => Token(token) })
  }

  def root: Parser[QueryParserAST] = {
    orStatement | andStatement | item
  }

  def apply(tokens: List[ParserToken]): Either[ParserLexerError, QueryParserAST] = {
    val reader = new ParserTokenReader(tokens)
    root(reader) match {
      case NoSuccess(err, next) =>
        Left(ParserLexerError(s"""query error Wrong token at element: "${next.first.toString}"."""))
      case Success(result, next) if !next.atEnd =>
        Left(ParserLexerError( s"""query error Wrong token at element: "${next.first.toString}"."""))
      case Success(result, next) =>
        Right(result)
    }
  }
}


