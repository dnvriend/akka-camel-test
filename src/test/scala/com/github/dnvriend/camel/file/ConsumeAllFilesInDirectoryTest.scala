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
 * Because no options have been set other than:
 * - the poll directory
 * - delete the file after processing
 *
 * All files will be read by the file consumer, because it is
 * both a batch consumer and a polling consumer. Because of this,
 * all files have been read and put in the mailbox of the actor.
 */
class ConsumeAllFilesInDirectoryTest extends FileComponentSpec {
  it should "consume all files in directory" in {
    createFiles(FileDir, 10)
    eventually {
      countFiles(FileDir) shouldBe 10
    }

    /**
     * The component will poll the FileDir:
     * - delete the file after processing successfully
     */
    val consumer = fileConsumer(Map(FileComponent.Options.Delete -> "true"))

    eventually {
      countFiles(FileDir) shouldBe 0
    }
    cleanup(consumer)
  }

  /**
   * Note that setting an ack, will not change the file component's behavior, which is to just read all
   * the files when detected. It will however only delete the file, after the Ack has been received, which is
   * after 1 second.
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
    val consumer = fileConsumer(Map(FileComponent.Options.Delete -> "true"), Option(1.second))

    eventually {
      countFiles(FileDir) shouldBe 0
    }
    cleanup(consumer)
  }
}
