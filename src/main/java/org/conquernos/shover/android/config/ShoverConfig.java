package org.conquernos.shover.android.config;


import org.conquernos.shover.android.exceptions.config.ShoverConfigException;
import org.conquernos.shover.android.schema.Subject;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.conquernos.shover.android.utils.StringUtils.trim;

public class ShoverConfig {

    public static final String CONFIG_FILE_NAME_KEY = "shover.config";
    public static final String DEFAULT_CONFIG_FILE_NAME = "src/main/resources/shover.conf";

    private static final String PROP_TOPICS = "subjects";
    private static final String PROP_KAFKA_REST_URL = "kafka.rest.url";
    private static final String PROP_MESSAGE_FLUSH_SIZE = "message.flush.size";
    private static final String PROP_SENDER_POOL_SIZE = "sender.pool.size";
    private static final String PROP_MESSAGE_FLUSH_PERIOD = "message.flush.period";
    private static final String PROP_MESSAGE_BULK_SIZE = "message.bulk.size";
    private static final String PROP_SSL_CERT_TIMEOUT = "ssl.cert.timeout";
    private static final String PROP_API_KEY = "api.key";
    private static final String PROP_SHUTDOWN_TIMEOUT = "shutdown.timeout";

    private Subject[] subjects;
    private String kafkaRestUrl;
    private int messageFlushSize;
    private int senderPoolSize;
    private long messageFlushPeriod;
    private int messageBulkSize;
    private int sslCertTimeout;
    private String apiKey;
    private int shutdownTimeout;


    public ShoverConfig(String configFilePath) {
        try {
            InputStream propStream = new FileInputStream(configFilePath);
            loadConfig(propStream);
        } catch (Exception e) {
            throw new ShoverConfigException(e);
        }
    }

    public ShoverConfig(InputStream propStream) {
        loadConfig(propStream);
    }

    private void loadConfig(InputStream propStream) {
        try {
            final Properties properties = new Properties();
            properties.load(propStream);

            subjects = toTopics(getStringListFromConfig(properties, PROP_TOPICS, true));
            kafkaRestUrl = getStringFromConfig(properties, PROP_KAFKA_REST_URL, true);

            senderPoolSize = getIntegerFromConfig(properties, PROP_SENDER_POOL_SIZE, 1);
            messageFlushSize = getIntegerFromConfig(properties, PROP_MESSAGE_FLUSH_SIZE, 100);
            messageFlushPeriod = getLongFromConfig(properties, PROP_MESSAGE_FLUSH_PERIOD, 0L);
            if (messageFlushSize == 0 && messageFlushPeriod == 0L) {
                messageFlushPeriod = 60000L;
            }
            messageBulkSize = getIntegerFromConfig(properties, PROP_MESSAGE_BULK_SIZE, 0);

            sslCertTimeout = getIntegerFromConfig(properties, PROP_SSL_CERT_TIMEOUT, 10000);
            apiKey = getStringFromConfig(properties, PROP_API_KEY, true);

            shutdownTimeout = getIntegerFromConfig(properties, PROP_SHUTDOWN_TIMEOUT, 3);
        } catch (Exception e) {
            throw new ShoverConfigException("failed to load config", e);
        }
    }

    public Subject[] getSubjects() {
        return subjects;
    }

    public String getKafkaRestUrl() {
        return kafkaRestUrl;
    }

    public int getMessageFlushSize() {
        return messageFlushSize;
    }

    public int getSenderPoolSize() {
        return senderPoolSize;
    }

    public long getMessageFlushPeriod() {
        return messageFlushPeriod;
    }

    public int getMessageBulkSize() {
        return messageBulkSize;
    }

    public int getSslCertTimeout() {
        return sslCertTimeout;
    }

    public String getApiKey() {
        return apiKey;
    }

    public int getShutdownTimeout() {
        return shutdownTimeout;
    }

    private Subject[] toTopics(List<String> expressions) {
        Subject[] subjects = new Subject[expressions.size()];

        int idx = 0;
        for (String expression : expressions) {
            if (expression.indexOf(':') == -1) {
                subjects[idx] = new Subject(expression, 1);
            } else {
                String[] parts = expression.split(":");
                subjects[idx] = new Subject(parts[0], toVersion(parts[1]));
            }

            idx++;
        }

        return subjects;
    }

    private int toVersion(String part) {
        if (part.startsWith("v")) {
            part = part.substring(1);
            try {
                int version = Integer.parseInt(part);
                if (version < 1) throw new ShoverConfigException("a version must be bigger than zero");

                return version;
            } catch (NumberFormatException e) {
                throw new ShoverConfigException("a version expression must be 'v' + integer", e);
            }
        } else {
            throw new ShoverConfigException("a version expression must be started with 'v' character");
        }
    }

    protected static String getStringFromConfig(Properties config, String path, String defaultValue) {
        String value = getStringFromConfig(config, path, false);
        return (value == null) ? defaultValue : value;
    }

    protected static String getStringFromConfig(Properties config, String path, boolean notNull) {
        final char[] trimChars = new char[]{' ', '\"'};
        String value = null;
        if (config.containsKey(path)) {
            value = config.getProperty(path);
            if (value != null) {
                value = trim(value, trimChars);
                if (value.length() == 0) value = null;
            }
        }

        if (notNull && value == null) throw new ShoverConfigException.WrongPathOrNullValue(path);

        return value;
    }

    protected static Integer getIntegerFromConfig(Properties config, String path, Integer defaultValue) {
        Integer value = getIntegerFromConfig(config, path, false);
        return (value == null) ? defaultValue : value;
    }

    protected static Integer getIntegerFromConfig(Properties config, String path, boolean notNull) {
        String value = getStringFromConfig(config, path, notNull);
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ShoverConfigException.WrongTypeValue(path);
        }
    }

    protected static Long getLongFromConfig(Properties config, String path, Long defaultValue) {
        Long value = getLongFromConfig(config, path, false);
        return (value == null) ? defaultValue : value;
    }

    protected static Long getLongFromConfig(Properties config, String path, boolean notNull) {
        String value = getStringFromConfig(config, path, notNull);
        if (value == null) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ShoverConfigException.WrongTypeValue(path);
        }
    }

    protected static Double getDoubleFromConfig(Properties config, String path, Double defaultValue) {
        Double value = getDoubleFromConfig(config, path, false);
        return (value == null) ? defaultValue : value;
    }

    protected static Double getDoubleFromConfig(Properties config, String path, boolean notNull) {
        String value = getStringFromConfig(config, path, notNull);
        if (value == null) return null;

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new ShoverConfigException.WrongTypeValue(path);
        }
    }

    protected static List<String> getStringListFromConfig(Properties config, String path, List<String> defaultValue) {
        List<String> values = getStringListFromConfig(config, path, false);
        return (values == null) ? defaultValue : values;
    }

    protected static List<String> getStringListFromConfig(Properties config, String path, boolean notNull) {
        final char[] trimChars = new char[]{' ', '\"'};
        List<String> values = null;
        String value = getStringFromConfig(config, path, notNull);
        if (value == null) return null;

        for (String part : value.split(",")) {
            part = trim(part, trimChars);
            if (part.length() > 0) {
                if (values == null) values = new ArrayList<>();
                values.add(part);
            }
        }

        if (notNull && values == null) throw new ShoverConfigException.WrongPathOrNullValue(path);

        return values;
    }

}
