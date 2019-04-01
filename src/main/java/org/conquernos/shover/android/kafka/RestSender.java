package org.conquernos.shover.android.kafka;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.conquernos.shover.android.exceptions.send.ShoverSendException;
import org.conquernos.shover.android.kafka.KafkaRestProducer.KafkaRestApi;
import org.conquernos.shover.android.stats.ShoverStats;
import org.conquernos.shover.android.thread.runnable.InterruptibleRunner;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class RestSender extends InterruptibleRunner {

    private static final String VALUE_SUBJECT_FIELD_NAME = "value_subject";
    private static final String VALUE_VERSION_FIELD_NAME = "value_version";
    private static final String RECORDS_FIELD_NAME = "records";

    private final KafkaRestApi restApi;

    private final ConcurrentMap<String, RecordQueue> recordsQueue;
    private final Map<String, Integer> versions;

    private final ShoverStats stats;

    private final int flushSize;
    private final long flushPeriod;
    private final int bulkSize;

    private final boolean useFlushSize;
    private final boolean useFlushPeriod;

    RestSender(KafkaRestApi restApi, ConcurrentMap<String, RecordQueue> recordsQueue, Map<String, Integer> versions, ShoverStats stats, int flushSize, long flushPeriod, int bulkSize) {
        this.restApi = restApi;
        this.recordsQueue = recordsQueue;
        this.versions = versions;
        this.stats = stats;
        this.flushSize = flushSize;
        this.flushPeriod = flushPeriod;
        this.bulkSize = bulkSize;

        useFlushSize = flushSize > 0;
        useFlushPeriod = flushPeriod > 0L;
    }

    @Override
    public void run() {
        List<JsonObject> records = new ArrayList<>(flushSize);
        long begin = System.currentTimeMillis();
        while (!isInterrupted()) {
            long now = System.currentTimeMillis();
            boolean passFlushPeriod = !useFlushPeriod || now - begin >= flushPeriod;

            if (useFlushSize || passFlushPeriod) {
                for (String topic : recordsQueue.keySet()) {
                    RecordQueue recordQueue = recordsQueue.get(topic);

                    if (recordQueue.lock()) {
                        try {
                            int numberOfRecords = recordQueue.size();
                            boolean passFlushSize = !useFlushSize || numberOfRecords >= flushSize;
                            if (passFlushPeriod || passFlushSize) {
                                begin = now;
                                try {
                                    while (numberOfRecords-- > 0) {
                                        JsonObject record = recordQueue.poll(1000, TimeUnit.MILLISECONDS);
                                        if (record == null) break;
                                        records.add(record);
                                    }
                                } catch (InterruptedException e) {
                                    interrupt();
                                    break;
                                }
                            }
                        } finally {
                            recordQueue.unlock();
                        }
                    }

                    if (records.size() > 0) {
                        try {
                            send(topic, records, 0, bulkSize);
                        } catch (ShoverSendException e) {
                            stats.addNumberOfFailedMessages(records.size());
                        } finally {
                            records.clear();
                        }
                    }
                }
            }
        }
    }

    private void send(String topic, List<JsonObject> records, int beginIndex, int bulkSize) throws ShoverSendException {
        try {
            int size = (bulkSize > 0) ? Math.min(records.size() - beginIndex, bulkSize) : records.size() - beginIndex;

            JsonArray recordArray = new JsonArray();
            for (int idx = 0; idx < size; idx++) {
                recordArray.add(records.get(beginIndex++));
            }

            JsonObject message = new JsonObject();
            message.addProperty(VALUE_SUBJECT_FIELD_NAME, topic);
            message.addProperty(VALUE_VERSION_FIELD_NAME, versions.get(topic));
            message.add(RECORDS_FIELD_NAME, recordArray);

            Call<JsonObject> call = restApi.send(topic, message);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) stats.addNumberOfCompletedMessages(size);
                    else stats.addNumberOfFailedMessages(size);
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable throwable) {
                    stats.addNumberOfFailedMessages(size);
                    throwable.printStackTrace();
                }
            });

            System.out.printf("[%s] send - %s\n", new Date(System.currentTimeMillis()), message.toString());

            if (beginIndex < records.size()) send(topic, records, beginIndex, bulkSize);
        } catch (Exception e) {
            throw new ShoverSendException(topic, records, e);
        }
    }

    static void send(String topic, int version, List<JsonObject> records, KafkaRestApi restApi, ShoverStats stats) throws ShoverSendException {
        try {
            int size = records.size();
            JsonArray recordArray = new JsonArray();
            for (JsonObject record : records) recordArray.add(record);

            JsonObject message = new JsonObject();
            message.addProperty(VALUE_SUBJECT_FIELD_NAME, topic);
            message.addProperty(VALUE_VERSION_FIELD_NAME, version);
            message.add(RECORDS_FIELD_NAME, recordArray);

            Call<JsonObject> call = restApi.send(topic, message);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) stats.addNumberOfCompletedMessages(size);
                    else stats.addNumberOfFailedMessages(size);
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable throwable) {
                    stats.addNumberOfFailedMessages(size);
                }
            });
        } catch (Exception e) {
            throw new ShoverSendException(topic, records, e);
        }
    }

}
