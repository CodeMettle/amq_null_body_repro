package com.codemettle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static com.codemettle.Util.tcpSession;
import static com.codemettle.Util.vmSession;

@SuppressWarnings("unused")
public class TestRepro implements InitializingBean, DisposableBean {
    private Logger m_logger = LoggerFactory.getLogger(getClass());
    private List<Closeable> m_toClose = new LinkedList<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        m_logger.info("Initializing");

        // create listeners on TCP transports to simulate other JVMs consuming
        for (int i = 0; i < 3; ++i)
            m_toClose.add(new TopicListeners(tcpSession(), false));

        // create listeners on VM transports to duplicate listeners on routers in AMQ
        for (int i = 0; i < 3; ++i)
            m_toClose.add(new TopicListeners(vmSession(), true));

        // create splitter on VM transport that takes in messages, sends them to queue, splits the messages, and sends
        // them to various topics to be consumed by TopicListeners
        m_toClose.add(new Splitter(vmSession()));

        // create sender on TCP transport simulate external JVM process sending items to the splitter
        m_toClose.add(new OriginalProducer(tcpSession()));

        m_logger.info("Initialization done");
    }

    private void shutdown(final Closeable aToClose) {
        try {
            aToClose.close();
        } catch (IOException e) {
            // ignore
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void destroy() throws Exception {
        m_logger.info("Shutting down");

        m_toClose.forEach(this::shutdown);

        m_logger.info("Completed shutting down");
    }
}
