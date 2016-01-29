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

package com.github.dnvriend.camel.stream

import akka.actor.{ ActorRef, Props }
import akka.camel.{ Ack, CamelMessage }
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import com.github.dnvriend.camel.FileComponentSpec
import com.github.dnvriend.camel.rx.AkkaCamelPublisher

import scala.concurrent.duration._

class AkkaCamelPublisherTest extends FileComponentSpec {

  def getSender(msg: CamelMessage): Option[ActorRef] =
    msg.headers.get(AkkaCamelPublisher.CamelComponentRef).map(_.asInstanceOf[ActorRef])

  def numFiles(num: Int): Unit = eventually {
    countFiles(FileDir) shouldBe num
  }

  it should "" in {
    createFiles(FileDir, 10)
    eventually {
      countFiles(FileDir) shouldBe 10
    }

    val tp =
      Source.actorPublisher(Props(new AkkaCamelPublisher(config(defaultConfig))))
        .buffer(1, OverflowStrategy.backpressure)
        .runWith(TestSink.probe[CamelMessage])

    tp.within(2.minutes) {
      (0 until 10).foreach { i ⇒
        println("===>" + i)
        // take 1
        tp.request(1)
        val s1 = getSender(tp.expectNext())
        numFiles(10 - i)
        s1.foreach(_ ! Ack)
        numFiles(10 - (i + 1))
        tp.expectNoMsg(2.seconds)
      }
      tp.cancel()
    }
  }
}
