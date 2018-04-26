/**
  * Yummly programming test unit testing spec
  *
  * @author Emmanuel Eytan
  */

package com.regularoddity.yummly

import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}

import scala.concurrent.duration._

class YummlyTestSpec(_system: ActorSystem)
  extends TestKit(_system)
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("YummlyTestSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "An empty query" should "return an error when lexed" in {
    val query = ""
    QueryLexer(query).left.get shouldBe ParserLexerError("""query error Input cannot be empty.""")
  }

  "A one-word query" should "be lexed into a single token object" in {
    val query = "token"
    QueryLexer(query).right.get shouldBe (InputToken("token") :: Nil)
  }

  "A one-word query" should "be parsed into a single token object" in {
    val query = "token"
    QueryParser(QueryLexer(query).right.get).right.get shouldBe Token("token")
  }

  "A query with an invalid character" should "return an error when lexed" in {
    val query = "hello|Hello"
    QueryLexer(query).left.get shouldBe ParserLexerError("""Error at character "H".""")
  }

  "An additional character in a query" should "return an error when parsed" in {
    val query = "(a|b)&c b"
    val tokens = QueryLexer(query).right.get
    QueryParser(tokens).left.get shouldBe ParserLexerError("""query error Wrong token at element: "InputToken(b)".""")
  }

  "An misplaced parenthesis in a query" should "return an error when parsed" in {
    val query = "(a)|b)&c"
    val tokens = QueryLexer(query).right.get
    QueryParser(tokens).left.get shouldBe ParserLexerError("""query error Wrong token at element: "CloseParenToken()".""")
  }

  "A query without enough parentheses" should "return an error when parsed" in {
    val query = "a|b&c"
    val tokens = QueryLexer(query).right.get
    QueryParser(tokens).left.get shouldBe ParserLexerError("""query error Wrong token at element: "AndToken()".""")
  }

  "Adding tokens to an index" should "return an ok message" in {
    val testProbe = TestProbe()
    val dataHandler = system.actorOf(DataHandler.props(testProbe.ref))
    dataHandler ! Store(2, "tomato soup cream".split(" ").map(s => Symbol(s)))
    testProbe.expectMsg(500 millis, PrintMessage("index ok 2"))
  }

  "A query for a non-existent item" should "return an error when processed" in {
    val testProbe = TestProbe()
    val dataHandler = system.actorOf(DataHandler.props(testProbe.ref))
    dataHandler ! Query(QueryLexer("tomato").flatMap(l => QueryParser(l)).right.get)
    testProbe.expectMsg(500 millis,
      PrintMessage("""query error Token either not yet stored or removed: "tomato"."""))
  }

  "Having tokens in an index" should "make its values available to query" in {
    val testProbe = TestProbe()
    val dataHandler = system.actorOf(DataHandler.props(testProbe.ref))
    dataHandler ! Store(2, "tomato soup cream".split(" ").map(s => Symbol(s)))
    dataHandler ! Query(QueryLexer("tomato").flatMap(l => QueryParser(l)).right.get)
    testProbe.expectMsg(500 millis, PrintMessage("index ok 2"))
    testProbe.expectMsg(500 millis, PrintMessage("query results 2"))
  }

  "Overriding an index" should "make its content unavailable" in {
    val testProbe = TestProbe()
    val dataHandler = system.actorOf(DataHandler.props(testProbe.ref))
    dataHandler ! Store(2, "tomato soup cream".split(" ").map(s => Symbol(s)))
    dataHandler ! Store(2, "coffee sugar".split(" ").map(s => Symbol(s)))
    dataHandler ! Query(QueryLexer("tomato").flatMap(l => QueryParser(l)).right.get)
    testProbe.expectMsg(500 millis, PrintMessage("index ok 2"))
    testProbe.expectMsg(500 millis, PrintMessage("index ok 2"))
    testProbe.expectMsg(500 millis,
      PrintMessage("""query error Token either not yet stored or removed: "tomato"."""))
  }

  "Storing a complex index" should "allow for complex queries" in {
    val testProbe = TestProbe()
    val dataHandler = system.actorOf(DataHandler.props(testProbe.ref))
    dataHandler ! Store(2, "tomato soup cream".split(" ").map(s => Symbol(s)))
    dataHandler ! Store(3, "coffee sugar cream".split(" ").map(s => Symbol(s)))
    dataHandler ! Store(5, "cake cherries flour".split(" ").map(s => Symbol(s)))
    dataHandler ! Store(8, "beef pototoes gravy salt".split(" ").map(s => Symbol(s)))
    dataHandler ! Store(13, "soup celery onions salt potatoes garlic".split(" ").map(s => Symbol(s)))
    dataHandler ! Store(21, "coconut apples bananas".split(" ").map(s => Symbol(s)))
    dataHandler ! Query(QueryLexer("cherries|((cream|salt)&soup)").flatMap(l => QueryParser(l)).right.get)
    testProbe.expectMsg(500 millis, PrintMessage("index ok 2"))
    testProbe.expectMsg(500 millis, PrintMessage("index ok 3"))
    testProbe.expectMsg(500 millis, PrintMessage("index ok 5"))
    testProbe.expectMsg(500 millis, PrintMessage("index ok 8"))
    testProbe.expectMsg(500 millis, PrintMessage("index ok 13"))
    testProbe.expectMsg(500 millis, PrintMessage("index ok 21"))
    testProbe.expectMsg(500 millis, PrintMessage("query results 2 5 13"))
  }

}
