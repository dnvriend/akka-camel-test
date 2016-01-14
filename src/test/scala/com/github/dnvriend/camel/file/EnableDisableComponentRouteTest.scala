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

import com.github.dnvriend.camel.FileComponent

/**
 * The file component's behavior is independent of the Actor. This is
 * because it is doing its thing in the context of Camel, as an asynchronous
 * process. But, we can use the Camel control bus interface to send `suspend`
 * or `resume` the route the component is part of. This is because the Actor
 * is also independent of Camel.
 *
 * Because a new route is registered in Camel that uses the `routeId` which is
 * the same as the string representation of the ActorPath, we can send a message
 * using the control bus protocol to the route, to suspend and/or resume the route.
 *
 * Note: the camel component __must__ first Ack the message, then the route can
 * be disabled
 */
class EnableDisableComponentRouteTest extends FileComponentSpec {

  it should "disable the route and enable it after 5 seconds" in {
    createFiles(FileDir, 3)
    eventually {
      countFiles(FileDir) shouldBe 3
    }

    import scala.concurrent.duration._
    val consumer = fileConsumer(
      Map(
        FileComponent.Options.Delay -> "1000",
        FileComponent.Options.Delete -> "true",
        FileComponent.Options.MaxMessagesPerPoll -> "1"
      ), disableDelay = Option(7.seconds)
    )

    eventually {
      countFiles(FileDir) shouldBe 0
    }
    cleanup(consumer)
  }
}
