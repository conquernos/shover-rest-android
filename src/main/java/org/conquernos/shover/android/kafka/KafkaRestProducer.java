package org.conquernos.shover.android.kafka;


import com.google.gson.JsonObject;
import org.conquernos.shover.android.cert.CertInstaller;
import org.conquernos.shover.android.schema.Subject;
import org.conquernos.shover.android.stats.ShoverStats;
import org.conquernos.shover.android.utils.Loader;
import okhttp3.*;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class KafkaRestProducer {

    /*
     * Produce a message using Avro embedded data, including the schema which will
     * be registered with the schema registry and used to validate and serialize
     * before storing the data in Kafka
     * $ curl -X POST -H "Content-Type: application/vnd.kafka.avro.v1+json" \
     *   --data '{"value_schema": "{\"type\": \"record\", \"name\": \"User\", \"fields\": [{\"name\": \"name\", \"type\": \"string\"}]}", "records": [{"value": {"name": "testUser"}}]}' \
     *   "http://localhost:8082/topics/avrotest"
     * {"value_schema_id":0,"offsets":[{"partition":0,"offset":0}]}
     */
    interface KafkaRestApi {
        @Headers({
            "Content-Type: application/vnd.kafka.avro.v1+json"
        })
        @POST("/topics/{topic}")
        Call<JsonObject> send(@Path("topic") String topic, @Body JsonObject data);
    }

    private final RestSenderPool senderPool;

    private final ShoverStats stats;

    public KafkaRestProducer(String kafkaRestUrl, Subject[] subjects, ShoverStats stats, int senderPoolSize, int flushSize, long flushPeriod, int bulkSize, String apiKey, int sslCertTimeout) {
        HttpUrl httpUrl = HttpUrl.parse(kafkaRestUrl);

        // get certificate
        CertInstaller certInstaller = new CertInstaller();
        InputStream cert = Loader.getResourceInputStream("src/main/resources/cert");
        if (cert != null) {
            certInstaller.certificate(cert);
        } else {
            certInstaller.certificate(httpUrl.host(), httpUrl.port(), sslCertTimeout);
        }

        // ssl client
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        builder.sslSocketFactory(certInstaller.getContext().getSocketFactory(), certInstaller.getX509TrustManager())
            .hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
        builder.addInterceptor(chain -> chain.proceed(chain.request().newBuilder().addHeader("Api-Key", apiKey).build()));
        OkHttpClient httpClient = builder.build();

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(httpUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build();

        final KafkaRestApi restApi = retrofit.create(KafkaRestApi.class);

        final Map<String, Integer> versions = new HashMap<>();

        for (Subject subject : subjects) versions.put(subject.getSubject(), subject.getVersion());

        senderPool = new RestSenderPool(restApi, versions, stats, senderPoolSize, flushSize, flushPeriod, bulkSize);

        this.stats = stats;
    }

    public void send(String topic, JsonObject record) {
        try {
            senderPool.send(topic, record);
        } catch (InterruptedException e) {
            stats.addNumberOfFailedMessages();
        }
    }

    public void close() {
        senderPool.close();
    }

}
