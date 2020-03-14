package trafficlightscontrol.system

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import trafficlightscontrol.model.{Config, Event}

object EventPersisterActor {
  def props()(implicit config: Config): Props = Props(new EventPersisterActor())
}

class EventPersisterActor extends PersistentActor with ActorLogging  {

  override def receiveRecover: Receive = {

    case _ =>  log.info("Received recover default...")// handle recovery here
  }

  override def receiveCommand: Receive = {
    case ev: Event => {

      log.info("Received event [{}]. Persisting...",ev)

      persistAsync(s"evt-$ev-1") { e =>
        log.info("Persisted async event [{}]",e)
      }
    }
  }

  override def persistenceId: String = "my-stable-persistence-id"
}
