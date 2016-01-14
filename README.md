# akka-camel-test
A study project on akka-camel

# TL;DR
- Each camel consumer has a camel route with as `routeId` the `ActorPath's String` representation  
- Using the [Camel Control Bus](http://camel.apache.org/controlbus.html), a camel route can be suspended/resumed thus the component can be suspended/resumed

# Resources
- [Camel File Component](http://camel.apache.org/file2.html)
- [Camel Batch Consumers](http://camel.apache.org/batch-consumer.html)
- [Camel Polling Consumer](http://camel.apache.org/polling-consumer.html)
- [Camel Control Bus](http://camel.apache.org/controlbus.html)
- [Martin Krasser - Akka Consumer Actors: New Features and Best Practices](http://krasserm.blogspot.nl/2011/02/akka-consumer-actors-new-features-and.html)

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
