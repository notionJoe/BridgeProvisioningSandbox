package com.getnotion.android.bridgeprovisioner.utils;

import android.content.Context;
import android.getnotion.android.bridgeprovisioner.R;
import com.getnotion.android.bridgeprovisioner.models.NotionSystem;

import retrofit.RetrofitError;

/**
 * Utils around NotionSystem objects
 */
public class SystemUtils {

    /**
     * Get a reader friendly string of the location for a NotionSystem
     * <p/>
     * e.g.
     * Denver, CO 80210
     *
     * @param system NotionSystem to
     * @return reader friendly location string (e.g. Denver, CO 80210)
     */
    public static String getReaderFriendlyLocationString(NotionSystem system) {
        return String.format("%s, %s %s",
                             system.getLocality(),
                             system.getAdministrativeArea(),
                             system.getPostalCode());
    }

    /**
     * Gets the appropriate user facing error message for the response code returned when attempting
     * to add a system resource.
     *
     * @param ctx           Current context for fetching the string resource
     * @param retrofitError Error provided by retrofit for looking at kind and response code
     * @return User facing error string
     */
    public static String getErrorMessageForResponse(Context ctx, RetrofitError retrofitError) {

        String error = ctx.getString(R.string.dialogs_systemSetupError_message);

        if (retrofitError.getKind() == RetrofitError.Kind.NETWORK) {
            error = ctx.getString(R.string.errors_bridge_responseCodeErrors_networkNotAvailable);
        } else if (retrofitError.getKind() == RetrofitError.Kind.HTTP && retrofitError.getResponse() != null) {
            switch (retrofitError.getResponse().getStatus()) {
                // Response code errors here
            }
        }

        return error;
    }
}
