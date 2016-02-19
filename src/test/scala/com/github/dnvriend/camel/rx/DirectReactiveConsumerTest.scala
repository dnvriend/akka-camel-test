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

import akka.camel.CamelMessage
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import com.github.dnvriend.TestSpec
import org.apache.camel.Message
import org.apache.camel.rx.ReactiveCamel
import org.reactivestreams.Publisher
import org.scalatest.Ignore
import rx.{ Observable, RxReactiveStreams }

import scala.concurrent.duration._

@Ignore
class DirectReactiveConsumerTest extends TestSpec {
  import CamelOps._
  final val Pub = "direct:observable"

  def send(msg: String): Unit = producerTemplate.sendBody(Pub, msg)

  it should "wait for messages" in {
    val observable: Observable[Message] = new ReactiveCamel(defaultCamelContext).toObservable(Pub)
    val publisher: Publisher[Message] = RxReactiveStreams.toPublisher(observable)
    val tp = Source.fromPublisher(publisher)
      .map(convertToCamel).log("convert")
      .map(_.copy(headers = NoHeaders)).log("copy headers")
      .runWith(TestSink.probe[CamelMessage])
    tp.within(10.seconds) {
      tp.request(1)
      send("hello")
      send("hello")
      send("hello")
      tp.expectNext(CamelMessage("hello", NoHeaders))
      tp.expectNoMsg(500.millis)
      tp.request(1)
      tp.expectNext(CamelMessage("hello", NoHeaders))
      tp.expectNoMsg(500.millis)
      tp.request(1)
      tp.expectNext(CamelMessage("hello", NoHeaders))
      tp.expectNoMsg(500.millis)
      tp.cancel()
    }
  }
}
