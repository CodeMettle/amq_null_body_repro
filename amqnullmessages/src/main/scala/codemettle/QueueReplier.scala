package codemettle

import akka.actor.{ActorRef, ActorSystem, Props}
import codemettle.QueueReplier.Reply
import com.codemettle.reactivemq.QueueConsumer
import com.codemettle.reactivemq.model.{AMQMessage, Queue}

import scala.concurrent.duration._
import scala.util.Random

object QueueReplier {
  private case class Reply(sender: ActorRef)
}

class QueueReplier extends QueueConsumer {
  import context.dispatcher

  override def connection: ActorRef = esbConnection

  override def consumeFrom: Queue = Queue("aQueue")

  override def receive: Receive = {
    case _: AMQMessage =>
      val delay = (10 + Random.nextInt(90)).millis
      val msg = Reply(sender())
      context.system.scheduler.scheduleOnce(delay, self, msg)

    case Reply(to) => to ! "ok"
  }
}

class QueueReplierSystem(val system: ActorSystem) extends HasSystem {
  system.actorOf(Props(new QueueReplier))
}
