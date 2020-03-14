package trafficlightscontrol.model

import scala.concurrent.duration._
import akka.actor.ActorRef
import trafficlightscontrol.system.TrafficLightActor

trait Message
trait Command extends Message
trait Event extends Message

case class Config(initialTLid: String,
                  greenInterval: FiniteDuration,
                  orangeInterval: FiniteDuration,
                  tickTimeout: FiniteDuration,
                  tickInterval : FiniteDuration) extends Message

case class StartSystem() extends Event
case class TickEventTL(id: String, density: Double) extends Event
case class StateChange(duration: FiniteDuration, state: LightState) extends Command


case class RegisterRecipientCommand(director: ActorRef) extends Command
case class RecipientRegisteredEvent(id: Id) extends Event


case class StateChangedEvent(id: Id, state: LightState) extends Event


case object TickEvent extends Event
case object TimeoutEvent extends Event
case class MessageIgnoredEvent(message: Message) extends Event
