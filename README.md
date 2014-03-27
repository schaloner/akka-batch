akka-batch
==========

A batch processing framework using Scala and Akka


How do I use it?
================
There are four types that you need to implement in order to use akka-batch.  These are

* Consumer - accepts a work item and processes it
* Producer - produces work
* EventListener - listens to the results of individual work items, and for lifecycle events
* OnFinishListener - listens for the end of the job

Consumer
--------
Consumers accept a single item of work at a time, and process that item.  You can choose whether this is asynchronous or not.

**Asynchonous example**
Everything here happens in a Future.


    class TestConsumer(master: ActorSelection) extends Consumer(master) {
      implicit val ec = context.dispatcher

      override def doWork(listener: ActorRef, key: Any, work: Any): Future[WorkComplete] = {
        import akka.pattern.pipe
        Future {
          // do some work
          // ...
          // Notify the listener of success or failure by using WorkSuccess or WorkError
          listener ! WorkSuccess(key, work, "ok")
          // Return WorkComplete to indicate the processing is finished
          WorkComplete(key, "done", successful = true)
        } pipeTo self
      }
    }

**Synchonous example**
Do some work directly in the consumer, and then use a Future to send lifecycle messages

    class MyConsumer(master: ActorSelection) extends Consumer(master) {
      implicit val ec = context.dispatcher

      override def doWork(listener: ActorRef, key: Any, work: Any): Future[WorkComplete] = {
        // do some work
        // ...
        import akka.pattern.pipe
        Future {
          // Notify the listener of success or failure by using WorkSuccess or WorkError
          listener ! WorkSuccess(key, work, "ok")
          // Return WorkComplete to indicate the processing is finished
          WorkComplete(key, "done", successful = true)
        } pipeTo self
      }
    }


Producer
--------
Producers load work in batches.  The batch size is up to you, so it could be a handful of items at a time, or the entire workload.  In this example, a hardcoded limit of 5 items is set.

    class MyProducer(master: ActorRef, resultListener: ActorRef) extends Producer(master, resultListener) {

      import Protocol._
      import context._

      val max = 5

      def hasMoreWork(processed: Int, errors: Int, parameters: Map[Any, Any]): Future[Any] = {
        Future[Any] {
          processed match {
          	// use the hardcoded limit.  In reality, this could be based on a database call, or...
            case p if p < max => MoreWorkAvailable
            case _ => NoRemainingWork
          }
        }
      }

      def getWork(processed: Int, errors: Int, parameters: Map[Any, Any]): Future[Work] = {
        Future[Work] {
          // Return 5 items that must be processed in order.  You could also return
          // unordered work by having Lists that contain a single item, e.g.
          // Work(immutable.Map[Any, List[Any]]("a" -> List("do"), "b" -> List("re"), "c" -> List("mi"), "d" -> List("fa"), "e" -> List("sol")))
          Work(immutable.Map[Any, List[Any]]("a" -> List("do", "re", "mi", "fa", "sol")))
        }
      }
    }


EventListener
--------------
A EventListener listens for notifications of success or error on a processed item, if the queue empty, if the job has finished, etc.

    class MyEventListener(onFinishListener: OnFinishListener) extends EventListener(onFinishListener) {

      def onSuccess(key: Any, work: Any, message: String): Unit = {
        // An item was processed successfully
      }

      def onError(key: Any, work: Any, message: String): Unit = {
        // An item was processed unsuccessfully
      }

      override def onCustomMessage(message: Any):Unit = {
        // An application-specific message was received
      }
    }


OnFinishListener
----------------
This listener is called when all work has been processed and the Producer reports that no more work is available.

    class MyOnFinishListener extends OnFinishListener {

      def jobFinished(processed: Int, errors: Int): Unit = {
        // processing is finished
      }
    }

References
==========

Based on http://letitcrash.com/post/29044669086/balancing-workload-across-nodes-with-akka-2