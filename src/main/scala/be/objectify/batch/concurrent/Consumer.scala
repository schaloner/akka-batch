package be.objectify.batch.concurrent

import akka.actor._
import scala.concurrent.Future

/**
 *
 * @param master
 */
abstract class Consumer(master: ActorSelection) extends Actor with ActorLogging {

  import Protocol._
  import context._

  def doWork(eventListener: ActorRef, key: Any, work: Any): Future[WorkComplete]

  def onCustomMessage(message: Any) = unhandled(message)

  override def preStart() = master ! ConsumerCreated(self)

  def active: Receive = {
    case WorkComplete(key, result, successful) =>
      log.debug("[Active consumer] Work complete, informing master and becoming idle")
      master ! WorkIsDone(key, self, successful)
      master ! ConsumerRequestsWork(self)
      context.become(idle)
    case WorkIsReady =>
      log.error("[Active consumer] Work is ready, but I'm already working.  Ignoring request.")
    case WorkToBeDone(_, _) =>
      log.error("[Active consumer] I 've been given work, but I 'm already busy.This is not good.")
    case NoWorkToBeDone =>
      log.debug("[Active consumer] No work to be done.  Ignoring request.")
    case msg =>
      log.debug("[Active consumer] Received a custom message [{}]", msg)
      onCustomMessage(msg)
  }

  def idle: Receive = {
    case WorkIsReady =>
      log.debug("[Idle consumer] Work is ready, requesting from master")
      master ! ConsumerRequestsWork(self)
    case WorkToBeDone(key, work) =>
      log.debug("[Idle consumer - {}] Got work [{}], becoming active", key, work)
      context.become(active)

      import akka.pattern.pipe
      doWork(sender(), key, work) pipeTo self
    case NoWorkToBeDone =>
      log.debug("[Idle consumer] Requested work, but none available")
    case msg =>
      log.debug("[Idle consumer] Received a custom message [{}]", msg)
      onCustomMessage(msg)
  }

  def receive = idle
}