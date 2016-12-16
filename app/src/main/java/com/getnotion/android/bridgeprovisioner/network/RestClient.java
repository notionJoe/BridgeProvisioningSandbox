/*
 * Copyright (c) 2015 Loop Labs, Inc. All rights reserved.
 */

package com.getnotion.android.bridgeprovisioner.network;

import android.getnotion.android.bridgeprovisioner.BuildConfig;
import com.getnotion.android.bridgeprovisioner.models.NotionAppModel;
import com.getnotion.android.bridgeprovisioner.models.NotionBridge;
import com.getnotion.android.bridgeprovisioner.models.NotionSystem;
import com.getnotion.android.bridgeprovisioner.models.NotionUser;
import com.getnotion.android.bridgeprovisioner.models.RealmInt;
import com.getnotion.android.bridgeprovisioner.models.RealmString;
import com.getnotion.android.bridgeprovisioner.models.serializers.NotionAppModelSerializer;
import com.getnotion.android.bridgeprovisioner.models.serializers.NotionBridgeSerializer;
import com.getnotion.android.bridgeprovisioner.models.serializers.NotionSystemSerializer;
import com.getnotion.android.bridgeprovisioner.models.serializers.NotionUserSerializer;
import android.util.Log;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.jakewharton.retrofit.Ok3Client;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.realm.RealmList;
import io.realm.RealmObject;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;

/**
 * RestClient Singleton -- uses Retrofit to turn our REST API into a Java interface
 */
public class RestClient {

    private static final String TAG = RestClient.class.getSimpleName();

    private static RestClient ourInstance = new RestClient();
    private static NotionApi notionApi;
    private static String accessToken = "";

    public static NotionApi notionApi() {
        return notionApi;
    }

    public static void setAccessToken(String token) {
        accessToken = token;
    }

    public static String getAccessToken() {
        return accessToken;
    }

    private RestClient() {
        try {
            createNotionAPIRestAdapter();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Error locating RealmProxyClass for configuring json serializer");
            e.printStackTrace();
        }
    }

    private void createNotionAPIRestAdapter() throws ClassNotFoundException {
        // Json converter -- set naming policy, exclusion strat for realm, and realm primitive array support
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                // This will automatically convert any server String dates to Date objects
                .registerTypeAdapter(Date.class, new GsonUTCdateAdapter())
                // RealmObject support for Retrofit
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass().equals(RealmObject.class);
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                // Realm int array support
                .registerTypeAdapter(new TypeToken<RealmList<RealmInt>>() {
                                     }.getType(),
                                     new TypeAdapter<RealmList<RealmInt>>() {
                                         @Override
                                         public void write(JsonWriter out, RealmList<RealmInt> value) throws IOException {
                                         }

                                         @Override
                                         public RealmList<RealmInt> read(JsonReader in) throws IOException {
                                             RealmList<RealmInt> list = new RealmList<>();
                                             in.beginArray();
                                             while (in.hasNext()) {
                                                 list.add(new RealmInt(in.nextInt()));
                                             }
                                             in.endArray();
                                             return list;
                                         }
                                     })
                // Realm string array support
                .registerTypeAdapter(new TypeToken<RealmList<RealmString>>() {
                }.getType(), new TypeAdapter<RealmList<RealmString>>() {
                    @Override
                    public void write(JsonWriter out, RealmList<RealmString> value) throws IOException {
                    }

                    @Override
                    public RealmList<RealmString> read(JsonReader in) throws IOException {
                        RealmList<RealmString> list = new RealmList<RealmString>();
                        in.beginArray();
                        while (in.hasNext()) {
                            list.add(new RealmString(in.nextString()));
                        }
                        in.endArray();
                        return list;
                    }
                })

                .registerTypeAdapter(NotionAppModel.class, new NotionAppModelSerializer())
                .registerTypeAdapter(NotionBridge.class, new NotionBridgeSerializer())
                .registerTypeAdapter(NotionSystem.class, new NotionSystemSerializer())
                .registerTypeAdapter(NotionUser.class, new NotionUserSerializer())

                // Type Adapters for serializing RealmObjects when accessed indirectly
                .registerTypeAdapter(Class.forName("io.realm.NotionAppModelRealmProxy"), new NotionAppModelSerializer())
                .registerTypeAdapter(Class.forName("io.realm.NotionBridgeRealmProxy"), new NotionBridgeSerializer())
                .registerTypeAdapter(Class.forName("io.realm.NotionSystemRealmProxy"), new NotionSystemSerializer())
                .registerTypeAdapter(Class.forName("io.realm.NotionUserRealmProxy"), new NotionUserSerializer())
                .create();

        // Intercepts all requests and adds the below headers
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("User-Agent", "Notion Android App");
                if (!getAccessToken().isEmpty()) {
                    request.addHeader("Authorization", "Token token=" + getAccessToken());
                }

            }
        };

        // Get OkHttpClientBuilder from singleton + wrap it inside Jake Wharton's Ok3Client
        // for OkHttp3 <==> Retrofit 1.9 Compatibility
        // Clunky until Dagger/Toothpick comes around...
        Ok3Client client = new Ok3Client(NotionHttpClientBuilder.getInstance().build());
        RestAdapter.Builder restAdapterBuilder = new RestAdapter.Builder()
                .setClient(client)
                .setEndpoint(BuildConfig.ENVIRONMENT.getEndpoint())
                .setConverter(new GsonConverter(gson))
                .setRequestInterceptor(requestInterceptor);

        // Set RetroFit logging to be full if debug
        if (BuildConfig.DEBUG) {
            restAdapterBuilder.setLogLevel(RestAdapter.LogLevel.FULL);
        }

        notionApi = restAdapterBuilder.build().create(NotionApi.class);
    }

    /**
     * Generic helper for parsing failed requests
     *
     * @param error the RetrofitError that the request returned
     * @return string representation of the error within the error response, e.g. "Email has already been taken"
     */
    public static String parseFailedRequest(RetrofitError error) {
        String errorString = "";

        try {
            JSONObject errorJSON = new JSONObject(new String(((TypedByteArray) error.getResponse().getBody()).getBytes()));
            if (errorJSON.has("errors")) {
                if (errorJSON.getJSONArray("errors").getJSONObject(0).has("title")) {
                    errorString = errorJSON.getJSONArray("errors").getJSONObject(0).getString("title");
                }
            }
        } catch (Exception e) {
            errorString = error.getMessage();
            Log.i(TAG, "Error parsing failed request body");
        }
        return errorString;
    }

//    public class NotionEventRealmListConverter implements
//            JsonSerializer<RealmList<NotionEvent>>, JsonDeserializer<RealmList<NotionEvent>> {
//        @Override
//        public JsonElement serialize(RealmList<NotionEvent> src, Type typeOfSrc,
//                                     JsonSerializationContext context) {
//            JsonArray ja = new JsonArray();
//            for (NotionEvent obj : src) {
//                ja.add(context.serialize(obj));
//            }
//            return ja;
//        }
//
//        @Override
//        public RealmList<NotionEvent> deserialize(
//                JsonElement json, Type typeOfT, JsonDeserializationContext context)
//                throws JsonParseException {
//            RealmList<NotionEvent> tags = new RealmList<>();
//            JsonArray ja = json.getAsJsonArray();
//            for (JsonElement je : ja) {
//                tags.add((NotionEvent) context.deserialize(je, NotionEvent.class));
//            }
//            return tags;
//        }
//    }
//
//    public class NotionSystemLinksRealmListConverter implements
//            JsonSerializer<RealmList<NotionEvent>>, JsonDeserializer<RealmList<NotionEvent>> {
//        @Override
//        public JsonElement serialize(RealmList<NotionEvent> src, Type typeOfSrc,
//                                     JsonSerializationContext context) {
//            JsonArray ja = new JsonArray();
//            for (NotionEvent obj : src) {
//                ja.add(context.serialize(obj));
//            }
//            return ja;
//        }
//
//        @Override
//        public RealmList<NotionEvent> deserialize(
//                JsonElement json, Type typeOfT, JsonDeserializationContext context)
//                throws JsonParseException {
//            RealmList<NotionEvent> tags = new RealmList<>();
//            JsonArray ja = json.getAsJsonArray();
//            for (JsonElement je : ja) {
//                tags.add((NotionEvent) context.deserialize(je, NotionEvent.class));
//            }
//            return tags;
//        }
//    }

    /**
     * Default gson date deserializer will always use the local timezone so we need a custom adapter
     * See: https://github.com/google/gson/issues/281
     */
    public static class GsonUTCdateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

        private final DateFormat dateFormat;

        public GsonUTCdateAdapter() {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public synchronized JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(dateFormat.format(date));
        }

        @Override
        public synchronized Date deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            try {
                return dateFormat.parse(jsonElement.getAsString());
            } catch (ParseException e) {
                throw new JsonParseException(e);
            }
        }
    }
}
