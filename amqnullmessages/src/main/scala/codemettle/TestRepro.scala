package codemettle

import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.{DisposableBean, InitializingBean}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class TestRepro extends StrictLogging with InitializingBean with DisposableBean {
  private var systems = Seq.empty[HasSystem]

  override def afterPropertiesSet(): Unit = {
    logger.info("Initializing")

    // create listeners on TCP transports to simulate other JVMs consuming
    systems ++= (for (i <- 1 to 3)
      yield new TopicListeners(tcpActSys(s"Remote$i"), logNulls = false))

    // create listeners on VM transports to duplicate listeners on routers in AMQ
    systems ++= (for (i <- 1 to 3)
      yield new TopicListeners(vmActSys(s"Local$i"), logNulls = i == 1))

    // setup queue on VM transport that responds to messages
    systems :+= new QueueReplierSystem(vmActSys("Replier"))

    // create splitter on VM transport that takes in messages, sends them to queue, splits the messages, and sends them
    // to various topics to be consumed by TopicListeners
    systems :+= new Splitter(vmActSys("Splitter"))

    // create sender on TCP transport simulate external JVM process sending items to the splitter
    systems :+= new OriginalProducer(tcpActSys("Producer"))

    logger.info("Initialization done")
  }

  override def destroy(): Unit = {
    logger.info("Shutting down")

    val termsF = Future.sequence(systems.map(_.system.terminate()))
    Await.ready(termsF, Duration.Inf)

    logger.info("Completed shutting down")
  }
}
