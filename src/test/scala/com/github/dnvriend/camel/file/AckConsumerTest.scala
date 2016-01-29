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

package com.github.dnvriend.camel.file

import com.github.dnvriend.camel.FileComponentSpec

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Because the file component is an independent unit in the camel route,
 * when manually `akka.camel.Ack`-ing or `akka.actor.Status.Failure`-ing
 * the message, the Actor can manage when to delete the file (Ack), or
 * when to hold the file (Failure), encoded in the reply of the Actor.
 */
class AckConsumerTest extends FileComponentSpec {

  it should "process the files one by one" in {
    val consumer = withAckConsumer() { _ ⇒
      Future(Thread.sleep(3000))
    }
    eventually {
      countFiles(FileDir) shouldBe 0
    }
    cleanup(consumer)
  }

  it should "hold all the files" in {
    val consumer = withAckConsumer() { _ ⇒
      Future.failed(new RuntimeException("Mock fail"))
    }

    sleep(7.seconds)

    eventually {
      countFiles(FileDir) shouldBe 3
    }
    cleanup(consumer)
  }
}
