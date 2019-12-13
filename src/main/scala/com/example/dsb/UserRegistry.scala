package com.example.dsb

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable


/* Core data model object; also high-level DTO to HTTP response encoding.) */
final case class User(name: String, age: Int, countryOfResidence: String)

/** User-registry actor behavior (factory)? */  //??? revisit name
private object UserRegistry {

  /** Request messages to user-registry actors. */
  //????
  /** User-registry command (message _to_ user registry actor). */
  sealed trait Command

  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class CreateUser(user: User, xxreplyTo: ActorRef[ActionConfirmation]) extends Command
  final case class GetUser(name: String, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(name: String, replyTo: ActorRef[ActionConfirmation]) extends Command

  /* Response messages from user-registry actor and/or high-level DTO to HTTP
     response encoding. */
  final case class GetUserResponse(maybeUser: Option[User])
  final case class Users(users: immutable.Seq[User])
  final case class ActionConfirmation(description: String)

  /** Gets behavior of registry with given set of users. */
  private def getRegistryBehavior(givenRegistryState: Set[User]): Behavior[Command] =
    Behaviors.receiveMessage {

      case GetUsers(xxreplyTo) =>
        xxreplyTo ! Users(givenRegistryState.toSeq)
        Behaviors.same

      case CreateUser(user, xxreplyTo) =>
        xxreplyTo ! ActionConfirmation(s"User ${user.name} created.")
        getRegistryBehavior(givenRegistryState + user)

      case GetUser(name, xxreplyTo) =>
        xxreplyTo ! GetUserResponse(givenRegistryState.find(_.name == name))
        Behaviors.same

      case DeleteUser(name, xxreplyTo) =>
        xxreplyTo ! ActionConfirmation(s"User $name deleted.")
        getRegistryBehavior(givenRegistryState.filterNot(_.name == name))
    }

  /** Constructs initial behavior (that of empty user registry). */
  def apply(): Behavior[Command] = getRegistryBehavior(Set.empty)
}
