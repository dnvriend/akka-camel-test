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

import java.io.{ File, InputStream, PrintWriter, Writer }

import akka.actor.Status.Failure
import akka.actor._
import akka.camel.{ Ack, CamelExtension, CamelMessage, Consumer }
import akka.event.{ Logging, LoggingAdapter, LoggingReceive }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.testkit.TestProbe
import org.apache.camel.ProducerTemplate
import org.apache.camel.impl.DefaultCamelContext
import org.scalatest._
import org.scalatest.concurrent.{ Eventually, ScalaFutures }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

trait TestSpec extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll with BeforeAndAfterEach with Eventually {
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val log: LoggingAdapter = Logging(system, this.getClass)
  implicit val pc: PatienceConfig = PatienceConfig(timeout = 50.seconds)
  val defaultCamelContext: DefaultCamelContext = CamelExtension(system).context
  val producerTemplate: ProducerTemplate = CamelExtension(system).template

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
          producerTemplate.sendBody("controlbus:route?routeId=" + self.path.toString + "&action=suspend", null)
          context.system.scheduler.scheduleOnce(delay, self, Resume)
        }

      case Resume ⇒
        // note that this only works if first the message is Ack-ed!
        producerTemplate.sendBody("controlbus:route?routeId=" + self.path.toString + "&action=resume", null)
    }
  }

  /**
   * ==Overview==
   * The AckCamelConsumer will reply with a Failure for any new message that the camel component will produce.
   * The component's implementation determines what will happen when such a message will be received, eg.
   * leave the file on the filesystem, leave a message on the queue etc.
   *
   * The AckCamelConsumer has two states:
   * <ul>
   *   <li>The default 'receive' state</li>
   *   <li>The replyWithFail state, in which the actor will reply with an [[akka.actor.Status.Failure]]</li>
   * </ul>
   */
  class AckCamelConsumer(val endpointUri: String, override val autoAck: Boolean = false, f: Any ⇒ Future[Unit]) extends Consumer {
    case object BecomeReceive

    def replyWithFail: Receive = LoggingReceive {
      case BecomeReceive ⇒
        context.become(receive)
        println("--> [REPLY_WITH_FAIL]: Becoming [RECEIVE], effectively accepting new messages")
      case _ ⇒
        println("--> [REPLY_WITH_FAIL]: The camel component sends a new message; Replying failure to the Camel component")
        sender() ! Failure(new RuntimeException("Not accepting new files"))
    }

    def action(theSender: ActorRef, payload: Any): Future[Unit] = (for {
      _ ← f(payload)
    } yield {
      self ! BecomeReceive
      theSender ! Ack
    }).recover {
      case t: Throwable ⇒
        self ! BecomeReceive
        theSender ! Failure(t)
    }

    /**
     * When a message has been received, the actor will become the state [REPLY_WITH_FAIL]
     */
    override def receive: Receive = LoggingReceive {
      case payload ⇒
        context.become(replyWithFail)
        action(sender(), payload)
        println("--> [RECEIVE]: Becoming [REPLY_WITH_FAIL], effectively accepting no more new messages")
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
    new File(dir).listFiles().toList.count(f ⇒ f.isFile && !f.getName.toLowerCase.endsWith(".camellock"))

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
