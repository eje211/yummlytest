/**
  * Defines the lexicon of the query language of the Yummly exercise.
  *
  * @author Emmanuel Eytan
  */
package com.regularoddity.yummly

import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.Positional

sealed trait ParserToken extends Positional

case class OrToken() extends ParserToken
case class AndToken() extends ParserToken
case class OpenParenToken() extends  ParserToken
case class CloseParenToken() extends  ParserToken
case class InputToken(token: String) extends ParserToken

object QueryLexer extends RegexParsers {
  override def skipWhitespace: Boolean = true

  def token: Parser[InputToken] =
    "[a-z][a-z]*".r ^^ { str => InputToken(str) }

  def or: Parser[OrToken] = "|" ^^ {_ => OrToken()}
  def and: Parser[AndToken] = "&" ^^ {_ => AndToken()}
  def openParen: Parser[OpenParenToken] = "(" ^^ {_ => OpenParenToken()}
  def closeParen: Parser[CloseParenToken] = ")" ^^ {_ => CloseParenToken()}

  def tokens: Parser[List[ParserToken]] =
    phrase(rep1(token | closeParen | openParen | and | or))

  def apply(query: String): Either[ParserLexerError, List[ParserToken]] = parse(tokens, query) match {
    case NoSuccess(message, next) if next.source.toString.isEmpty =>
      Left(ParserLexerError(s"""query error Input cannot be empty."""))
    case NoSuccess(message, next) => Left(ParserLexerError(s"""Error at character "${next.first.toString}"."""))
    case Success(result, next) => Right(result)
  }
}

