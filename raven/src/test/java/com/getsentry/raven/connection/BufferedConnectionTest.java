package com.getsentry.raven.connection;

import com.getsentry.raven.BaseTest;
import com.getsentry.raven.buffer.Buffer;
import com.getsentry.raven.event.Event;
import com.getsentry.raven.event.EventBuilder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class BufferedConnectionTest extends BaseTest {
    private List<Event> bufferedEvents;
    private List<Event> sentEvents;
    private Buffer mockBuffer;
    private Connection mockConnection;
    private BufferedConnection bufferedConnection;
    private volatile boolean connectionUp;

    @BeforeMethod
    public void setup() {
        bufferedEvents = Lists.newArrayList();
        sentEvents = Lists.newArrayList();
        connectionUp = true;

        mockConnection = new Connection() {
            @Override
            public void send(Event event) throws ConnectionException {
                if (connectionUp) {
                    sentEvents.add(event);
                } else {
                    throw new ConnectionException("Connection is down.");
                }
            }

            @Override
            public void addEventSendFailureCallback(EventSendFailureCallback eventSendFailureCallback) {

            }

            @Override
            public void close() throws IOException {

            }
        };

        mockBuffer = new Buffer() {
            @Override
            public void add(Event event) {
                bufferedEvents.add(event);
            }

            @Override
            public void discard(Event event) {
                bufferedEvents.remove(event);
            }

            @Override
            public Iterator<Event> getEvents() {
                return Lists.newArrayList(bufferedEvents).iterator();
            }
        };

        int flushtime = 10;
        int shutdownTimeout = 0;
        bufferedConnection = new BufferedConnection(mockConnection, mockBuffer, flushtime, false, shutdownTimeout);
    }

    @AfterMethod
    public void teardown() throws IOException {
        bufferedConnection.close();
    }

    @Test
    public void test() throws Exception {
        Event event = new EventBuilder().build();
        connectionUp = false;
        try {
            bufferedConnection.send(event);
        } catch (Exception e) {

        }
        assertThat(bufferedEvents.size(), equalTo(1));
        assertThat(bufferedEvents.get(0), equalTo(event));

        connectionUp = true;
        waitUntilTrue(1000, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return bufferedEvents.size() == 0;
            }
        });
        assertThat(bufferedEvents.size(), equalTo(0));
        assertThat(sentEvents.get(0), equalTo(event));
    }
}
