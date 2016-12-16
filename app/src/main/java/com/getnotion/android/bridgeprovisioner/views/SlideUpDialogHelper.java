package com.getnotion.android.bridgeprovisioner.views;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.getnotion.android.bridgeprovisioner.BuildConfig;
import android.getnotion.android.bridgeprovisioner.R;
import android.net.Uri;
import android.provider.Settings;

/**
 * Created by TheMDP on 9/25/15.
 *
 * Here is a place for common shared implementations of slide up dialogs to live
 */
public class SlideUpDialogHelper {

    public static void showFeatureNotImplemented(Context ctx) {
        // Don't worry about hard-coded strings, this should never reach production
        // and dont want to waste money on translating if it does accidentally make it in
        new SlideUpDialog.OkBuilder()
                .colorTheme(SlideUpDialog.ColorTheme.RED)
                .title("Feature not implemented")
                .message("It will be included in the release of a future version")
                .build(ctx).show();
    }

    public static void showNetworkConnectionError(Context ctx) {
        new SlideUpDialog.OkBuilder()
                .colorTheme(SlideUpDialog.ColorTheme.RED)
                .title(ctx.getString(R.string.dialogs_networkError_title))
                .message(ctx.getString(R.string.dialogs_networkError_title))
                .build(ctx).show();
    }

    /**
     * Shows and error dialog for a failed API call for fetching resources
     * @param ctx context of current Activity/Fragment
     * @param message message to display in the error -- usually RetrofitError.getMessage()
     */
    public static void showErrorFetchingResources(Context ctx, String message) {
        new SlideUpDialog.OkBuilder()
                .colorTheme(SlideUpDialog.ColorTheme.RED)
                .title(ctx.getString(R.string.dialogs_networkError_title))
                .message(message)
                .build(ctx).show();
    }

    /**
     * Shows error dialog informing that the provided permission is required to continue and option
     * to open the application settings
     * @param ctx context of current Activity/Fragment
     * @param permissionGroup Manifest.permission_group of the permission required
     */
    public static void showPermissionsRequiredDialog(final Context ctx, String permissionGroup) {
        String message = "Specific permissions required to continue";
        switch(permissionGroup) {
            case Manifest.permission_group.LOCATION: message = ctx.getString(R.string.dialogs_permissions_location_message); break;
            case Manifest.permission_group.CAMERA: message = ctx.getString(R.string.dialogs_permissions_camera_message); break;
            case Manifest.permission_group.CONTACTS: message = ctx.getString(R.string.dialogs_permissions_contact_message); break;
            default: break;
        }

        new SlideUpDialog.Builder()
                .colorTheme(SlideUpDialog.ColorTheme.RED)
                .title(ctx.getString(R.string.dialogs_permissions_title))
                .message(message)
                .confirmTitle(ctx.getString(R.string.dialogs_permissions_confirmTitle))
                .confirmClickListener(new SlideUpDialog.OnConfirmListener() {
                    @Override
                    public void onConfirm(Dialog dialog) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        intent.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                        ctx.startActivity(intent);
                        dialog.dismiss();
                    }
                })
                .cancelTitle(ctx.getString(R.string.global_cancel))
                .build(ctx).show();
    }
}
