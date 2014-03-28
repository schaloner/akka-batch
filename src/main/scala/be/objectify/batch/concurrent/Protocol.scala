package be.objectify.batch.concurrent

import akka.actor.ActorRef

/**
 *
 */
object Protocol {

  // Master messages
  object NoWorkToBeDone
  object WorkIsReady
  case class WorkToBeDone(key: Any, work: Any)
  case class ReQueue(key: Any, work: Any)

  // Consumer messages
  case class ConsumerCreated(consumer: ActorRef)
  case class ConsumersCreated(requestedConsumers: Int, createdConsumers: Int)
  case class ConsumerRequestsWork(consumer: ActorRef)
  case class WorkIsDone(key: Any, consumer: ActorRef, successful: Boolean)
  case class WorkComplete(key: Any, result: Any, successful: Boolean)

  // Listener messages
  object JobFinished
  case class JobStatus(processed: Int, errors: Int)
  object QueryJobStatus
  case class WorkError(key: Any, work: Any, message: String)
  case class WorkSuccess(key: Any, work: Any, message: String)
  object WorkQueueEmpty

  // Producer messages
  case class CheckForWork(processed: Int, errors: Int, parameters: Map[Any, Any])
  case class LoadWork(processed: Int, errors: Int, parameters: Map[Any, Any])
  object CheckForFinish
  object LoadWorkFinished
  case class Work(items: scala.collection.Map[Any, List[Any]])

  sealed trait WorkStatus
  object MoreWorkAvailable extends WorkStatus
  object NoRemainingWork extends WorkStatus
}
