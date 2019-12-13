package com.example.dsb

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.Failure
import scala.util.Success

private object QuickstartApp {

  private val bindAddress = "localhost"
  private val bindPort = 8080

  private def startHttpServer(routing: Route, actorSystem: ActorSystem[_]): Unit = {

    // Akka HTTP still needs a classic ActorSystem to start
    implicit val classicActorSystem: akka.actor.ActorSystem =
      actorSystem.toClassic
    import actorSystem.executionContext

    //???DSB: Might try using second port for health check
    //  (https://stackoverflow.com/questions/39391387/using-akka-http-for-multiple-bindings)

    val futureBinding = Http().bindAndHandle(routing, bindAddress, bindPort)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        actorSystem.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        actorSystem.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        actorSystem.terminate()
    }
  }

  def main(args: Array[String]): Unit = {

    val rootBehavior = Behaviors.setup[Nothing] { actorContext =>

      val userRegistryActor = actorContext.spawn(UserRegistry(), "UserRegistryActor")
      actorContext.watch(userRegistryActor)

      val userRegistryRouting = new UserApiRouting(userRegistryActor)(actorContext.system)
      startHttpServer(userRegistryRouting.userRoutes, actorContext.system)

      Behaviors.empty
    }
    val actorSystem = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }
}
