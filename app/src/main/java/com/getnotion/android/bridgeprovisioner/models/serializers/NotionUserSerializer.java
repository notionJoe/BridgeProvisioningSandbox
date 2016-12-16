package com.getnotion.android.bridgeprovisioner.models.serializers;

import com.getnotion.android.bridgeprovisioner.models.NotionUser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Date;

/**
 * Serializer for the NotionUserRealmProxy object
 * <p/>
 * Note: Necessary since GSON will try to serialize the RealmProxy object and not the RealmObject
 * itself. Please see: https://github.com/realm/realm-java/issues/1127
 */
public class NotionUserSerializer implements JsonSerializer<NotionUser> {
    @Override
    public JsonElement serialize(NotionUser src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("id", src.getId());
        object.addProperty("first_name", src.getFirstName());
        object.addProperty("last_name", src.getLastName());
        object.addProperty("authentication_token", src.getAuthenticationToken());
        object.addProperty("email", src.getEmail());
        object.addProperty("password", src.getPassword());
        object.addProperty("phone_number", src.getPhoneNumber());
        object.addProperty("role", src.getRole());
        object.addProperty("organization", src.getOrganization());
        object.addProperty("created_at", context.serialize(src.getCreatedAt(), Date.class).toString());
        object.addProperty("updated_at", context.serialize(src.getUpdatedAt(), Date.class).toString());
        return object;
    }
}
