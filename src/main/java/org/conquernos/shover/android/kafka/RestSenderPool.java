package org.conquernos.shover.android.kafka;

import com.google.gson.JsonObject;
import org.conquernos.shover.android.exceptions.send.ShoverSendException;
import org.conquernos.shover.android.kafka.KafkaRestProducer.KafkaRestApi;
import org.conquernos.shover.android.stats.ShoverStats;
import org.conquernos.shover.android.thread.pool.InterruptibleThreadPool;

import java.util.*;
import java.util.concurrent.*;

public class RestSenderPool {

    private final String REST_SENDER_POOL_NAME = "RestSenderPoolName" + UUID.randomUUID();

    private final ConcurrentMap<String, RecordQueue> recordsMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> versions;

    private final InterruptibleThreadPool senderPool;
    private final KafkaRestApi restApi;
    private final ShoverStats stats;


    RestSenderPool(KafkaRestApi restApi, Map<String, Integer> versions, ShoverStats stats, int poolSize, int flushSize, long flushPeriod, int bulkSize) {
        this.restApi = restApi;
        this.versions = versions;
        this.stats = stats;

        senderPool = new InterruptibleThreadPool(REST_SENDER_POOL_NAME, poolSize);
        for (int senderNum = 0; senderNum < poolSize; senderNum++) {
            senderPool.execute(newRestSender(restApi, stats, flushSize, flushPeriod, bulkSize));
        }
    }

    void send(String topic, JsonObject record) throws InterruptedException {
        RecordQueue records = recordsMap.get(topic);
        if (records == null) {
            recordsMap.putIfAbsent(topic, new RecordQueue());
            records = recordsMap.get(topic);
        }

        records.put(record);
    }

    void close() {
        try {
            senderPool.interruptAll();
            senderPool.joinForRunners();
        } finally {
            senderPool.shutdown();
            sendRemainRecords();
        }
    }

    private RestSender newRestSender(KafkaRestApi restApi, ShoverStats stats, int flushSize, long flushPeriod, int bulkSize) {
        return new RestSender(restApi, recordsMap, versions, stats, flushSize, flushPeriod, bulkSize);
    }

    private void sendRemainRecords() {
        List<JsonObject> records = new ArrayList<>();
        for (String topic : recordsMap.keySet()) {
            RecordQueue recordQueue = recordsMap.get(topic);
            while (recordQueue.size() > 0) {
                JsonObject record = recordQueue.poll();
                if (record == null) break;
                records.add(record);
            }

            if (records.size() > 0) {
                try {
                    RestSender.send(topic, versions.get(topic), records, restApi, stats);
                } catch (ShoverSendException e) {
                    stats.addNumberOfFailedMessages(records.size());
                }
            }

            records.clear();
        }
    }

}
