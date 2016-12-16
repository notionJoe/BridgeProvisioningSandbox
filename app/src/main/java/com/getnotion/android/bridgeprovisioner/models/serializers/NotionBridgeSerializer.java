package com.getnotion.android.bridgeprovisioner.models.serializers;

import com.getnotion.android.bridgeprovisioner.models.NotionBridge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Date;

/**
 * Serializer for the NotionBridgeRealmProxy object
 * <p/>
 * Note: Necessary since GSON will try to serialize the RealmProxy object and not the RealmObject
 * itself. Please see: https://github.com/realm/realm-java/issues/1127
 */
public class NotionBridgeSerializer implements JsonSerializer<NotionBridge> {
    @Override
    public JsonElement serialize(NotionBridge src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("id", src.getId());
        object.addProperty("name", src.getName());
        object.addProperty("mode", src.getMode());
        object.addProperty("hardware_id", src.getHardwareId());
        object.addProperty("firmware_version", src.getFirmwareVersion());
        object.addProperty("missing_at", context.serialize(src.getMissingAt(), Date.class).toString());
        object.addProperty("created_at", context.serialize(src.getCreatedAt(), Date.class).toString());
        object.addProperty("updated_at", context.serialize(src.getUpdatedAt(), Date.class).toString());
        object.addProperty("system_id", src.getSystemId());
        return object;
    }
}
