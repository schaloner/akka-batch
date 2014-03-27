package be.objectify.batch.concurrent

import akka.actor.{ActorSelection, Props, ActorSystem}
import org.specs2.matcher.MustMatchers
import akka.testkit.{TestKit, ImplicitSender}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import be.objectify.batch.concurrent.Protocol.CheckForFinish

/**
 * 
 */
class UnorderedWorkerSpec extends TestKit(ActorSystem("WorkerSpec")) with ImplicitSender
                                                            with WordSpecLike
                                                            with BeforeAndAfterAll
                                                            with MustMatchers {

  override def afterAll() {
    system.shutdown()
  }

  def consumer(masterName: String, count: Int) = {
    val master: ActorSelection = system.actorSelection(
      "akka://%s/user/%s".format(system.name, masterName))
    for (a <- 0 until count) system.actorOf(Props(new TestConsumer(master)))
  }

  "Worker" should {
    "work" in {
      val onFinishListener = new OnFinishListener() {
        def jobFinished(processed: Int, errors: Int): Unit = {}
      }
      val eventListener = system.actorOf(Props(classOf[TestEventListener], onFinishListener, "unordered"), "resultListener")
      val master = system.actorOf(Props(classOf[Master], eventListener), "master")
      system.actorOf(Props(classOf[TestProducer], master, eventListener), "producer")

      consumer("master", 1)

      eventListener ! self
      master ! CheckForFinish

      expectMsgAllOf("do", "re", "mi", "fa", "sol")
    }
  }
}