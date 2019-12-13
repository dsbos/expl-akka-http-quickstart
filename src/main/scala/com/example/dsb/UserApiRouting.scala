package com.example.dsb

import com.example.dsb.UserRegistry._

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.Future


/**
 * Routing of user-registry ReST API to a given user-registry actor.
 */
private class UserApiRouting(userRegistryActor: ActorRef[UserRegistry.Command])(implicit val actorSystem: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import UserRegistryApiJsonFormats._

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout =
    Timeout.create(actorSystem.settings.config.getDuration("my-app.routes.ask-timeout"))

  // (each sends corresponding message to associated actor, returning future results)
  private def getUsers(): Future[Users] =
    userRegistryActor.ask(GetUsers)
  private def getUser(name: String): Future[GetUserResponse] =
    userRegistryActor.ask(GetUser(name, _))
  private def createUser(user: User): Future[ActionConfirmation] =
    userRegistryActor.ask(CreateUser(user, _))
  private def deleteUser(name: String): Future[ActionConfirmation] =
    userRegistryActor.ask(DeleteUser(name, _))

  val userRoutes: Route =
    pathPrefix("users") {
      concat(
        pathEnd {  // - /users (but not "/users/")
          concat(
            get {
              complete(getUsers())
            },
            post {
              entity(as[User]) { user =>
                onSuccess(createUser(user)) { actionConfirmation =>
                  complete((StatusCodes.Created, actionConfirmation))
                }
              }
            })
        },
        path(Segment) { userName =>  // - /users/<non-empty string, as "name">
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(getUser(userName)) { requestedUser =>
                  complete(requestedUser.maybeUser)
                }
              }
            },
            delete {
              onSuccess(deleteUser(userName)) { actionConfirmation =>
                complete((StatusCodes.OK, actionConfirmation))
              }
            })
        })
      //???DSB: see Remaining and RemainingPath (etc.); maybe Neutral
      //???DSB: https://stackoverflow.com/questions/41627747/how-do-i-access-the-full-path-of-a-request-in-akka-http-request - see extractMatchedPath and extractUnmatchedPath, or extractUri
      //???DSB: RequestContext.unmatchedPath

    }
}
