package be.objectify.batch.concurrent

import scala.collection.{immutable, mutable}
import akka.actor.{Terminated, ActorLogging, Actor, ActorRef}

/**
 *
 * @param eventListener
 */
class Master(eventListener: ActorRef) extends Actor with ActorLogging {

  import Protocol._

  val consumers = mutable.Map.empty[ActorRef, Option[WorkAssociation]]
  val workQueue = mutable.Queue.empty[WorkAssociation]
  val subsequentItems = mutable.Map.empty[Any, List[Any]]

  def notifyConsumers(): Unit = {
    if (!workQueue.isEmpty) {
      consumers.foreach {
        case (worker, m) if m.isEmpty => worker ! WorkIsReady
        case _ =>
      }
    }
  }

  def noWork: Boolean = workQueue.isEmpty && subsequentItems.isEmpty match {
    case true =>
      val result: Option[Option[WorkAssociation]] = consumers.values.find(_.isDefined)
      result.isEmpty || result.get.isEmpty
    case false =>
      false
  }

  def receive: Actor.Receive = {
    case ConsumerRequestsWork(consumer) =>
      log.debug("Consumer is requesting work")
      if (consumers.contains(consumer)) {
        if (workQueue.isEmpty) {
          log.debug("No work available for consumer")
          consumer ! NoWorkToBeDone
        } else if (consumers(consumer) == None) {
          val workAssociation = workQueue.dequeue()
          consumers += (consumer -> Some(workAssociation))
          log.debug("[{}] Assigning work [{}] to consumer", workAssociation.key, workAssociation.work)
          consumer.tell(WorkToBeDone(workAssociation.key,
                                     workAssociation.work),
                        workAssociation.owner)
        }
      }
    case WorkIsDone(key, consumer, successful) =>
      log.debug("[{}] Work item finished with success [{}]", key, successful)
      if (!consumers.contains(consumer)) {
        log.error("[{}] said it's done work but we didn't know about it",
                  consumer)
      } else {
        val workAssociation = consumers(consumer).get
        consumers += (consumer -> None)

        if (subsequentItems.isDefinedAt(key)) {
          val items: List[Any] = subsequentItems(key)
          if (items.isEmpty) {
            subsequentItems -= key
          } else {
            val head: Any = items.head
            subsequentItems += (key -> items.tail)
            workQueue.enqueue(WorkAssociation(key, head, workAssociation.owner, successful))
            notifyConsumers()
          }
        }
      }

      if (noWork) {
        eventListener ! WorkQueueEmpty
      }
    case Work(items) =>
      log.debug("Received work for [{}] keys", items.size)
      items.foreach(pair => {
        pair._2.size match {
          case 0 => log.debug("No items associated with [{}]", pair._1)
          case _ => {
            log.debug("Added work for key [{}]", pair._1)
            workQueue.enqueue(WorkAssociation(pair._1, pair._2.head, eventListener, earlierSequenceOk = true))
            val otherItemsForKey: List[Any] = pair._2.tail
            otherItemsForKey.size match {
              case 0 =>  log.debug("No sequenced items associated with [{}]", pair._1)
              case _ =>
                log.debug("[{}] more items associated with key [{}]", otherItemsForKey.size, pair._1)
                subsequentItems += (pair._1 -> pair._2.tail)
            }
          }
        }
      })
      notifyConsumers()
    case ReQueue(key, work) =>
      log.debug("Re-queuing items for key [{}]", key)
      workQueue.enqueue(WorkAssociation(key, work, eventListener, earlierSequenceOk = true))
      notifyConsumers()
    case ConsumerCreated(consumer) =>
      log.debug("Consumer registered with master")
      context.watch(consumer)
      consumers += (consumer -> None)
      notifyConsumers()
    case CheckForFinish =>
      noWork match {
        case true => {
          log.debug("No work available in master, informing listener")
          eventListener ! WorkQueueEmpty
        }
        case false =>
      }
    case Terminated(consumer) =>
      val option: Option[WorkAssociation] = consumers(consumer)
      option match {
        case Some(_) => {
          val workAssociation = option.get
          val rework = immutable.Map(workAssociation.key -> List(workAssociation.work))
          self.tell(ReQueue(workAssociation.key, rework), workAssociation.owner)
        }
        case None => log.debug("Consumer died but had no work")
      }
      consumers -= consumer
    case msg => unhandled(msg)
  }

  case class WorkAssociation(key: Any,
                             work: Any,
                             owner: ActorRef,
                             earlierSequenceOk: Boolean)
}
