package bitbonanza
package service

import akka.actor.{Props, Actor, ActorSystem}
import spray.routing._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

object Server extends App with SimpleRoutingApp {
  implicit val actorSystem = ActorSystem("solum-server")
  implicit val timeout = Timeout(1.second)
  import actorSystem.dispatcher

  val countingActor = actorSystem.actorOf(Props(new CountingActor()))

  startServer(interface = "0.0.0.0", port = 8080) {
    // definition of how the server should behave when a request comes in

    // simplest route, matching only GET /hello, and sending a "Hello World!" response
    get {
      path("hello") {
        complete {
          "Hello World!"
        }
      }
    } ~ // the ~ concatenates two routes: if the first doesn't match, the second is tried
    path("counter" / Segment) { counterName =>  // extracting the second path component into a closure argument
      get {
        complete {
          (countingActor ? Get(counterName)) // integration with futures
            .mapTo[Int]
            .map(amount => s"$counterName is: $amount")
        }
      } ~
      post {
        parameters("amount".as[Int]) { amount => // the type of the amount closure argument is Int, as specified!
          countingActor ! Add(counterName, amount) // fire-and-forget
          complete {
            "OK"
          }
        }
      }
    }
  }

  // implementation of the actor
  class CountingActor extends Actor {
    private var counters = Map[String, Int]()

    override def receive = {
      case Get(counterName) => sender ! counters.getOrElse(counterName, 0)
      case Add(counterName, amount) =>
        val newAmount = counters.getOrElse(counterName, 0) + amount
        counters = counters + (counterName -> newAmount)
    }
  }

  // messages to communicate with the counters actor
  case class Get(counterName: String)
  case class Add(counterName: String, amount: Int)
}