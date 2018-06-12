package com.codemettle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.TextMessage;
import java.io.Closeable;
import java.io.IOException;

import static com.codemettle.Util.*;

public class TopicListeners implements Closeable {
    public static class TopicListener implements Closeable {
        private final Logger m_logger = LoggerFactory.getLogger(getClass());
        private final MessageConsumer m_cons;

        TopicListener(final boolean aDoLogging, final String aConsumeFrom,
                      final CloseableSession aSession) throws JMSException {
            m_cons = aSession.getSession().createConsumer(aSession.getSession().createTopic(aConsumeFrom));
            m_cons.setMessageListener(message -> {
                if (aDoLogging) {
                    if (message instanceof TextMessage) {
                        try {
                            if (((TextMessage) message).getText() == null) {
                                m_logger.error("Got NULL message body: " + message);
                            } else {
                                m_logger.trace("got message " + message);
                            }
                        } catch (final JMSException e) {
                            m_logger.error("Error", e);
                        }
                    } else {
                        m_logger.warn("Got unexpected message: " + message);
                    }
                }
            });
        }

        @SuppressWarnings("RedundantThrows")
        @Override
        public void close() throws IOException {
            try {
                m_cons.close();
            } catch (JMSException e) {
                // ignore
            }
        }
    }

    private final TopicListener m_top1, m_top2, m_top3;

    TopicListeners(final CloseableSession aSession, final boolean aDoLogging) throws JMSException {
        m_top1 = new TopicListener(aDoLogging, TOPIC_1, aSession);
        m_top2 = new TopicListener(aDoLogging, TOPIC_2, aSession);
        m_top3 = new TopicListener(aDoLogging, TOPIC_3, aSession);
    }

    @Override
    public void close() throws IOException {
        m_top1.close();
        m_top2.close();
        m_top3.close();
    }
}
