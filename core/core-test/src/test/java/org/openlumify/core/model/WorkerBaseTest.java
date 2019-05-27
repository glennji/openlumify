package org.openlumify.core.model;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.ingest.WorkerSpout;
import org.openlumify.core.ingest.graphProperty.WorkerItem;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.status.JmxMetricsManager;
import org.openlumify.core.util.OpenLumifyLogger;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkerBaseTest {
    private boolean stopOnNextTupleException;
    private int nextTupleExceptionCount;

    @Mock
    private WorkQueueRepository workQueueRepository;
    @Mock
    private Configuration configuration;
    @Mock
    private WorkerSpout workerSpout;

    @Before
    public void before() {
        nextTupleExceptionCount = 0;
    }

    @Test
    public void testExitOnNextTupleFailure_exitOnNextTupleFailure_true() throws Exception {
        stopOnNextTupleException = false;
        when(configuration.getBoolean(eq(TestWorker.class.getName() + ".exitOnNextTupleFailure"), anyBoolean())).thenReturn(true);
        when(workQueueRepository.createWorkerSpout(eq("test"))).thenReturn(workerSpout);
        when(workerSpout.nextTuple()).thenThrow(new OpenLumifyException("could not get nextTuple"));

        TestWorker testWorker = new TestWorker(workQueueRepository, configuration);
        try {
            testWorker.run();
            fail("should throw");
        } catch (OpenLumifyException ex) {
            assertEquals(1, nextTupleExceptionCount);
        }
    }

    @Test
    public void testExitOnNextTupleFailure_exitOnNextTupleFailure_false() throws Exception {
        stopOnNextTupleException = true;
        when(configuration.getBoolean(eq(TestWorker.class.getName() + ".exitOnNextTupleFailure"), anyBoolean())).thenReturn(false);
        when(workQueueRepository.createWorkerSpout(eq("test"))).thenReturn(workerSpout);
        when(workerSpout.nextTuple()).thenThrow(new OpenLumifyException("could not get nextTuple"));

        TestWorker testWorker = new TestWorker(workQueueRepository, configuration);
        testWorker.run();
        assertEquals(1, nextTupleExceptionCount);
    }

    private class TestWorker extends WorkerBase<TestWorkerItem> {
        protected TestWorker(WorkQueueRepository workQueueRepository, Configuration configuration) {
            super(workQueueRepository, configuration, new JmxMetricsManager());
        }

        @Override
        public TestWorkerItem tupleDataToWorkerItem(byte[] data) {
            return new TestWorkerItem(data);
        }

        @Override
        protected void process(TestWorkerItem workerItem) throws Exception {
            stop();
        }

        @Override
        protected String getQueueName() {
            return "test";
        }

        @Override
        protected void handleNextTupleException(OpenLumifyLogger logger, Exception ex) throws InterruptedException {
            nextTupleExceptionCount++;
            if (stopOnNextTupleException) {
                stop();
                return;
            }
            super.handleNextTupleException(logger, ex);
        }
    }

    private class TestWorkerItem extends WorkerItem {
        private final byte[] data;

        public TestWorkerItem(byte[] data) {
            this.data = data;
        }
    }
}
