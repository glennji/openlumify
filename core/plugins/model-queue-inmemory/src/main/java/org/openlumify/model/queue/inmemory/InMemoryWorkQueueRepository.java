package org.openlumify.model.queue.inmemory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.ingest.WorkerSpout;
import org.openlumify.core.ingest.WorkerTuple;
import org.openlumify.core.model.WorkQueueNames;
import org.openlumify.core.model.workQueue.Priority;
import org.openlumify.core.model.workQueue.WorkQueueRepository;

import java.util.*;

@Singleton
public class InMemoryWorkQueueRepository extends WorkQueueRepository {
    private static Map<String, List<byte[]>> queues = new HashMap<>();
    private List<BroadcastConsumer> broadcastConsumers = new ArrayList<>();

    @Inject
    public InMemoryWorkQueueRepository(
            Graph graph,
            WorkQueueNames workQueueNames,
            Configuration configuration
    ) {
        super(graph, workQueueNames, configuration);
    }

    @Override
    protected void broadcastJson(JSONObject json) {
        for (BroadcastConsumer consumer : broadcastConsumers) {
            consumer.broadcastReceived(json);
        }
    }

    @Override
    public void pushOnQueue(String queueName, byte[] data, Priority priority) {
        LOGGER.debug("push on queue: %s: %s", queueName, data);
        addToQueue(queueName, data, priority);
    }

    @Override
    public void pushOnDeadLetterQueue(String queueName, byte[] data) {
        LOGGER.debug("push on dead letter queue: %s: %s", queueName, data);
        addToQueue(String.format("%s.deadletter", queueName), data, Priority.NORMAL);
    }

    public void addToQueue(String queueName, byte[] data, Priority priority) {
        final List<byte[]> queue = getQueue(queueName);
        // getQueue - only returns static variables
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (queue) {
            if (priority == Priority.HIGH) {
                queue.add(0, data);
            } else {
                queue.add(data);
            }
            queue.notifyAll();
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public void format() {
        clearQueue();
    }

    @Override
    public void subscribeToBroadcastMessages(BroadcastConsumer broadcastConsumer) {
        broadcastConsumers.add(broadcastConsumer);
    }

    @Override
    public void unsubscribeFromBroadcastMessages(BroadcastConsumer broadcastConsumer) {
        broadcastConsumers.remove(broadcastConsumer);
    }

    @Override
    public WorkerSpout createWorkerSpout(String queueName) {
        final List<byte[]> queue = getQueue(queueName);
        return new WorkerSpout() {
            @Override
            public WorkerTuple nextTuple() throws Exception {
                synchronized (queue) {
                    if (queue.size() == 0) {
                        Thread.sleep(100);
                        return null;
                    }
                    byte[] entry = queue.remove(0);
                    if (entry == null) {
                        Thread.sleep(100);
                        return null;
                    }
                    return new WorkerTuple("", entry);
                }
            }
        };
    }

    public static void clearQueue() {
        queues.clear();
    }

    @Override
    protected void deleteQueue(String queueName) {
        queues.remove(queueName);
    }

    public static List<byte[]> getQueue(String queueName) {
        return queues.computeIfAbsent(queueName, k -> new LinkedList<>());
    }
}
