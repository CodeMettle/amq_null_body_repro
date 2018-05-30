package codemettle

import akka.actor.ActorSystem

trait HasSystem {
  def system: ActorSystem
}
