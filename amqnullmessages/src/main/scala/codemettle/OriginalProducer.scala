package codemettle

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import codemettle.OriginalProducer.PeriodicProducer
import com.codemettle.reactivemq.model.{AMQMessage, Destination, Topic}
import com.codemettle.reactivemq.{Oneway, Producer}
import upickle.default._

import scala.concurrent.duration._
import scala.util.Random

object OriginalProducer {
  class ProducerActor extends Producer with Oneway {
    override def connection: ActorRef = esbConnection

    override def destination: Destination = Topic(SplitterTopic)
  }

  class PeriodicProducer extends Actor {
    import context.dispatcher

    private val producer = context.actorOf(Props(new ProducerActor))

    private val timer = context.system.scheduler.schedule(1.second, 1.second, self, 'produce)

    override def postStop(): Unit = {
      super.postStop()

      timer.cancel()
    }

    private def sendChunkOfType(msgType: String): Unit = {
      val msgs = (1 to 30) map { _ =>
        val msgLen = 1000 + Random.nextInt(1000)
        Random.alphanumeric.take(msgLen).mkString("")
      }

      val json = write(msgs)

      val msg = AMQMessage(json, headers = Map("JMSXGroupID" -> Int.box(Random.nextInt(256)))).withType(msgType)

      producer ! msg
    }

    override def receive: Receive = {
      case 'produce => Random.shuffle(allTypes).foreach(sendChunkOfType)
    }
  }
}

class OriginalProducer(val system: ActorSystem) extends HasSystem {
  system.actorOf(Props(new PeriodicProducer))
}
