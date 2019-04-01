package org.conquernos.shover.android;


import com.google.gson.JsonObject;
import org.conquernos.shover.android.config.ShoverConfig;
import org.conquernos.shover.android.exceptions.ShoverException;
import org.conquernos.shover.android.exceptions.send.ShoverMessageException;
import org.conquernos.shover.android.exceptions.send.ShoverSendException;
import org.conquernos.shover.android.exceptions.topic.ShoverNullTopicException;
import org.conquernos.shover.android.kafka.KafkaRestProducer;
import org.conquernos.shover.android.schema.Record;
import org.conquernos.shover.android.stats.ShoverStats;
import org.conquernos.shover.android.utils.FileUtils;
import org.conquernos.shover.android.utils.Loader;

import java.io.InputStream;
import java.net.URL;


/**
 * A Kafka producer that publishes records to the Kafka cluster.
 * The Shover is thread-safe and the singleton class.
 * An instance need only a config file. So the directory of the file 'shover.conf' is in classpath
 * or set the system properties 'key:shover.config, value:path'. (ex. -Dshover.config=/path/shover.conf)
 * TODO 1. 스키마 레지스트리에 스키마가 변경 되어서 적용하고자 할 때 application 재시작하지 않고 적용될 수 있도록 기능 추가 (dynamic configuration)
 * TODO 2. 정상종료 되지 못 했을 경우를 대비해서 데이터 백업 기능 추가
 */
public class Shover {

    // class for graceful shutdown
    private static class ShutdownShover extends Thread {

        private final Shover shover;

        ShutdownShover(Shover shover) {
            this.shover = shover;
        }

        @Override
        public void run() {
            // close producer
            shover.close();
            System.out.printf("shutdown shover : %s\n", shover.stats);
        }

    }

    private static Shover shover = null;

    private final ShoverConfig config;

    private final KafkaRestProducer producer;

    private final ShoverStats stats = new ShoverStats();


    private Shover(String configFilePath) {
        this(new ShoverConfig(configFilePath));
    }

    private Shover(ShoverConfig config) {
        this.config = config;

        producer = new KafkaRestProducer(config.getKafkaRestUrl()
            , config.getSubjects()
            , stats
            , config.getSenderPoolSize()
            , config.getMessageFlushSize()
            , config.getMessageFlushPeriod()
            , config.getMessageBulkSize()
            , config.getApiKey()
            , config.getSslCertTimeout());

        Runtime.getRuntime().addShutdownHook(new ShutdownShover(this));
    }

    /**
     * Get the shover instance.
     *
     * @return Shover instance
     */
    public static Shover getInstance() {
        if (shover == null) {
            String configFilePath = System.getProperty(ShoverConfig.CONFIG_FILE_NAME_KEY);
            if (configFilePath == null) {
                URL url = Loader.getResourceUrl(ShoverConfig.DEFAULT_CONFIG_FILE_NAME);
                if (url != null) {
                    shover = new Shover(url.getPath());
                } else {
                    InputStream configInputStream = Loader.getResourceInputStream(ShoverConfig.DEFAULT_CONFIG_FILE_NAME);
                    if (configInputStream != null) shover = new Shover(new ShoverConfig(configInputStream));
                }
            } else {
                if (FileUtils.isFile(configFilePath)) shover = new Shover(configFilePath);
            }
        }

        if (shover != null) return shover;

        throw new RuntimeException("Config file is not found");
    }

    /**
     * Asynchronously send a message to the kafka cluster.
     * The topic of this message is the first value of the topics property. (ex. topics=topic1,topic2,... -> topic1)
     * And the schema version is the value of the topics property or 1.
     * So if want to select other topic, use {@link #send(String, ShoverMessage)}.
     *
     * @param message A value object that takes message (the member values of the class have to correspond with the schema)
     * @throws ShoverException If the message was not sent
     */
    public void send(ShoverMessage message) throws ShoverException {
        send(config.getSubjects()[0].getSubject(), message);
    }

    /**
     * Asynchronously send a message to the kafka cluster.
     * The topic of this message is the first value of the topics property. (ex. topics=topic1,topic2,... -> topic1)
     * And the schema version is the value of the topics property or 1.
     * So if want to select other topic, use {@link #send(String, Object[])}.
     *
     * @param values Values that is in the order of their schema positions
     * @throws ShoverException If the message was not sent
     */
    public void send(Object[] values) throws ShoverException {
        send(config.getSubjects()[0].getSubject(), values);
    }

    /**
     * Asynchronously send a message to the kafka cluster.
     * The schema version is the value of the topics property or 1.
     *
     * @param topic   A topic of this message
     * @param message A value object that takes message (the member values of the class has to match the schema)
     * @throws ShoverException
     */
    public void send(String topic, ShoverMessage message) throws ShoverException {
        send(topic, (Object) message);
    }

    /**
     * Asynchronously send a message to the kafka cluster.
     * The schema version is the value of the topics property or 1.
     *
     * @param topic   A topic of this message
     * @param message Values that is in the order of their schema positions
     * @throws ShoverException
     */
    public void send(String topic, Object[] message) throws ShoverException {
        send(topic, (Object) message);
    }

    /**
     * Asynchronously send a message to the kafka cluster.
     * The schema version is the value of the topics property or 1.
     *
     * @param topic   A topic of this message
     * @param message Values that is in the order of their schema positions
     * @throws ShoverException
     */
    private void send(String topic, Object message) throws ShoverException {
        if (topic == null) throw new ShoverNullTopicException();

        JsonObject record;
        try {
            if (message instanceof ShoverMessage) {
                record = Record.makeRecordJsonObject((ShoverMessage) message);
            } else if (message instanceof Object[]) {
                record = Record.makeRecordJsonObject((Object[]) message);
            } else {
                throw new ShoverMessageException(topic, message);
            }
        } catch (Exception e) {
            throw new ShoverMessageException(topic, message, e);
        }

        try {
            producer.send(topic, record);

        } catch (Exception e) {
            throw new ShoverSendException(topic, message, e);
        }
    }

    /**
     * Get the number of total messages that have been delivered to 'send()' method
     *
     * @return the number of total messages
     */
    public long getNumberOfMessages() {
        return stats.getNumberOfMessages();
    }

    /**
     * Get the number of total messages that have been delivered to the Kafka broker (flushed)
     *
     * @return the number of total messages
     */
    public long getNumberOfCompletedMessages() {
        return stats.getNumberOfCompletedMessages();
    }

    private void close() {
        producer.close();
    }

}
