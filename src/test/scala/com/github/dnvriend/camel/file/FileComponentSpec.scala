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

import java.io.File

import akka.actor.{ ActorRef, Props }
import com.github.dnvriend.TestSpec
import com.github.dnvriend.camel.{ FileComponent, QueryStringBuilder }

import scala.concurrent.duration.FiniteDuration

trait FileComponentSpec extends TestSpec {
  final val FileDir = "/tmp/files"

  def config(cfg: Map[String, String]): String = QueryStringBuilder(FileComponent, FileDir, cfg)

  def fileConsumer(cfg: Map[String, String], ackDelay: Option[FiniteDuration] = None, disableDelay: Option[FiniteDuration] = None): ActorRef = {
    val autoAck: Boolean = ackDelay.isEmpty
    system.actorOf(Props(new TestCamelConsumer(config(cfg), autoAck, ackDelay, disableDelay)))
  }

  override protected def beforeEach(): Unit = {
    if (new File(FileDir).exists()) {
      val dir = new File(FileDir)
      dir.listFiles() foreach (_.delete)
      dir.delete()
    }
    new File(FileDir).mkdir()
  }
}
