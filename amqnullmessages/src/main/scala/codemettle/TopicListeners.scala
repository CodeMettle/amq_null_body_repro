package codemettle

import akka.actor.{ActorRef, ActorSystem, Props}
import codemettle.TopicListeners.TopicListener
import com.codemettle.reactivemq.TopicConsumer
import com.codemettle.reactivemq.model.{AMQMessage, Topic}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object TopicListeners {
  class TopicListener(logNulls: Boolean, val consumeFrom: Topic) extends TopicConsumer {
    private lazy val logger = Logger(LoggerFactory.getLogger(consumeFrom.toString))

    override def connection: ActorRef = esbConnection

    override def receive: Receive = {
      case m@AMQMessage(null, _, _) if logNulls =>
        logger.error(s"Got NULL message body: $m")

      case m => logger.trace(s"got message $m")
    }
  }
}

class TopicListeners(val system: ActorSystem, logNulls: Boolean) extends HasSystem {
  system.actorOf(Props(new TopicListener(logNulls, Topic(Topic1))))
  system.actorOf(Props(new TopicListener(logNulls, Topic(Topic2))))
  system.actorOf(Props(new TopicListener(logNulls, Topic(Topic3))))
}
