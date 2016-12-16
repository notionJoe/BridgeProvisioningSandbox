package com.getnotion.android.bridgeprovisioner.models.serializers;

import com.getnotion.android.bridgeprovisioner.models.NotionAppModel;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Serializer for the NotionAppModelRealmProxy object
 * <p/>
 * Note: Necessary since GSON will try to serialize the RealmProxy object and not the RealmObject
 * itself. Please see: https://github.com/realm/realm-java/issues/1127
 */
public class NotionAppModelSerializer implements JsonSerializer<NotionAppModel> {
    @Override
    public JsonElement serialize(NotionAppModel src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("app_model_version", src.getAppModelVersion());
        object.addProperty("device_id", src.getDeviceId());
        object.addProperty("gcm_token", src.getGcmToken());
        object.addProperty("selected_system_id", src.getSelectedSystemId());


        object.addProperty("logged_in", src.isLoggedIn());
        object.addProperty("push_enabled", src.isPushEnabled());
        object.addProperty("is_bridge_provisioned", src.isBridgeProvisioned());
        object.addProperty("setup_complete", src.isSetupComplete());

        return object;
    }
}
