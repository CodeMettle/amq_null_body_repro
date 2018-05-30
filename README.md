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
