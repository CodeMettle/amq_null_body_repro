AMQ Null Text Bodies
====================

* Get ActiveMQ

```
wget "https://search.maven.org/remotecontent?filepath=org/apache/activemq/apache-activemq/5.15.4/apache-activemq-5.15.4-bin.tar.gz"
```

* Unpack it

```
tar zxvf apache-activemq-5.15.4-bin.tar.gz 
```

* Load test code

```
vim apache-activemq-5.15.4/conf/activemq.xml 
```

```
diff -uw apache-activemq-5.15.4/conf/activemq.xml.orig apache-activemq-5.15.4/conf/activemq.xml
--- apache-activemq-5.15.4/conf/activemq.xml.orig	2018-05-29 20:24:20.337690221 -0400
+++ apache-activemq-5.15.4/conf/activemq.xml	2018-05-29 18:48:16.655662546 -0400
@@ -132,5 +132,7 @@
     -->
     <import resource="jetty.xml"/>
 
+    <bean id="test" class="codemettle.TestRepro"/>
+
 </beans>
 <!-- END SNIPPET: example -->
```

* Compile test code

```
cd amqnullmessages
mvn package
cd ..
```

* Copy output

```
cp amqnullmessages/target/amq-null-messages-1.0-SNAPSHOT.jar apache-activemq-5.15.4/lib/
cp amqnullmessages/target/lib/* apache-activemq-5.15.4/lib/
```

* Run AMQ

```
cd apache-activemq-5.15.4/
./bin/linux-x86-64/activemq console
```

```
...
jvm 1    |  INFO | Sending message #10000
jvm 1    | ERROR | Got NULL message body: AMQMessage(null,JMSMessageProperties(Some(ID:stevencentos7vm-37095-1527640225751-16:1:1:1:3481),1527640346096,None,None,Some(Topic(topic2)),2,false,Some(msgType2),0,4),Map(JMSXGroupID -> 41, JMSXGroupSeq -> 0))
jvm 1    |  INFO | Sending message #11000
jvm 1    |  INFO | Sending message #12000
jvm 1    |  INFO | Sending message #13000
jvm 1    | ERROR | Got NULL message body: AMQMessage(null,JMSMessageProperties(Some(ID:stevencentos7vm-37095-1527640225751-16:1:1:2:4390),1527640376123,None,None,Some(Topic(topic3)),2,false,Some(msgType3),0,4),Map(JMSXGroupID -> 33, JMSXGroupSeq -> 0))
jvm 1    |  INFO | Sending message #14000
...
```

Logging all sent messages
=========================

In `conf/log4j.properties`:

```
log4j.logger.codemettle.Splitter$ListenActor=TRACE
```

What the test is doing
======================

* Starts a connection on the openwire (tcp) transport using an `nio://` address
* Every second, this produces 3 `TextMessage`s to a topic, each made up of 30
 strings between 1k & 2k characters (joined in a JSON format)
* Starts a connection on the VM transport which consumes these messages, splits
 them into the 30 individual messages, and pushes the individual messages to 3
 separate topics (1 topic for each of the original messages)
* Starts 3 connections on NIO transports, each listening to all 3 topics, does 
 nothing with those messages upon receiving them
* Starts 3 connections on VM transports, each listening to all 3 topics. One of
 these connections logs messages that have `null` bodies (which, of course, 
 should be none of the messages)

### Technical note

This test is using an ActiveMQ wrapper library based on Akka message passing, 
using an immutable model class converted to/from `jms.Message`s. It solely deals
with the immutable models except at the boundaries of ActiveMQ code; e.g. it
creates a `MessageListener` whose `onMessage` method simply converts from a
`jms.Message` to an immutable `AMQMessage` (on the calling thread) and then
hands the immutable object off to the Akka library. An analogous operation 
applies for message sending - it only deals with the immutable `AMQMessage`
object until calling `MessageProducer.send` (and does not retain a reference to
the sent message). Thus, I am convinced that the race condition demonstrated by
this test has nothing to do with user code - unless it is invalid to have 
separate `Session`s with separate `MessageListener`s calling `getText` on the
same message on separate threads using the VM transport.