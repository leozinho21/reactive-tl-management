package trafficlightscontrol.system

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import trafficlightscontrol.http.publishers
import trafficlightscontrol.model._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object TrafficLightActor {
  def props(id : String)(implicit config: Config): Props = Props(new TrafficLightActor(id))
}

class TrafficLightActor(id : String)(implicit config: Config) extends Actor with ActorLogging {

  import scala.concurrent.duration._

  implicit val executionContext = context.system.dispatcher

  val statePublisher = new publishers.ReactivePublisher[StateChange]()(context.system)

  private var state : LightState = RedLight;

  override def preStart(): Unit = {
    schedule(config.tickTimeout, config.tickInterval, publish(TickEventTL(id, calculateDensity)))
  }

      // each call should be higher than previous,unless its reseted after turning to green
      def calculateDensity: Double = {
        val min = 1
        val max = 6

        val r = new scala.util.Random

        min + (max - min) * r.nextDouble
      }

      def schedule(timeout: FiniteDuration, interval: FiniteDuration, f: => Unit): Cancellable =
        context.system.scheduler.schedule(timeout, interval)(f)

      override def receive: Receive ={

        case e @ StateChange(_, RedLight)    => receiveRedLight(e.duration,e.state)
        case e @ StateChange(_, OrangeLight) => receiveOrangeLight(e.duration,e.state)
        case e @ StateChange(_, GreenLight)  => receiveGreenLight(e.duration,e.state)
        case e @ _                           => log.info("Received msg {}", e)
      }

  private var delayTask: Cancellable = _

  def scheduleDelay(delay: FiniteDuration, f: => Unit): Unit =
    delayTask = context.system.scheduler.scheduleOnce(delay)(f)

  private def receiveRedLight(duration : FiniteDuration, nextState: LightState): Unit =

    state match {
          case GreenLight => {
            scheduleDelay(0.seconds, self ! StateChange(config.orangeInterval, OrangeLight))
          }
          case _ => {
            scheduleDelay(duration, changeStateTo(nextState))
          }

    }



  def changeStateTo(newState: LightState) = {
    log.info("[{}] State changed to {}...", id, newState)

    this.state = newState

    publish(StateChangedEvent(id, newState))
  }

  final def publish(event: Event): Unit =
    context.system.eventStream.publish(event)

  private def receiveOrangeLight(duration : FiniteDuration, state: LightState): Unit = {

        changeStateTo(state);
        log.info("[{}] Going red in {}...", id, duration)
        scheduleDelay(duration, self ! StateChange(0.seconds, RedLight))
  }

  private def receiveGreenLight(duration : FiniteDuration, state: LightState): Unit = {

        changeStateTo(state);

        log.info("{} before going orange...", duration - config.orangeInterval)

        scheduleDelay(duration - config.orangeInterval, self ! StateChange(config.orangeInterval, RedLight))
  }
}
