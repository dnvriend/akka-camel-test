/*
 * Copyright 2016 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dnvriend.camel.rx

import akka.actor.ActorLogging
import akka.actor.Status.Failure
import akka.camel.{ Ack, CamelMessage, Consumer }
import akka.event.LoggingReceive
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{ Cancel, Request }

object AkkaCamelPublisher {
  final val CamelComponentRef = "AKKA_CAMEL_PUBLISHER_CAMEL_COMPONENT_REF"
}

class AkkaCamelPublisher(override val endpointUri: String) extends Consumer with ActorPublisher[CamelMessage] with DeliveryBuffer[CamelMessage] with ActorLogging {
  import AkkaCamelPublisher._
  override val autoAck: Boolean = false

  override def receive: Receive = LoggingReceive {
    case msg: CamelMessage if buf.isEmpty && totalDemand > 0 ⇒
      println("--> Buffer is empty, storing camel message: " + msg + ", and if there is demand, sending message")
      buf :+= msg.copy(headers = msg.headers + (CamelComponentRef -> sender()))
      deliverBuf()

    case Request(demand) ⇒
      println("--> Received Request(" + demand + "), delivering buffer, totalDemand: " + totalDemand)
      deliverBuf()

    case Cancel ⇒
      println("--> Received cancel, stopping self")
      context.stop(self)

    case msg ⇒
      println("--> Sending Failure: totalDemand: " + totalDemand + ", msg:" + msg)
      sender() ! Failure(new IllegalStateException("Not accepting new messages"))
  }
}
