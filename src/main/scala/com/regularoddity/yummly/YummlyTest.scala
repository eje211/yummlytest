//#full-example
package com.regularoddity.yummly

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.regularoddity.yummly

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

sealed trait Action
case class Store(collection: Int, items: Seq[Symbol]) extends Action
case class Query(query: QueryParserAST) extends Action
case class PrintMessage(message: String)

trait YummlyException extends Exception { val message: String }
case class YummlyQueryException(message: String) extends YummlyException
case class YummlyIndexException(message: String) extends YummlyException


object DataHandler {
  def props(printer: ActorRef) = Props(classOf[DataHandler], printer)
}

class DataHandler(printer: ActorRef) extends Actor {

  private val index = mutable.Map[Symbol, mutable.Set[Int]]()
  private val directory = mutable.Map[Int, Seq[Symbol]]()

  final private def parseQuery(query: QueryParserAST, result: Set[Symbol]=Set[Symbol]()): Set[Int] = query match {
    case Token(input: String) =>
      if (!index.contains(Symbol(input))) {
        throw YummlyQueryException(s"""query error Token either not yet stored or removed: "$input".""")
      }
      index(Symbol(input)).toSet
    case Group(operation: QueryParserAST) =>
      parseQuery(operation, result)
    case And(arg1: QueryItemAST, arg2: QueryItemAST) =>
      parseQuery(arg1, result) & parseQuery(arg2, result)
    case Or(arg1: QueryItemAST, arg2: QueryItemAST) =>
      parseQuery(arg1, result) | parseQuery(arg2, result)
  }

  def receive: PartialFunction[Any, Unit] = {
    case Query(query: QueryParserAST) =>
      try {
        val collections = parseQuery(query)
        printer ! PrintMessage(s"query results ${collections.toList.sorted.mkString(" ")}")
      } catch {
        case e: YummlyQueryException => printer ! PrintMessage(e.message)
      }
    case Store(coll, items) =>
      if (directory contains coll) {
        for (item <- directory(coll)) {
          index(item) -= coll
          if (index(item).isEmpty) {
            index.remove(item)
          }
        }
      }
      directory(coll) = items
      for (item <- items) {
        if (!index.contains(item)) {
          index(item) = mutable.Set[Int]()
        }
        index(item) += coll
      }
      printer ! PrintMessage(s"index ok $coll")
  }
}


object MessagePrinter {
  def props() = Props(new MessagePrinter)
}

class MessagePrinter extends Actor {
  override def receive: Receive = {
    case PrintMessage(message: String) =>
      println(message)
  }
}


  object YummlyTest extends App {
  override def main(args: Array[String] = super.args): Unit = {
    val system: ActorSystem = ActorSystem("yummyActorSystem")
    val printer: ActorRef = system.actorOf(MessagePrinter.props())
    val dataHandler: ActorRef = system.actorOf(DataHandler.props(printer))

    println(
      """
        |Yummly programming test entry by Emmanuel Eytan
        |
        |Enter query or index commands below.
        |Entering an empty line will quit the program.
        |""".stripMargin)
    for (ln <- io.Source.stdin.getLines) {
      if (ln.isEmpty) {
        printer ! PrintMessage("Goodbye!")
        System.exit(0)
      }
      val action = determineAction(ln.split(" ").toSeq.map(_.trim).filter(_.nonEmpty))
      action match {
        case Left(error: YummlyException) => printer ! PrintMessage(error.message)
        case Right(a: Action) => dataHandler ! a
      }
    }
  }

  def determineAction(input: Seq[String]): Either[YummlyException, Action] = {
    val index: Option[Int] = Try(input(1).toInt).toOption

    input.head match {
      case "index" if input.length > 2 && index.isDefined
          && input.slice(2, input.length).forall(_.matches("[a-z][a-z]*")) =>
        Right(Store(index.get, input.slice(2, input.length).map(s => Symbol(s))))
      case "index" if input.length <= 2 =>
        Left(YummlyIndexException("index error An index ID and some content must be specified."))
      case "index" if index.isEmpty =>
        Left(YummlyIndexException("index error The index ID must be an integer."))
      case "index" =>
        Left(YummlyIndexException("index error All tokens must be space-separated lower-case text."))
      case "query" if input.length == 1 =>
        Left(YummlyQueryException("query error A query needs to be specified."))
      case "query" =>
        val queryString = input.slice(1, input.length).foldLeft("")((a, b) => s"$a $b")
        QueryLexer(queryString) match {
          case Left(lexerError) => Left(lexerError)
          case Right(tokens) =>
            val query = QueryParser(tokens)
            query match {
              case Left(queryerror) => Left(queryerror)
              case Right(query) => Right(Query(query))
            }
        }
      case _ => Left(YummlyIndexException("error The command enteered was not recognised."))
    }
  }

}
