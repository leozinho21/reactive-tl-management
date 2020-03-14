package trafficlightscontrol.system

import akka.actor.{Actor, ActorSystem}

import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import trafficlightscontrol.model.{Config, StartSystem}

import scala.concurrent.duration._

object Application extends App {

  import system.dispatcher // for the future transformations
  implicit val futureTimeout = Timeout(10.seconds)

  implicit val system = ActorSystem("app")

  val conf = ConfigFactory.load()

  implicit val config = Config("tl1", 20.seconds,5.seconds,5.seconds,5.seconds)

  val controllerActor = system.actorOf(ControllerActor.props("controller") );
  controllerActor tell( new StartSystem, Actor.noSender)




  println(s"Started server press RETURN to stop...")
  scala.io.StdIn.readLine()

//  httpBinding
//    .flatMap(_.unbind()) // trigger unbinding from the port
//    .onComplete(_ ⇒ system.terminate()) // and terminate when done
}
