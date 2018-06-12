AMQ Null Text Bodies
====================

I believe an issue similar to
[AMQ-6218](https://issues.apache.org/jira/browse/AMQ-6218)
or
[AMQ-6256](https://issues.apache.org/jira/browse/AMQ-6256)
still exists in ActiveMQ 5.15.4, the latest release as of today.
[AMQ-6221](https://issues.apache.org/jira/browse/AMQ-6221)
also seems related, but was closed as `Won't Fix`.

# What the test is doing

* Starts a connection on the openwire (tcp) transport using an `nio://` address
* Every second, this produces 3 `TextMessage`s to a topic, each made up of 30
 strings between 1k & 2k characters (joined in a JSON format)
* Starts a connection on the VM transport which consumes these messages, splits
 them into the 30 individual messages, and pushes the individual messages to 3
 separate topics (1 topic for each of the original messages)
* Starts 3 connections on NIO transports, each listening to all 3 topics, does 
 nothing with those messages upon receiving them
* Starts 3 connections on VM transports, each listening to all 3 topics. 
 These connections log messages that have `null` bodies (which, of course, 
 should be none of the messages)

# Replication

* Get ActiveMQ

```bash
wget -O apache-activemq-5.15.4-bin.tar.gz "https://search.maven.org/remotecontent?filepath=org/apache/activemq/apache-activemq/5.15.4/apache-activemq-5.15.4-bin.tar.gz"
```

* Unpack it

```bash
tar zxvf apache-activemq-5.15.4-bin.tar.gz 
```

* Load test code

```bash
vim apache-activemq-5.15.4/conf/activemq.xml 
```

```patch
diff -uw apache-activemq-5.15.4/conf/activemq.xml.orig apache-activemq-5.15.4/conf/activemq.xml
--- apache-activemq-5.15.4/conf/activemq.xml.orig	2018-05-29 20:24:20.337690221 -0400
+++ apache-activemq-5.15.4/conf/activemq.xml	2018-05-29 18:48:16.655662546 -0400
@@ -132,5 +132,7 @@
     -->
     <import resource="jetty.xml"/>
 
+    <bean id="test" class="com.codemettle.TestRepro"/>
+
 </beans>
 <!-- END SNIPPET: example -->
```

* Compile test code

```bash
cd amqnullmessages
mvn package
cd ..
```

* Copy output

```bash
cp amqnullmessages/target/amq-null-messages-1.0-SNAPSHOT.jar apache-activemq-5.15.4/lib/
cp amqnullmessages/target/lib/* apache-activemq-5.15.4/lib/
```

* Run AMQ

```bash
cd apache-activemq-5.15.4/
./bin/linux-x86-64/activemq console
```

* Observe error logs when a message has a `null` body

```
...
jvm 1    |  INFO | Connector vm://localhost started
jvm 1    |  INFO | Initialization done
jvm 1    |  INFO | Sending message #1000
jvm 1    | ERROR | Got NULL message body: AMQMessage(null,JMSMessageProperties(Some(ID:stevencentos7vm-37982-1527643119840-16:1:1:1:455),1527643139187,None,None,Some(Topic(topic1)),2,false,Some(msgType1),0,4),Map(JMSXGroupID -> 82, JMSXGroupSeq -> 0))
jvm 1    |  INFO | Sending message #2000
jvm 1    |  INFO | Sending message #3000
jvm 1    |  INFO | Sending message #4000
jvm 1    |  INFO | Sending message #5000
jvm 1    | ERROR | Got NULL message body: AMQMessage(null,JMSMessageProperties(Some(ID:stevencentos7vm-37982-1527643119840-16:1:1:1:1753),1527643182186,None,None,Some(Topic(topic1)),2,false,Some(msgType1),0,4),Map(JMSXGroupID -> 54, JMSXGroupSeq -> 0))
jvm 1    |  INFO | Sending message #6000
jvm 1    |  INFO | Sending message #7000
jvm 1    |  INFO | Sending message #8000
jvm 1    |  INFO | Sending message #9000
jvm 1    |  INFO | Sending message #10000
jvm 1    | ERROR | Got NULL message body: AMQMessage(null,JMSMessageProperties(Some(ID:stevencentos7vm-37982-1527643119840-16:1:1:3:3552),1527643242188,None,None,Some(Topic(topic3)),2,false,Some(msgType3),0,4),Map(JMSXGroupID -> 155, JMSXGroupSeq -> 0))
jvm 1    | ERROR | Got NULL message body: AMQMessage(null,JMSMessageProperties(Some(ID:stevencentos7vm-37982-1527643119840-16:1:1:2:3603),1527643244171,None,None,Some(Topic(topic2)),2,false,Some(msgType2),0,4),Map(JMSXGroupID -> 95, JMSXGroupSeq -> 0))
jvm 1    | ERROR | Got NULL message body: AMQMessage(null,JMSMessageProperties(Some(ID:stevencentos7vm-37982-1527643119840-16:1:1:3:3652),1527643245180,None,None,Some(Topic(topic3)),2,false,Some(msgType3),0,4),Map(JMSXGroupID -> 59, JMSXGroupSeq -> 0))
jvm 1    |  INFO | Sending message #11000
...
```

