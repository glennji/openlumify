package org.openlumify.core.model;

import com.codahale.metrics.Counter;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.ingest.WorkerSpout;
import org.openlumify.core.ingest.WorkerTuple;
import org.openlumify.core.ingest.graphProperty.WorkerItem;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.status.MetricsManager;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

public abstract class WorkerBase<TWorkerItem extends WorkerItem> {
    private final boolean exitOnNextTupleFailure;
    private final Counter queueSizeMetric;
    private final MetricsManager metricsManager;
    private final String queueSizeMetricName;
    private WorkQueueRepository workQueueRepository;
    private volatile boolean shouldRun;
    private final Queue<WorkerItemWrapper> tupleQueue = new LinkedList<>();
    private final int tupleQueueSize;
    private Thread processThread;

    protected WorkerBase(
            WorkQueueRepository workQueueRepository,
            Configuration configuration,
            MetricsManager metricsManager
    ) {
        this.workQueueRepository = workQueueRepository;
        this.metricsManager = metricsManager;
        this.exitOnNextTupleFailure = configuration.getBoolean(getClass().getName() + ".exitOnNextTupleFailure", true);
        this.tupleQueueSize = configuration.getInt(getClass().getName() + ".tupleQueueSize", 10);
        this.queueSizeMetricName = metricsManager.createMetricName(this, "counter", "queue-size-" + Thread.currentThread().getId());
        this.queueSizeMetric = metricsManager.counter(queueSizeMetricName);
    }

    @Override
    protected void finalize() throws Throwable {
        metricsManager.removeMetric(queueSizeMetricName);
        super.finalize();
    }

    public void run() throws Exception {
        OpenLumifyLogger logger = OpenLumifyLoggerFactory.getLogger(this.getClass());

        logger.debug("begin runner");
        WorkerSpout workerSpout = prepareWorkerSpout();
        shouldRun = true;
        startProcessThread(logger, workerSpout);
        pollWorkerSpout(logger, workerSpout);
    }

    private void startProcessThread(OpenLumifyLogger logger, WorkerSpout workerSpout) {
        processThread = new Thread(() -> {
            while (shouldRun) {
                WorkerItemWrapper workerItemWrapper = null;
                try {
                    synchronized (tupleQueue) {
                        do {
                            while (shouldRun && tupleQueue.size() == 0) {
                                tupleQueue.wait();
                            }
                            if (!shouldRun) {
                                return;
                            }
                            if (tupleQueue.size() > 0) {
                                workerItemWrapper = tupleQueue.remove();
                                queueSizeMetric.dec();
                                tupleQueue.notifyAll();
                            }
                        } while (shouldRun && workerItemWrapper == null);
                    }
                } catch (Exception ex) {
                    throw new OpenLumifyException("Could not get next workerItem", ex);
                }
                if (!shouldRun) {
                    return;
                }
                try {
                    logger.debug("start processing");
                    long startTime = System.currentTimeMillis();
                    process(workerItemWrapper.getWorkerItem());
                    long endTime = System.currentTimeMillis();
                    logger.debug("completed processing in (%dms)", endTime - startTime);
                    workerSpout.ack(workerItemWrapper.getWorkerTuple());
                } catch (Throwable ex) {
                    logger.error("Could not process tuple: %s", workerItemWrapper, ex);
                    workerSpout.fail(workerItemWrapper.getWorkerTuple());
                }
            }
        });
        processThread.setName(Thread.currentThread().getName() + "-process");
        processThread.start();
    }

    private void pollWorkerSpout(OpenLumifyLogger logger, WorkerSpout workerSpout) throws InterruptedException {
        while (shouldRun) {
            WorkerItemWrapper workerItemWrapper;
            WorkerTuple tuple = null;
            try {
                tuple = workerSpout.nextTuple();
                if (tuple == null) {
                    workerItemWrapper = null;
                } else {
                    TWorkerItem workerItem = tupleDataToWorkerItem(tuple.getData());
                    workerItemWrapper = new WorkerItemWrapper(workerItem, tuple);
                }
            } catch (InterruptedException ex) {
                if (tuple != null) {
                    workerSpout.fail(tuple);
                }
                throw ex;
            } catch (Exception ex) {
                if (tuple != null) {
                    workerSpout.fail(tuple);
                }
                handleNextTupleException(logger, ex);
                continue;
            }
            if (workerItemWrapper == null) {
                continue;
            }
            synchronized (tupleQueue) {
                tupleQueue.add(workerItemWrapper);
                queueSizeMetric.inc();
                tupleQueue.notifyAll();
                while (shouldRun && tupleQueue.size() >= tupleQueueSize) {
                    tupleQueue.wait();
                }
            }
        }
    }

    protected void handleNextTupleException(OpenLumifyLogger logger, Exception ex) throws InterruptedException {
        if (exitOnNextTupleFailure) {
            throw new OpenLumifyException("Failed to get next tuple", ex);
        } else {
            logger.error("Failed to get next tuple", ex);
            Thread.sleep(10 * 1000);
        }
    }

    protected abstract void process(TWorkerItem workerItem) throws Exception;

    /**
     * This method gets called in a different thread than {@link #process(WorkerItem)} this
     * allows an implementing class to prefetch data needed for processing.
     */
    protected abstract TWorkerItem tupleDataToWorkerItem(byte[] data) throws Exception;

    public void stop() {
        shouldRun = false;
        synchronized (tupleQueue) {
            tupleQueue.notifyAll();
        }
        try {
            if (processThread != null) {
                processThread.join(10000);
            }
        } catch (InterruptedException e) {
            throw new OpenLumifyException("Could not stop process thread: " + processThread.getName());
        }
    }

    protected WorkerSpout prepareWorkerSpout() {
        WorkerSpout spout = workQueueRepository.createWorkerSpout(getQueueName());
        spout.open();
        return spout;
    }

    protected abstract String getQueueName();

    protected WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    public boolean shouldRun() {
        return shouldRun;
    }

    private class WorkerItemWrapper {
        private final TWorkerItem workerItem;
        private final WorkerTuple workerTuple;

        public WorkerItemWrapper(TWorkerItem workerItem, WorkerTuple workerTuple) {
            this.workerItem = workerItem;
            this.workerTuple = workerTuple;
        }

        public WorkerTuple getWorkerTuple() {
            return workerTuple;
        }

        public Object getMessageId() {
            return workerTuple.getMessageId();
        }

        public TWorkerItem getWorkerItem() {
            return workerItem;
        }

        @Override
        public String toString() {
            return "WorkerItemWrapper{" +
                    "messageId=" + getMessageId() +
                    ", workerItem=" + workerItem +
                    '}';
        }
    }
}
