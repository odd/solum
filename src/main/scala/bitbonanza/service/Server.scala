package bitbonanza
package service

import akka.actor.{Props, Actor, ActorSystem}
import spray.routing._
import akka.pattern._
import akka.util.Timeout
import scala.concurrent.duration._
import com.github.mauricio.async.db.{RowData, Configuration, Connection}
import com.github.mauricio.async.db.mysql.MySQLConnection

object Server extends App with SimpleRoutingApp {
  implicit val actorSystem = ActorSystem("solum-server")
  implicit val timeout = Timeout(1.second)
  import actorSystem.dispatcher

  val countingActor = actorSystem.actorOf(Props(new DbCountingActor()))

  startServer(interface = "0.0.0.0", port = 8080) {
    // definition of how the server should behave when a request comes in

    // simplest route, matching only GET /hello, and sending a "Hello World!" response
    get {
      path("hello") {
        complete {
          "Hello World 2."
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

  class DbCountingActor extends Actor {
    val configuration = Configuration(
        username = System.getProperty("RDS_USERNAME", "missing"),
        //host = System.getProperty("RDS_HOSTNAME", "192.168.59.103"),
        host = System.getProperty("RDS_HOSTNAME", "missing"),
        port = System.getProperty("RDS_PORT", "3306").toInt,
        password = Option(System.getProperty("RDS_PASSWORD", "missing")),
        database = Option(System.getProperty("RDS_DB_NAME", "missing"))
    )
    val connection: Connection = {
      println(s"Creating MySQL connection from configuration [$configuration].")
      new MySQLConnection(configuration)
    }

    override def receive = {
      case Get(counterName) =>
        println(s"Getting counter $counterName")
        val client = sender
        println(s"Querying counter: select amount from counter where name = '$counterName'")
        connection.sendPreparedStatement("select amount from counter where name = ?", Seq(counterName)).map { qr =>
          qr.rows.flatMap { rs =>
            val headOption: Option[RowData] = rs.headOption
            headOption.flatMap { rd =>
              rd.headOption
            }
          }.map { d =>
            d.asInstanceOf[Int]
          }.getOrElse(0)
        } pipeTo client
      case Add(counterName, amount) =>
        println(s"Adding $amount to $counterName")
        connection.inTransaction { c =>
          println(s"Updating counter: update counter set amount = amount + $amount where name = '$counterName'")
          c.sendPreparedStatement("update counter set amount = amount + ? where name = ?", Seq(amount, counterName)).map { qr =>
            if (qr.rowsAffected == 0) {
              println(s"Inserting new counter: insert into counter (name, amount) values ('$counterName', $amount)")
              c.sendPreparedStatement("insert into counter (name, amount) values (?, ?)", Seq(counterName, amount))
            }
          }
        }
    }
  }

  // messages to communicate with the counters actor
  case class Get(counterName: String)
  case class Add(counterName: String, amount: Int)
}