package codemettle

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.{AskTimeoutException, ask}
import akka.util.Timeout
import codemettle.Splitter.ListenActor
import com.codemettle.reactivemq.ReActiveMQMessages.SendMessage
import com.codemettle.reactivemq.TopicConsumer
import com.codemettle.reactivemq.model._
import com.typesafe.scalalogging.StrictLogging
import upickle.default._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Splitter {
  private class ListenActor extends TopicConsumer with StrictLogging {
    import context.dispatcher

    override def connection: ActorRef = esbConnection

    override def consumeFrom: Topic = Topic(SplitterTopic)

    private val topic1 = Topic(Topic1)
    private val topic2 = Topic(Topic2)
    private val topic3 = Topic(Topic3)

    private def sendTopicMessage(dest: Destination, msg: AMQMessage)(implicit timeout: FiniteDuration) = {
      val request = SendMessage(dest, msg, timeout = timeout)

      logger.trace(s"Sending $msg")

      implicit val to: Timeout = Timeout(timeout + 3.seconds)

      (esbConnection ? request) transform(_ ⇒ {}, {
        case _: AskTimeoutException ⇒ new Exception("Timed out while waiting for SendMessage Ack")
        case t ⇒ t
      })
    }

    private def sendBulkTopicMessage(dest: Destination, msg: AMQMessage)(implicit timeout: FiniteDuration) = {
      val msgs = read[Seq[String]](msg.bodyAs[String])
      Future.sequence(msgs.map(j ⇒ sendTopicMessage(dest, msg.copy(body = j)))).map(_ ⇒ {})
    }

    private def doOp(msg: AMQMessage)(f: ⇒ Future[Unit]): Unit = {
      val resF = f recover {
        case t ⇒ logger.error(s"Error while processing $msg", t)
      }

      Await.result(resF, Duration.Inf)
    }

    private implicit val timeout: FiniteDuration = 30.seconds

    override def receive: Receive = {
      case msg: AMQMessage ⇒
        val msgCopy = msg.copy(properties = JMSMessageProperties(`type` = msg.properties.`type`))

        msg.properties.`type` match {
          case None ⇒ logger.warn(s"Invalid message: $msg")

          case Some(MsgType1) ⇒ doOp(msg) {
            sendBulkTopicMessage(topic1, msgCopy.withType(MsgType1))
          }

          case Some(MsgType2) ⇒ doOp(msg) {
            sendBulkTopicMessage(topic2, msgCopy.withType(MsgType2))
          }

          case Some(MsgType3) ⇒ doOp(msg) {
            sendBulkTopicMessage(topic3, msgCopy.withType(MsgType3))
          }

          case Some(_) ⇒ logger.warn(s"Invalid JMSType: $msg")
        }
    }
  }

}

class Splitter(val system: ActorSystem) extends HasSystem {
  system.actorOf(Props(new ListenActor))
}
