import akka.actor.{ActorContext, ActorRef, ActorRefFactory, ActorSystem}
import com.codemettle.reactivemq.ReActiveMQExtension
import com.typesafe.config.ConfigFactory

package object codemettle {
  final val SplitterTopic = "splitter"

  final val Topic1 = "topic1"
  final val Topic2 = "topic2"
  final val Topic3 = "topic3"

  final val MsgType1 = "msgType1"
  final val MsgType2 = "msgType2"
  final val MsgType3 = "msgType3"

  final val allTypes = List(MsgType1, MsgType2, MsgType3)

  def topicForType(msgType: String): String = msgType match {
    case MsgType1 => Topic1
    case MsgType2 => Topic2
    case MsgType3 => Topic3
    case other => sys.error(s"invalid topic $other")
  }

  def amqConfigStr(addr: String): String =
    s"""reactivemq {
       |  autoconnect {
       |    amq {
       |      address = "$addr"
       |    }
       |  }
       |}
     """.stripMargin

  def configuredActSys(name: String, addr: String) =
    ActorSystem(name, ConfigFactory.parseString(amqConfigStr(addr)).withFallback(ConfigFactory.load()))

  def vmActSys(name: String): ActorSystem = configuredActSys(name, "vm://localhost?create=false")
  def tcpActSys(name: String): ActorSystem = configuredActSys(name, "nio://localhost:61616")

  def actorSystem(implicit arf: ActorRefFactory): ActorSystem = arf match {
    case sys: ActorSystem => sys
    case ctx: ActorContext => ctx.system
    case _ => sys.error(s"unsupported $arf")
  }

  def esbConnection(implicit arf: ActorRefFactory): ActorRef =
    ReActiveMQExtension(actorSystem).autoConnects("amq")
}
