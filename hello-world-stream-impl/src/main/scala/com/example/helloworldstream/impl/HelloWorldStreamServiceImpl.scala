package com.example.helloworldstream.impl

import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.example.helloworldstream.api.HelloWorldStreamService
import com.example.helloworld.api.HelloWorldService

import scala.concurrent.Future

/**
  * Implementation of the HelloWorldStreamService.
  */
class HelloWorldStreamServiceImpl(helloWorldService: HelloWorldService) extends HelloWorldStreamService {
  def stream = ServiceCall { hellos =>
    Future.successful(hellos.mapAsync(8)(helloWorldService.hello(_).invoke()))
  }
}
