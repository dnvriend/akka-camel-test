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

package com.github.dnvriend.camel

import java.io.File

import akka.actor.{ ActorRef, Props }
import com.github.dnvriend.TestSpec

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait FileComponentSpec extends TestSpec {
  final val FileDir = "/tmp/files"

  type Config = Map[String, String]
  type Delay = Option[FiniteDuration]

  val defaultConfig = Map(
    FileComponent.Options.Delay -> "1000",
    FileComponent.Options.Delete -> "true",
    FileComponent.Options.MaxMessagesPerPoll -> "1"
  )

  def config(cfg: Config): String = QueryStringBuilder(FileComponent, FileDir, cfg)

  def fileConsumer(cfg: Config, ackDelay: Delay = None, disableDelay: Delay = None): ActorRef = {
    val autoAck: Boolean = ackDelay.isEmpty
    system.actorOf(Props(new TestCamelConsumer(config(cfg), autoAck, ackDelay, disableDelay)))
  }

  def ackFileConsumer(cfg: Config)(f: Any ⇒ Future[Unit]): ActorRef = {
    system.actorOf(Props(new AckCamelConsumer(config(cfg), autoAck = false, f)))
  }

  def withAckConsumer(noFiles: Int = 3)(f: Any ⇒ Future[Unit]): ActorRef = {
    createFiles(FileDir, noFiles)
    eventually {
      countFiles(FileDir) shouldBe noFiles
    }
    ackFileConsumer(defaultConfig)(f)
  }

  def clearFiles(): Unit = {
    if (new File(FileDir).exists()) {
      val dir = new File(FileDir)
      dir.listFiles() foreach (_.delete)
      println("--> Deleting: " + dir)
      dir.delete()
    }
  }

  override protected def beforeEach(): Unit = {
    clearFiles()
    new File(FileDir).mkdir()
  }
}
