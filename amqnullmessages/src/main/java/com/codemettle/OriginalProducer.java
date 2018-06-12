package com.codemettle;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.codemettle.Util.ALL_TYPES;
import static com.codemettle.Util.SPLITTER_TOPIC;

public class OriginalProducer implements Closeable {
    private final CloseableSession m_sess;
    private final MessageProducer m_prod;
    private final AtomicBoolean m_running = new AtomicBoolean(true);

    OriginalProducer(final CloseableSession aSession) throws JMSException {
        m_sess = aSession;
        final MessageProducer prod = aSession.getSession().createProducer(aSession.getSession().createTopic(SPLITTER_TOPIC));
        m_prod = prod;

        final Logger logger = LoggerFactory.getLogger(getClass());

        Thread thread = new Thread() {
            private final Random m_rand = new Random();
            private final String m_chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            private final Gson m_gson = new Gson();

            private char nextAlphaNum() {
                return m_chars.charAt(m_rand.nextInt(m_chars.length()));
            }

            private String alphanum(final int length) {
                final StringBuilder sb = new StringBuilder(length);
                for (int i = 0; i < length; ++i) sb.append(nextAlphaNum());
                return sb.toString();
            }

            private String createMessage() {
                final int msgLen = 1000 + m_rand.nextInt(1000);
                return alphanum(msgLen);
            }

            private List<String> thirtyMsgs() {
                final ArrayList<String> msgs = new ArrayList<>();
                for (int i = 0; i < 30; ++i)
                    msgs.add(createMessage());
                return Collections.unmodifiableList(msgs);
            }

            private void sendChunkOfType(final String msgType){
                final List<String> msgs = thirtyMsgs();
                final String json = m_gson.toJson(msgs);

                try {
                    final TextMessage msg = aSession.getSession().createTextMessage(json);
                    msg.setIntProperty("JMSXGroupID", m_rand.nextInt(256));
                    msg.setJMSType(msgType);

                    prod.send(msg);
                } catch (final JMSException e) {
                    logger.error("Error", e);
                }
            }

            @Override
            public void run() {
                while(m_running.get()) {
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                        // ignore
                    }

                    if (m_running.get())
                        ALL_TYPES.forEach(this::sendChunkOfType);
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void close() throws IOException {
        m_running.set(false);

        try {
            m_prod.close();
        } catch (JMSException e) {
            // ignore
        }

        m_sess.close();
    }
}
