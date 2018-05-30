package codemettle

import akka.actor.{ActorRef, ActorSystem, Props}
import codemettle.TopicListeners.TopicListener
import com.codemettle.reactivemq.TopicConsumer
import com.codemettle.reactivemq.model.{AMQMessage, Topic}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object TopicListeners {
  class TopicListener(doLogging: Boolean, val consumeFrom: Topic) extends TopicConsumer {
    private lazy val logger = Logger(LoggerFactory.getLogger(consumeFrom.toString))

    override def connection: ActorRef = esbConnection

    override def receive: Receive = {
      case m@AMQMessage(null, _, _) if doLogging =>
        logger.error(s"Got NULL message body: $m")

      case m if doLogging => logger.trace(s"got message $m")
      
      case _ => // ignore
    }
  }
}

class TopicListeners(val system: ActorSystem, doLogging: Boolean) extends HasSystem {
  system.actorOf(Props(new TopicListener(doLogging, Topic(Topic1))))
  system.actorOf(Props(new TopicListener(doLogging, Topic(Topic2))))
  system.actorOf(Props(new TopicListener(doLogging, Topic(Topic3))))
}
