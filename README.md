# rest-gateway

Reference- https://stackoverflow.com/questions/47800497/how-can-messaginggateway-be-configured-with-spring-cloud-stream-messagechannels

First of all we have to determine for ourselves that gateway means request/reply, therefore correlation. 
And this available in @MessagingGateway via replyChannel header in face of TemporaryReplyChannel instance. 
Even if you have an explicit replyChannel = AccountChannels.ACCOUNT_CREATED, the correlation is done only via the mentioned header 
and its value. The fact that this TemporaryReplyChannel is not serializable and can't be transferred over the network to the 
consumer on another side.

Luckily Spring Integration provide some solution for us. 
It is a part of the HeaderEnricher and its headerChannelsToString option behind HeaderChannelRegistry:

Starting with Spring Integration 3.0, a new sub-element is available; it has no attributes. 
This converts existing replyChannel and errorChannel headers (when they are a MessageChannel) to a String and stores the channel(s) 
in a registry for later resolution when it is time to send a reply, or handle an error. 
This is useful for cases where the headers might be lost; for example when serializing a message into a message store or 
when transporting the message over JMS. If the header does not already exist, or it is not a MessageChannel, no changes are made.

https://docs.spring.io/spring-integration/docs/5.0.0.RELEASE/reference/html/messaging-transformation-chapter.html#header-enricher

But in this case you have to introduce an internal channel from the gateway to the HeaderEnricher and only 
the last one will send the message to the AccountChannels.CREATE_ACCOUNT_REQUEST. 
So, the replyChannel header will be converted to a string representation and be able to travel over the network. 
On the consumer side when you send a reply you should ensure that you transfer that replyChannel header as well, as it is. 
So, when the message will arrive to the AccountChannels.ACCOUNT_CREATED on the producer side, where we have that @MessagingGateway, 
the correlation mechanism is able to convert a channel identificator to the proper TemporaryReplyChannel and correlate the reply 
to the waiting gateway call.

Only the problem here that your producer application must be as single consumer in the group for 
the AccountChannels.ACCOUNT_CREATED - we have to ensure that only one instance in the cloud is operating at a time.
Just because only one instance has that TemporaryReplyChannel in its memory.

More info about gateway: https://docs.spring.io/spring-integration/docs/5.0.0.RELEASE/reference/html/messaging-endpoints-chapter.html#gateway

* Add `Processor` binding for independent `input` and `output` channels.
This way the `TO_UPPERCASE_REPLY` channel doesn't clash with one more
subscriber for round-robin distribution
* The same is applied for the `TO_UPPERCASE_REQUEST`
* Add bindings for `input` and `output` for respective destinations
* Remove `spring-cloud-stream-test-support` to let to test really against
Binder for Apache Kafka
* Add `spring-kafka-test` for testing against Embedded Kafka
* Remove redundant dependencies which are polled transitively
* Add full integration test to demonstrate how things work
* Add a `Converter<byte[], String>` bean to let to convert reply `byte[]`
to the expected return `String`.
Starting with version `2.0`, Spring Cloud String doesn't convert `byte[]`
payload unconditionally if we don't have an explicit POJO method signature
