# akka-camel-test
A study project on akka-camel

# TL;DR
- Camel components, when configured as the Producer of messages, are ['hot'](https://speakerdeck.com/benjchristensen/reactive-programming-with-rx-at-qconsf-2014?slide=21) component; they emit CamelMessages whether you're ready or not. 
- Each camel consumer has a camel route with as `routeId` the `ActorPath's String` representation  
- Using the [Camel Control Bus](http://camel.apache.org/controlbus.html), a camel route can be suspended/resumed thus the component can be suspended/resumed

# Resources
- [Camel File Component](http://camel.apache.org/file2.html)
- [Camel Batch Consumers](http://camel.apache.org/batch-consumer.html)
- [Camel Polling Consumer](http://camel.apache.org/polling-consumer.html)
- [Camel Control Bus](http://camel.apache.org/controlbus.html)
- [Martin Krasser - Akka Consumer Actors: New Features and Best Practices](http://krasserm.blogspot.nl/2011/02/akka-consumer-actors-new-features-and.html)
- [Ben Christensen - Reactive Programming with Rx](http://www.infoq.com/presentations/rx-service-architecture)
- [Ben Christensen - Applying Reactive Programming with Rx](https://www.youtube.com/watch?v=8OcCSQS0tug)
- [Ben Christensen - Functional Reactive Programming with RxJava](https://www.youtube.com/watch?v=_t06LRX0DV0)
- [RxJavaReactiveStreams - Ben Christensen Issue 12 - Hot Observable Handling](https://github.com/ReactiveX/RxJavaReactiveStreams/issues/12)

## Camel producers are hot
Camel components, when configured as the Producer of messages, are ['hot'](https://speakerdeck.com/benjchristensen/reactive-programming-with-rx-at-qconsf-2014?slide=21) 
components; they emit CamelMessages whether you're ready or not. In Akka Camel, this can become problematic as such a producer can easily fill a mailbox. It's a non-blocking environment
and so, [Reactive Streams](http://www.reactive-streams.org/) that the publisher can never emit more than has been requested 
by the consumer. The 'amount-  

## Default Camel Route
The camel route that is used by Akka Camel for the Actor is defined in `akka.camel.internal.ConsumerActorRouteBuilder`
and is defined as

```scala
val endpointUri: String
val consumer: ActorRef
val targetActorUri: String

from(endpointUri).routeId(consumer.path.toString))).to(targetActorUri)
```

This means each camel consumer has a camel route with as routeId the ActorPath's String representation. Using 
the [Camel Control Bus](http://camel.apache.org/controlbus.html), the route can be suspended and/or resumed:

```scala
// suspend a route
CamelExtension(context.system).template.sendBody("controlbus:route?routeId=" + self.path.toString + "&action=suspend", null)

// resume a route
CamelExtension(context.system).template.sendBody("controlbus:route?routeId=" + self.path.toString + "&action=resume", null)
```
