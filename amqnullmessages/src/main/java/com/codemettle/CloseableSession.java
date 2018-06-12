package com.codemettle;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import java.io.Closeable;
import java.io.IOException;

public class CloseableSession implements Closeable {
    private final Connection m_conn;

    private final Session m_sess;

    CloseableSession(final String address) {
        ActiveMQConnectionFactory m_connFact = new ActiveMQConnectionFactory(address);
        try {
            m_conn = m_connFact.createConnection();
            m_conn.start();
            m_sess = m_conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    Session getSession() {
        return m_sess;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void close() throws IOException {
        try {
            m_sess.close();
        } catch (final JMSException e) {
            // ignore
        }

        try {
            m_conn.close();
        } catch (JMSException e) {
            // ignore
        }
    }
}
