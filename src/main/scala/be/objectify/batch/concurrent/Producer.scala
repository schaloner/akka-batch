package be.objectify.batch.concurrent

import akka.actor.{ActorRef, ActorLogging, Actor}
import scala.concurrent.Future

/**
 *
 * @param master
 * @param eventListener
 */
abstract class Producer(master: ActorRef, eventListener: ActorRef) extends Actor with ActorLogging {

  import Protocol._
  import context._

  def onCustomMessage(message: Any) = unhandled(message)

  def hasMoreWork(processed: Int, errors: Int, parameters: Map[Any, Any]): Future[Any]

  def getWork(processed: Int, errors: Int, parameters: Map[Any, Any]): Future[Work]

  def active: Receive = {
    case LoadWorkFinished =>
      log.debug("[Active] Load work finished, becoming idle")
      context.become(idle)
    case MoreWorkAvailable =>
      log.debug("[Active] More work available, informing listener and becoming idle")
      eventListener ! MoreWorkAvailable
      context.become(idle)
    case NoRemainingWork =>
      log.debug("[Active] No remaining work, informing listener and becoming idle")
      eventListener ! JobFinished
      context.become(idle)
    case CheckForWork =>
      log.error("[Active] We're already checking for work, learn patience")
    case LoadWork =>
      log.error("[Active] We're already loading work, learn patience")
    case msg =>
      log.debug("[Active] Received a custom message [{}]", msg)
      onCustomMessage(msg)
  }

  def idle: Receive = {
    case CheckForWork(processed, errors, parameters) =>
      log.debug("[Idle] Checking for work, becoming active")
      import akka.pattern.pipe
      hasMoreWork(processed, errors, parameters) pipeTo self
      context.become(active)
    case LoadWork(processed, errors, parameters) =>
      log.debug("[Idle] Loading work, becoming active")
      import akka.pattern.pipe
      val future: Future[Work] = getWork(processed, errors, parameters)
      future to (master, sender())
      future.onComplete{
        case _ => Future {
          LoadWorkFinished
        } pipeTo self
      }
      context.become(active)
    case msg =>
      log.debug("[Idle] Received a custom message [{}]", msg)
      onCustomMessage(msg)
  }

  def receive = idle
}
