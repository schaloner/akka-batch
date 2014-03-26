package be.objectify.batch.concurrent

import akka.actor._

/**
 *
 * @param onFinishListener
 */
abstract class ResultListener(onFinishListener: OnFinishListener) extends Actor with ActorLogging {

  import Protocol._

  var processed = 0
  var errors = 0

  def onSuccess(key: Any, work: Any, message: String)

  def onError(key: Any, work: Any, message: String)

  def onJobFinished() = onFinishListener.jobFinished(processed, errors)

  override def receive = {
    case WorkSuccess(key, work, message) =>
      log.debug("[{}] Work item processed successfully: [{}]", key, message)
      processed = processed + 1
      onSuccess(key, work, message)
    case WorkError(key, work, message) =>
      log.debug("[{}] Work item processing failed: [{}]", key, message)
      processed = processed + 1
      errors = errors + 1
      onError(key, work, message)
    case WorkQueueEmpty =>
      log.debug("Work queue empty")
      val system = context.system
      onQueueEmpty(system.actorSelection(system.child("producer")))
    case MoreWorkAvailable =>
      log.debug("More work available, informing producer")
      val system = context.system
      onLoadWork(system.actorSelection(system.child("producer")))
    case QueryJobStatus =>
      onQueryJobStatus()
    case JobFinished =>
      log.debug("Job finished, informing listener")
      onJobFinished()
    case msg =>
        log.debug("Received a custom message [{}]", msg)
        onCustomMessage(msg)
  }

  def onQueryJobStatus() = sender() ! JobStatus(processed, errors)

  def onQueueEmpty(producer: ActorSelection) = {
    producer ! CheckForWork(processed, errors, parameters)
  }

  def onLoadWork(producer: ActorSelection) = {
    var params = parameters
    producer ! LoadWork(processed, errors, params)
  }

  def onCustomMessage(message: Any) = unhandled(message)

  def parameters: Map[Any, Any] = Map()
}
