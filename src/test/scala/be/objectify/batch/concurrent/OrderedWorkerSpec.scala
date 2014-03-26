package be.objectify.batch.concurrent

import akka.actor.{ActorSelection, Props, ActorSystem}
import org.specs2.matcher.MustMatchers
import akka.testkit.{TestKit, ImplicitSender}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import be.objectify.batch.concurrent.Protocol.CheckForFinish

/**
 * 
 */
class OrderedWorkerSpec extends TestKit(ActorSystem("WorkerSpec")) with ImplicitSender
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

  "Sequenced worker" should {
    "work" in {
      val onFinishListener = new OnFinishListener() {
        def jobFinished(processed: Int, errors: Int): Unit = {}
      }
      val resultListener = system.actorOf(Props(classOf[TestResultListener], onFinishListener, "ordered"), "sequencedResultListener")
      val master = system.actorOf(Props(classOf[Master], resultListener), "sequencedMaster")
      system.actorOf(Props(classOf[TestProducer], master, resultListener), "producer")

      consumer("sequencedMaster", 1)

      resultListener ! self
      master ! CheckForFinish
      expectMsg("do")
      expectMsg("re")
      expectMsg("mi")
      expectMsg("fa")
      expectMsg("sol")
    }
  }
}