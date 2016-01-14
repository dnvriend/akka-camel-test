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

package com.github.dnvriend

import java.io.{ InputStream, File, PrintWriter, Writer }

import akka.actor.Status.Failure
import akka.actor._
import akka.camel.{ CamelExtension, Ack, CamelMessage, Consumer }
import akka.event.{ LoggingReceive, Logging, LoggingAdapter }
import akka.testkit.TestProbe
import org.scalatest._
import org.scalatest.concurrent.{ Eventually, ScalaFutures }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

trait TestSpec extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll with BeforeAndAfterEach with Eventually {
  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val log: LoggingAdapter = Logging(system, this.getClass)
  implicit val pc: PatienceConfig = PatienceConfig(timeout = 50.seconds)

  implicit class FutureToTry[T](f: Future[T]) {
    def toTry: Try[T] = Try(f.futureValue)
  }

  /**
   * A test camel consumer
   */
  class TestCamelConsumer(val endpointUri: String, override val autoAck: Boolean = true, ackDelay: Option[FiniteDuration] = None, disableDelay: Option[FiniteDuration] = None) extends Consumer {

    case object Resume

    override def receive: Receive = LoggingReceive {
      case CamelMessage(is: InputStream, headers) ⇒

        ackDelay.foreach { delay ⇒
          context.system.scheduler.scheduleOnce(delay, sender, Ack)
        }

        disableDelay.foreach { delay ⇒
          // note that this only works if first the message is Ack-ed!
          CamelExtension(context.system).template.sendBody("controlbus:route?routeId=" + self.path.toString + "&action=suspend", null)
          context.system.scheduler.scheduleOnce(delay, self, Resume)
        }

      case Resume ⇒
        // note that this only works if first the message is Ack-ed!
        CamelExtension(context.system).template.sendBody("controlbus:route?routeId=" + self.path.toString + "&action=resume", null)
    }
  }

  class AckCamelConsumer(val endpointUri: String, override val autoAck: Boolean = false, f: Any ⇒ Future[Unit]) extends Consumer {
    case object BecomeReceive

    def replyWithFail: Receive = LoggingReceive {
      case BecomeReceive ⇒
        context.become(receive)
      case _ ⇒
        println("replyWithFail")
        sender() ! Failure
    }

    def action(payload: Any): Future[Unit] =
      f(payload)

    override def receive: Receive = LoggingReceive {
      case payload ⇒
        context.become(replyWithFail)
        val theSender = sender()
        action(payload)
          .map { _ ⇒
            self ! BecomeReceive
            theSender ! Ack
          }.recover {
            case t: Throwable ⇒
              self ! BecomeReceive
              theSender ! Failure
          }
    }
  }

  /**
   * Writes content to a file
   */
  def write(path: String)(f: Writer ⇒ Unit): Unit = {
    val out = new PrintWriter(path)
    try { f(out) }
    finally { Try(out.close()) }
  }

  /**
   * Sends the PoisonPill command to an actor and waits for it to die
   */
  def cleanup(actors: ActorRef*): Unit = {
    val tp = TestProbe()
    actors.foreach { (actor: ActorRef) ⇒
      actor ! PoisonPill
      tp watch actor
      tp.expectTerminated(actor)
    }
  }

  def countFiles(dir: String): Int =
    new File(dir).listFiles().count(_.isFile)

  /**
   * Creates a number of files
   */
  def createFiles(fileDir: String, noFiles: Int = 10): Unit =
    (1 to noFiles).foreach { i ⇒
      write(s"$fileDir/file$i.txt") { writer ⇒
        writer.write("foobar")
      }
    }

  def sleep(duration: FiniteDuration): Unit =
    Thread.sleep(duration.toMillis)

  override protected def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.toTry should be a 'success
  }
}
