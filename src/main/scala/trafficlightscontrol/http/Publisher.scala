package trafficlightscontrol.http

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object publishers {

  import org.reactivestreams.{Publisher, Subscriber}

  final class ReactivePublisher[T](implicit system: ActorSystem) extends Publisher[T] {

    private[this] val worker = system.actorOf(Props(new PublisherActorWorker))

    override def subscribe(subscriber: Subscriber[_ >: T]): Unit =
      Option(subscriber)
        .map(_ => Subscription(subscriber, worker, all))
        .getOrElse(throw new NullPointerException)

    def publish(element: T): Unit = worker ! Publish(element)

    case class Cancel(s: Subscription)
    case class Subscribe(s: Subscription, predicate: T => Boolean)
    case class Demand(s: Subscription, n: Long)
    case class Publish(element: T)

    final def all: T => Boolean = (_: T) => true

    final case class Subscription(
      subscriber: Subscriber[_ >: T],
      worker: ActorRef,
      predicate: T => Boolean,
      var cancelled: Boolean = false)
        extends org.reactivestreams.Subscription {

      override def cancel(): Unit = worker ! Cancel(this)
      override def request(n: Long): Unit = worker ! Demand(this, n)

      private var demand: Long = 0
      private var buffer: Vector[T] = Vector()

      private[publishers] def addDemand(n: Long) =
        if (n > 0) {
          demand = demand + n
          if (demand < 0) demand = 0
          if (buffer.size > 0) {
            val min = Math.min(buffer.size, demand).toInt
            val (send, stay) = buffer.splitAt(min)
            buffer = stay
            send foreach push
          }
        }

      private[publishers] def push(element: T): Unit =
        if (demand > 0) {
          subscriber.onNext(element)
          demand = demand - 1
        } else {
          buffer = buffer :+ element
        }
      println("Creating reactive subscription actor")

      worker ! Subscribe(this, predicate)
    }

    private[this] final class PublisherActorWorker extends Actor {

      var subscriptions: Vector[Subscription] = Vector()

      def receive: Receive = {
        case Subscribe(s, _) =>
          subscriptions.find(_.subscriber == s.subscriber).getOrElse {
            subscriptions = subscriptions :+ s
            s.subscriber.onSubscribe(s)
          }
        case Cancel(s) if !s.cancelled =>
          subscriptions = subscriptions.filterNot(_ == s)
          s.cancelled = true
        case Cancel(s) if s.cancelled =>
        case Demand(s, n) if !s.cancelled && n > 0 =>
          s.addDemand(n)
        case Demand(s, _) if s.cancelled =>
        case Publish(element) =>
          subscriptions.foreach(s => if (s.predicate(element)) s.push(element))
        case _ =>
      }

    }

  }
}
