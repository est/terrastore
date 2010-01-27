/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package terrastore.communication.seda;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import terrastore.communication.protocol.Command;
import terrastore.store.Store;
import static org.junit.Assert.*;
import static org.easymock.classextension.EasyMock.*;
import terrastore.store.StoreOperationException;

/**
 *
 * @author sergio
 */
public class SEDAThreadPoolExecutorTest {

    @Test
    public void testExecution() throws Exception {
        Store store = createMock(Store.class);

        replay(store);

        int commands = 1000;
        final CountDownLatch waitThreadsExecution = new CountDownLatch(commands);

        SEDAThreadPoolExecutor pool = new SEDAThreadPoolExecutor();

        for (int i = 0; i < commands; i++) {
            pool.execute(new ExecutionHandler<Object>(store, new Command<Object>() {

                @Override
                public Object executeOn(Store store) throws StoreOperationException {
                    waitThreadsExecution.countDown();
                    return null;
                }

                @Override
                public void setId(String id) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public String getId() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            }));
        }

        assertTrue(waitThreadsExecution.await(60, TimeUnit.SECONDS));

        pool.shutdown();

        verify(store);
    }

    @Test
    public void testPauseWaitsActiveThreads() throws Exception {
        Store store = createMock(Store.class);

        replay(store);

        int commands = 1000;
        final CountDownLatch waitThreadsStart = new CountDownLatch(commands);
        final AtomicInteger count = new AtomicInteger(commands);

        SEDAThreadPoolExecutor pool = new SEDAThreadPoolExecutor();

        for (int i = 0; i < commands; i++) {
            pool.execute(new ExecutionHandler<Object>(store, new Command<Object>() {

                @Override
                public Object executeOn(Store store) throws StoreOperationException {
                    try {
                        waitThreadsStart.countDown();
                        Thread.sleep(10);
                        count.decrementAndGet();
                        return null;
                    } catch (InterruptedException ex) {
                        return null;
                    }
                }

                @Override
                public void setId(String id) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public String getId() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            }));
        }

        waitThreadsStart.await();
        pool.pause();

        assertEquals(0, count.get());

        pool.shutdown();

        verify(store);
    }

    @Test
    public void testPauseIgnoresExecutionRequests() throws Exception {
        Store store = createMock(Store.class);

        replay(store);

        int commands = 1000;
        final CountDownLatch waitThreadsStart = new CountDownLatch(commands);
        final AtomicInteger count = new AtomicInteger(commands);

        SEDAThreadPoolExecutor pool = new SEDAThreadPoolExecutor();

        pool.pause();

        for (int i = 0; i < commands; i++) {
            pool.execute(new ExecutionHandler<Object>(store, new Command<Object>() {

                @Override
                public Object executeOn(Store store) throws StoreOperationException {
                    try {
                        waitThreadsStart.countDown();
                        Thread.sleep(10);
                        count.decrementAndGet();
                        return null;
                    } catch (InterruptedException ex) {
                        return null;
                    }
                }

                @Override
                public void setId(String id) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public String getId() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            }));
        }

        assertFalse(waitThreadsStart.await(1, TimeUnit.SECONDS));
        assertEquals(commands, count.get());

        pool.shutdown();

        verify(store);
    }

    @Test
    public void testPauseAndResumeExecutionRequests() throws Exception {
        Store store = createMock(Store.class);

        replay(store);

        int commands = 1000;
        final CountDownLatch waitThreadsStart = new CountDownLatch(commands);
        final AtomicInteger count = new AtomicInteger(commands);

        SEDAThreadPoolExecutor pool = new SEDAThreadPoolExecutor();

        pool.pause();

        for (int i = 0; i < commands; i++) {
            pool.execute(new ExecutionHandler<Object>(store, new Command<Object>() {

                @Override
                public Object executeOn(Store store) throws StoreOperationException {
                    try {
                        waitThreadsStart.countDown();
                        Thread.sleep(10);
                        count.decrementAndGet();
                        return null;
                    } catch (InterruptedException ex) {
                        return null;
                    }
                }

                @Override
                public void setId(String id) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public String getId() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            }));
        }

        assertFalse(waitThreadsStart.await(1, TimeUnit.SECONDS));
        assertEquals(commands, count.get());

        pool.resume();

        assertTrue(waitThreadsStart.await(60, TimeUnit.SECONDS));

        pool.shutdown();

        verify(store);
    }
}