package com.example.helloworldstream.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.example.helloworldstream.api.HelloWorldStreamService
import com.example.helloworld.api.HelloWorldService
import com.softwaremill.macwire._

class HelloWorldStreamLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new HelloWorldStreamApplication(context) {
      override def serviceLocator: NoServiceLocator.type = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new HelloWorldStreamApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[HelloWorldStreamService])
}

abstract class HelloWorldStreamApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[HelloWorldStreamService](wire[HelloWorldStreamServiceImpl])

  // Bind the HelloWorldService client
  lazy val helloWorldService: HelloWorldService = serviceClient.implement[HelloWorldService]
}
