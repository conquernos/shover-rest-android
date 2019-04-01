package org.conquernos.shover.android.schema;


import com.google.gson.JsonObject;
import org.conquernos.shover.android.exceptions.schema.ShoverSchemaException;
import org.conquernos.shover.android.exceptions.schema.ShoverSchemaNotExistException;
import org.conquernos.shover.android.exceptions.schema.ShoverSchemaRegistryException;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;


public class ShoverSchemaRegistryClient {

    private interface SchemaSubject {
        @GET("/schemas/{subject}")
        Call<JsonObject> send(@Path("subject") String subject);
    }

    private interface SchemaSubjectVersion {
        @GET("/schemas/{subject}/{version}")
        Call<JsonObject> send(@Path("subject") String subject, @Path("version") int version);
    }

    private final SchemaSubject schemaSubject;
    private final SchemaSubjectVersion schemaSubjectVersion;

    public ShoverSchemaRegistryClient(String kafkaRestUrl) {
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(kafkaRestUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        schemaSubject = retrofit.create(SchemaSubject.class);
        schemaSubjectVersion = retrofit.create(SchemaSubjectVersion.class);
    }

    public JsonObject getLatestSchema(String subject) throws ShoverSchemaException {
        Call<JsonObject> call = schemaSubject.send(subject);
        try {
            Response<JsonObject> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                throw new ShoverSchemaNotExistException("schema error : " + subject);
            }
        } catch (Exception e) {
            throw new ShoverSchemaRegistryException("schema-registry error");
        }
    }

    public JsonObject getSchema(String subject, int version) throws ShoverSchemaException {
        Call<JsonObject> call = schemaSubjectVersion.send(subject, version);
        try {
            Response<JsonObject> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                throw new ShoverSchemaNotExistException("schema error : " + subject);
            }
        } catch (Exception e) {
            throw new ShoverSchemaRegistryException("schema-registry error");
        }
    }

}

