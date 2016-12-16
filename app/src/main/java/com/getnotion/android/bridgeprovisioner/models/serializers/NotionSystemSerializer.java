package com.getnotion.android.bridgeprovisioner.models.serializers;

import com.getnotion.android.bridgeprovisioner.models.NotionSystem;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Date;

/**
 * Serializer for the NotionSystemRealmProxy object
 * <p/>
 * Note: Necessary since GSON will try to serialize the RealmProxy object and not the RealmObject
 * itself. Please see: https://github.com/realm/realm-java/issues/1127
 */
public class NotionSystemSerializer implements JsonSerializer<NotionSystem> {
    @Override
    public JsonElement serialize(NotionSystem src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("id", src.getId());
        object.addProperty("name", src.getName());
        object.addProperty("mode", src.getMode());
        object.addProperty("latitude", src.getLatitude());
        object.addProperty("longitude", src.getLongitude());
        object.addProperty("timezone_id", src.getTimezoneId());
        object.addProperty("device_timezone_id", src.getDeviceTimezoneId());
        object.addProperty("locality", src.getLocality());
        object.addProperty("administrative_area", src.getAdministrativeArea());
        object.addProperty("postal_code", src.getPostalCode());
        object.addProperty("fire_number", src.getFireNumber());
        object.addProperty("police_number", src.getPoliceNumber());
        object.addProperty("emergency_number", src.getEmergencyNumber());
        object.addProperty("night_time_start", context.serialize(src.getNightTimeStart(), Date.class).toString());
        object.addProperty("night_time_end", context.serialize(src.getNightTimeEnd(), Date.class).toString());
        object.addProperty("created_at", context.serialize(src.getCreatedAt(), Date.class).toString());
        object.addProperty("updated_at", context.serialize(src.getUpdatedAt(), Date.class).toString());
        return object;
    }
}
