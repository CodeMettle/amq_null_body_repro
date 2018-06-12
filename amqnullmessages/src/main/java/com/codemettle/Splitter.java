package com.codemettle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.Closeable;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

import static com.codemettle.Util.*;

public class Splitter implements Closeable {
    private final Logger m_logger = LoggerFactory.getLogger(getClass());
    private final CloseableSession m_sess;
    private final MessageProducer m_top1prod, m_top2prod, m_top3prod;
    private final MessageConsumer m_cons;

    Splitter(final CloseableSession aSession) throws JMSException {
        m_sess = aSession;
        m_top1prod = aSession.getSession().createProducer(aSession.getSession().createTopic(TOPIC_1));
        m_top2prod = aSession.getSession().createProducer(aSession.getSession().createTopic(TOPIC_2));
        m_top3prod = aSession.getSession().createProducer(aSession.getSession().createTopic(TOPIC_3));
        m_cons = aSession.getSession().createConsumer(aSession.getSession().createTopic(SPLITTER_TOPIC));
        m_cons.setMessageListener(new MessageListener() {
            private final Gson m_gson = new Gson();
            private int sent = 0;

            private void sendTopicMessage(final MessageProducer prod, final Message msg) throws JMSException {
                m_logger.trace("Sending " + msg);

                sent += 1;

                if (sent % 1000 == 0)
                    m_logger.info("Sending message #" + sent);

                prod.send(msg);
            }

            private void sendBulkTopicMessage(final MessageProducer prod, final TextMessage msg) throws JMSException {
                List<String> msgs = m_gson.fromJson(msg.getText(), new TypeToken<List<String>>(){}.getType());
                for (String msgBody : msgs) {
                    TextMessage newMsg = aSession.getSession().createTextMessage(msgBody);
                    newMsg.setJMSType(msg.getJMSType());

                    final Enumeration e = msg.getPropertyNames();
                    while (e.hasMoreElements()) {
                        String key = (String) e.nextElement();
                        newMsg.setObjectProperty(key, msg.getObjectProperty(key));
                    }

                    sendTopicMessage(prod, newMsg);
                }
            }

            @Override
            public void onMessage(Message message) {
                try {
                    String type = message.getJMSType();

                    if (message instanceof TextMessage) {
                        if (MSG_TYPE_1.equals(type))
                            sendBulkTopicMessage(m_top1prod, (TextMessage) message);
                        else if (MSG_TYPE_2.equals(type))
                            sendBulkTopicMessage(m_top2prod, (TextMessage) message);
                        else if (MSG_TYPE_3.equals(type))
                            sendBulkTopicMessage(m_top3prod, (TextMessage) message);
                        else
                            m_logger.warn("Invalid JMSType: " + message);
                    } else {
                        m_logger.warn("Invalid message: " + message);
                    }
                } catch (final JMSException e) {
                    m_logger.error("Error while processing " + message, e);
                }
            }
        });
    }

    @Override
    public void close() throws IOException {
        try {
            m_cons.close();
            m_top1prod.close();
            m_top2prod.close();
            m_top3prod.close();
        } catch (JMSException e) {
            // ignore
        }

        m_sess.close();
    }
}
