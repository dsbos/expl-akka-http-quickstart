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

  final case class GetUsers(replyTarget: ActorRef[Users]) extends Command
  final case class CreateUser(user: User, replyTarget: ActorRef[ActionConfirmation]) extends Command
  final case class GetUser(name: String, replyTarget: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(name: String, replyTarget: ActorRef[ActionConfirmation]) extends Command

  /* Response messages from user-registry actor and/or high-level DTO to HTTP
     response encoding. */
  final case class GetUserResponse(maybeUser: Option[User])
  final case class Users(users: immutable.Seq[User])
  final case class ActionConfirmation(description: String)

  /** Gets behavior of registry with given set of users. */
  private def getRegistryBehavior(givenRegistryState: Set[User]): Behavior[Command] =
    Behaviors.receiveMessage {

      case GetUsers(replyTarget) =>
        replyTarget ! Users(givenRegistryState.toSeq)
        Behaviors.same

      case CreateUser(user, replyTarget) =>
        replyTarget ! ActionConfirmation(s"User ${user.name} created.")
        getRegistryBehavior(givenRegistryState + user)

      case GetUser(name, replyTarget) =>
        replyTarget ! GetUserResponse(givenRegistryState.find(_.name == name))
        Behaviors.same

      case DeleteUser(name, replyTarget) =>
        replyTarget ! ActionConfirmation(s"User $name deleted.")
        getRegistryBehavior(givenRegistryState.filterNot(_.name == name))
    }

  /** Constructs initial behavior (that of empty user registry). */
  def apply(): Behavior[Command] = getRegistryBehavior(Set.empty)
}
