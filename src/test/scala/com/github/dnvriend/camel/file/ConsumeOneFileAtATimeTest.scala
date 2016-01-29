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

import com.github.dnvriend.camel.{ FileComponentSpec, FileComponent }

/**
 * The Camel file component is both a polling and a batch consumer,
 * and because of this, when we don't want to flood the mailbox with
 * messages that are on the filesystem anyway, we can set the
 * `maxMessagesPerPoll` option, so that one file at a time will be read
 * by the consumer.
 */
class ConsumeOneFileAtATimeTest extends FileComponentSpec {

  it should "consume one file at a time because of the maxMessagesPerPoll setting of `1`" in {
    createFiles(FileDir, 10)
    eventually {
      countFiles(FileDir) shouldBe 10
    }

    /**
     * The component will poll the FileDir:
     * - every 0.5 seconds
     * - delete the file after processing successfully
     * - only read one file per poll from the directory and put it in the mailbox
     */
    val consumer = fileConsumer(
      Map(
        FileComponent.Options.Delay -> "500",
        FileComponent.Options.Delete -> "true",
        FileComponent.Options.MaxMessagesPerPoll -> "1"
      )
    )

    eventually {
      countFiles(FileDir) shouldBe 0
    }
    cleanup(consumer)
  }

  /**
   * Note that setting an ack, will not change the file component's behavior,
   * which is to just poll every 1 500 millis, and send (push) the detected new file to
   * the mailbox of the actor. The file component's behavior is independent of the actor.
   *
   * It will however only delete the file, after the Ack has been received.
   */
  it should "ack-ing does not change the behavior that the file-component will read all the files" in {
    createFiles(FileDir, 10)
    eventually {
      countFiles(FileDir) shouldBe 10
    }

    /**
     * The component will poll the FileDir:
     * - delete the file after processing successfully
     */
    import scala.concurrent.duration._
    val consumer = fileConsumer(
      Map(
        FileComponent.Options.Delay -> "500",
        FileComponent.Options.Delete -> "true",
        FileComponent.Options.MaxMessagesPerPoll -> "1"
      ), Option(1.second)
    )

    eventually {
      countFiles(FileDir) shouldBe 0
    }
    cleanup(consumer)
  }
}