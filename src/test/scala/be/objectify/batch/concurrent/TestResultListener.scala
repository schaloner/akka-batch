package be.objectify.batch.concurrent

import akka.actor.ActorRef

/**
 *
 * @param onFinishListener
 * @param workType
 */
class TestResultListener(onFinishListener: OnFinishListener, workType: String) extends ResultListener(onFinishListener) {

  var probe: Option[ActorRef] = None

  def onSuccess(key: Any, work: Any, message: String): Unit = probe match {
    case Some(actor) => actor ! work
    case _ =>
  }


  def onError(key: Any, work: Any, message: String): Unit = probe match {
    case Some(actor) => actor ! work
    case _ =>
  }

  override def onCustomMessage(message: Any):Unit = message match {
    case (actor: ActorRef) => probe = Some(actor)
    case msg => unhandled(msg)
  }

  override def parameters: Map[Any, Any] = {
    Map("workType" -> workType)
  }
}
