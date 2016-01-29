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
import com.github.dnvriend.camel.FileComponentSpec
import com.github.dnvriend.camel.rx.CamelOps._
import org.apache.camel.Message
import org.apache.camel.rx.ReactiveCamel
import org.reactivestreams.Publisher
import org.scalatest.Ignore
import rx.{ RxReactiveStreams, Observable }

import scala.concurrent.duration._

/**
 * Does not work, because the Camel FileComponent is a 'hot' Producer, it just keeps producing messages
 * whether you're ready or not...
 */
@Ignore
class ReactiveFileConsumerTest extends FileComponentSpec {
  it should "consume files, adhere to backpressure request" in {
    createFiles(FileDir, 10)
    eventually {
      countFiles(FileDir) shouldBe 10
    }

    val observable: Observable[Message] = new ReactiveCamel(defaultCamelContext).toObservable("file:///tmp/files?maxMessagesPerPoll=1&delete=true")
    val publisher: Publisher[Message] = RxReactiveStreams.toPublisher(observable)
    val tp = Source.fromPublisher(publisher)
      .map(convertToCamel).log("convert")
      .runWith(TestSink.probe[CamelMessage])
    tp.within(10.seconds) {
      tp.request(1)
      tp.expectNext() should matchPattern {
        case CamelMessage(_, headers) if headers.exists { case (k, v) ⇒ k == "CamelFileName" && v == "file1.txt" } ⇒
      }
      countFiles(FileDir) shouldBe 9
      tp.expectNoMsg(5.second)
      tp.cancel()
    }
  }

}
