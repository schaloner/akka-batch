package be.objectify.batch.concurrent

import akka.actor.ActorRef

/**
 *
 */
object Protocol {

  // Master messages
  case class NoWorkToBeDone()
  case class WorkIsReady()
  case class WorkToBeDone(key: Any, work: Any)
  case class ReQueue(key: Any, work: Any)

  // Consumer messages
  case class ConsumerCreated(consumer: ActorRef)
  case class ConsumersCreated(requestedConsumers: Int, createdConsumers: Int)
  case class ConsumerRequestsWork(consumer: ActorRef)
  case class WorkIsDone(key: Any, consumer: ActorRef, successful: Boolean)
  case class WorkComplete(key: Any, result: Any, successful: Boolean)

  // Listener messages
  case class JobFinished()
  case class JobStatus(processed: Int, errors: Int)
  case class QueryJobStatus()
  case class WorkError(key: Any, work: Any, message: String)
  case class WorkSuccess(key: Any, work: Any, message: String)
  case class WorkQueueEmpty()

  // Producer messages
  case class CheckForWork(processed: Int, errors: Int, parameters: Map[Any, Any])
  case class LoadWork(processed: Int, errors: Int, parameters: Map[Any, Any])
  case class CheckForFinish()
  case class MoreWorkAvailable()
  case class LoadWorkFinished()
  case class NoRemainingWork()
  case class Work(items: Map[Any, List[Any]])
}
