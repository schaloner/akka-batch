package be.objectify.batch.concurrent

import akka.actor.{ActorSelection, ActorRef}
import be.objectify.batch.concurrent.Protocol.{WorkSuccess, WorkComplete}
import scala.concurrent.Future

/**
 *
 * @param master
 */
class TestConsumer(master: ActorSelection) extends Consumer(master) {
  implicit val ec = context.dispatcher

  override def doWork(eventListener: ActorRef, key: Any, work: Any): Future[WorkComplete] = {
    import akka.pattern.pipe
    Future {
      eventListener ! WorkSuccess(key, work, "ok")
      WorkComplete(key, "done", successful = true)
    } pipeTo self
  }
}