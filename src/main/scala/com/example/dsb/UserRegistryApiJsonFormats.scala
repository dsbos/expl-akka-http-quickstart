package com.example.dsb

import com.example.dsb.UserRegistry.{ActionPerformed, Users}

import spray.json.DefaultJsonProtocol

/** ???? codecs between user registry actor response messages and HTTP response bodies */
private object UserRegistryApiJsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat = jsonFormat3(User)
  implicit val usersJsonFormat = jsonFormat1(Users)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
