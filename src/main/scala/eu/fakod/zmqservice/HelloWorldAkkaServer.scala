package eu.fakod.zmqservice

import akka.serialization.SerializationExtension
import akka.zeromq._
import akka.routing.RoundRobinRouter
import akka.actor.{Props, ActorSystem, ActorRef, Actor}

case class ServiceResult(time: Long)

case class ServiceCall(time: Long)


class WorkerActor extends Actor {

  val repSocket = context.system.newDealerSocket(Array(Connect("inproc://workers"), Listener(self)))

  val ser = SerializationExtension(context.system)


  def receive = {

    case msg: ZMQMessage if (new String(msg.frames(1).payload.toArray, "UTF-8") == "ServiceCall") => {

      val serviceCall = ser.deserialize(msg.frames(2).payload.toArray, classOf[ServiceCall]) match {
        case Left(e) => throw e
        case Right(o) => o.asInstanceOf[ServiceCall]
      }

      val v = ser.serialize(ServiceResult(serviceCall.time)).right.get
      repSocket ! ZMQMessage(Seq(msg.frames(0), Frame("ServiceResult"), Frame(v)))
    }

  }
}

class DealerActor extends Actor {

  def receive = {
    case msg@ZMQMessage(resp) =>
      Server.routerSocket ! msg
  }
}


class RouterActor extends Actor {

  val ser = SerializationExtension(context.system)

  def receive = {
    case msg@ZMQMessage(resp) =>
      Server.backendSocket ! msg
  }
}


object Server {

  var routerSocket: ActorRef = _
  var backendSocket: ActorRef = _

  def start {

    val system = ActorSystem("AkkaServerSystem")
    val router = system.actorOf(Props[RouterActor], name = "router")
    val dealer = system.actorOf(Props[DealerActor], name = "dealer")

    // bind the router where the clients will connect to
    routerSocket = system.newRouterSocket(Array(Bind(forBind), Listener(router)))

    // bind the dealer socket where the workers will connect to get work
    backendSocket = system.newDealerSocket(Array(Bind("inproc://workers"), Listener(dealer)))

    // start up our worker service who will do the REP to the dealer
    // I'm also tried SmallestMailbox Router with no success
    val worker = system.actorOf(Props[WorkerActor].withRouter(RoundRobinRouter(nrOfInstances = 5)), name = "worker")
  }
}
