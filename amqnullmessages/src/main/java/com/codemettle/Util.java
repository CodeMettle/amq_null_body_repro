package com.codemettle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class Util {
    private Util() {
        // static
    }

    static final String SPLITTER_TOPIC = "splitter";

    static final String TOPIC_1 = "topic1";
    static final String TOPIC_2 = "topic2";
    static final String TOPIC_3 = "topic3";

    static final String MSG_TYPE_1 = "msgType1";
    static final String MSG_TYPE_2 = "msgType2";
    static final String MSG_TYPE_3 = "msgType3";

    static final List<String> ALL_TYPES =
            Collections.unmodifiableList(Arrays.asList(MSG_TYPE_1, MSG_TYPE_2, MSG_TYPE_3));

    static CloseableSession vmSession() {
        return new CloseableSession("vm://localhost?create=false");
    }

    static CloseableSession tcpSession() {
        return new CloseableSession("nio://localhost:61616");
    }
}
