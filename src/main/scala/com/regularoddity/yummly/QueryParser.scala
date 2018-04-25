package com.regularoddity.yummly

import scala.util.parsing.combinator.{Parsers, RegexParsers}
import scala.util.parsing.input.{NoPosition, Position, Positional, Reader}

trait WorkflowCompilationError
case class WorkflowLexerError(msg: String) extends WorkflowCompilationError

sealed trait ParserToken extends Positional

case class OrToken() extends ParserToken
case class AndToken() extends ParserToken
case class OpenParenToken() extends  ParserToken
case class CloseParenToken() extends  ParserToken
case class InputToken(token: String) extends ParserToken

object WorkflowLexer extends RegexParsers {
  override def skipWhitespace: Boolean = true

  def token: Parser[InputToken] =
    "[a-z][a-z]*".r ^^ { str => InputToken(str) }

  def or: Parser[OrToken] = "|" ^^ {_ => OrToken()}
  def and: Parser[AndToken] = "&" ^^ {_ => AndToken()}
  def openParen: Parser[OpenParenToken] = "(" ^^ {_ => OpenParenToken()}
  def closeParen: Parser[CloseParenToken] = ")" ^^ {_ => CloseParenToken()}

  def tokens: Parser[List[ParserToken]] =
    phrase(rep1(token | closeParen | openParen | and | or))

  def apply(query: String): Either[WorkflowLexerError, List[ParserToken]] = parse(tokens, query) match {
    case NoSuccess(message, next) if next.source.toString.isEmpty =>
      Left(WorkflowLexerError(s"""Error: input cannot be empty."""))
    case NoSuccess(message, next) => Left(WorkflowLexerError(s"""Error at character "${next.first.toString}"."""))
    case Success(result, next) => Right(result)
  }
}

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
      case Token(input1) ~ _ ~ Token(input2) => Or(Left(input1), Left(input2))
      case Group(input1) ~ _ ~ Token(input2) => Or(Right(input1), Left(input2))
      case Token(input1) ~ _ ~ Group(input2) => Or(Left(input1), Right(input2))
      case Group(input1) ~ _ ~ Group(input2) => Or(Right(input1), Right(input2))
    }
  }

  def andStatement: Parser[QueryParserAST] = positioned {
    (item ~ AndToken() ~ item) ^^ {
      case Token(input1) ~ _ ~ Token(input2) => And(Left(input1), Left(input2))
      case Group(input1) ~ _ ~ Token(input2) => And(Right(input1), Left(input2))
      case Token(input1) ~ _ ~ Group(input2) => And(Left(input1), Right(input2))
      case Group(input1) ~ _ ~ Group(input2) => And(Right(input1), Right(input2))
    }
  }
  def either: Parser[QueryParserAST] = {
    orStatement | andStatement
  }

  def group: Parser[QueryParserAST] = {
    (OpenParenToken() ~ either ~ CloseParenToken()) ^^ {
      case _ ~ either ~ _ => Group(either)
    }
  }

  def item: Parser[QueryParserAST] = {
    token | group
  }

  private def token: Parser[Token] = positioned {
    accept("InputToken", { case tokenroot @ InputToken(token) => Token(token) })
  }

  def root: Parser[QueryParserAST] = {
    orStatement | andStatement | item
  }

  def apply(tokens: List[ParserToken]): Either[WorkflowLexerError, QueryParserAST] = {
    val reader = new ParserTokenReader(tokens)
    root(reader) match {
      case NoSuccess(err, next) => Left(WorkflowLexerError(s"""Error found at element "${next.first.toString}"."""))
      case Success(result, next) => Right(result)
    }
  }
}


sealed trait QueryParserAST extends Positional
case class Token(token: String) extends QueryParserAST
case class Group(content: QueryParserAST) extends QueryParserAST
case class And(arg1: Either[String, QueryParserAST], arg2: Either[String, QueryParserAST]) extends QueryParserAST
case class Or(arg1: Either[String, QueryParserAST], arg2: Either[String, QueryParserAST]) extends QueryParserAST

