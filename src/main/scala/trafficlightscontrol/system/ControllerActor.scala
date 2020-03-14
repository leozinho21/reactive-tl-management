package trafficlightscontrol.system

import akka.actor._
import trafficlightscontrol.model._
import trafficlightscontrol.system.Application.system

import scala.collection.immutable.ListMap

object ControllerActor {
  def props(name: String)(implicit config: Config): Props = Props(new ControllerActor(name))
}
class ControllerActor(name: String)(implicit val config: Config) extends Actor with ActorLogging {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  lazy val map: Map[String, ActorRef] = Map(
                  "tl1" -> system.actorOf(TrafficLightActor.props("tl1") ),
                  "tl2" -> system.actorOf(TrafficLightActor.props("tl2") ),
                  "tl3" -> system.actorOf(TrafficLightActor.props("tl3") ),
                  "tl4" -> system.actorOf(TrafficLightActor.props("tl4") ))

  var densityMap: scala.collection.mutable.Map[String, Double] = scala.collection.mutable.Map()

  private lazy val persisterActor = system.actorOf(EventPersisterActor.props(),"persister");

  var greenId : String = _;

  override def receive: Receive = {

    case ev @ StartSystem() => {
      greenId = config.initialTLid;

      log.info("System starting... Initially green traffic actor [{}}] for [{}]",greenId,config.greenInterval)

      map (greenId) ! StateChange(config.greenInterval, GreenLight)

      persistEvent(ev)
    }

    case ev @ TickEventTL(id, density) => {

      if(densityMap.contains(id)){
        val newD = densityMap (id)+ density
        densityMap += (id -> newD)
      }
      else densityMap += (id -> density)

      log.info("TickEvent from [{}] -> [{}]",id, densityMap (id))
    }

    case ev @ StateChangedEvent(id,state) => {

      state match {
          case OrangeLight => if(id == greenId) {

            log.info("[{}] state is Orange, peeking next green",id)

            val next = nextGreen
            log.info("Next green actor {} in [{}]", next,config.orangeInterval)

            scheduleDelay(config.orangeInterval, {
              densityMap += (next -> 0.0); // reset its density
              greenId = next;
              map ( next ) ! StateChange(config.greenInterval, GreenLight)
            })

            persistEvent(ev)
          }
          case _ => {
            persistEvent(ev)
            log.info("[{}] state is [{}] now...",id,state)
          }


      }


    }
    case e @ _ =>{
      log.info("Default case {}", e)
    }

  }

  def persistEvent[E <: Event](ev : E): Unit = {

    persisterActor ! ev

  }

  def nextGreen : String = {
    //sort descending
     ListMap(densityMap.toSeq.sortWith(_._2 > _._2): _*).head._1
  }

  context.system.eventStream.subscribe(self, classOf[TickEventTL])
  context.system.eventStream.subscribe(self, classOf[StateChangedEvent])

  def scheduleMessage(timeout: FiniteDuration, dest: ActorRef, msg: Any): Cancellable =
    context.system.scheduler.scheduleOnce(timeout, dest, msg)(context.system.dispatcher)

  def scheduleDelay(delay: FiniteDuration, f: => Unit): Unit =
     context.system.scheduler.scheduleOnce(delay)(f)
}
