package be.objectify.batch.concurrent

import akka.actor.ActorRef
import scala.concurrent.Future
import scala.collection.immutable

/**
 *
 * @param master
 * @param resultListener
 */
class TestProducer(master: ActorRef, resultListener: ActorRef) extends Producer(master, resultListener) {

  import Protocol._
  import context._

  val max = 5

  def hasMoreWork(processed: Int, errors: Int, parameters: Map[Any, Any]): Future[WorkStatus] = {
    Future[WorkStatus] {
      processed match {
        case p if p < max => MoreWorkAvailable
        case _ => NoRemainingWork
      }
    }
  }

  def getWork(processed: Int, errors: Int, parameters: Map[Any, Any]): Future[Work] = {
    Future[Work] {
      parameters("workType") match {
        case "unordered" => Work(immutable.Map[Any, List[Any]]("a" -> List("do"), "b" -> List("re"), "c" -> List("mi"), "d" -> List("fa"), "e" -> List("sol")))
        case "ordered" => Work(immutable.Map[Any, List[Any]]("a" -> List("do", "re", "mi", "fa", "sol")))
      }
    }
  }
}
