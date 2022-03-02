/*
 * MIT License
 *
 * Copyright (c) 2021 Thales DIS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.thalesgroup.tshpaysample.sdk.push;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gemalto.mfs.mwsdk.mobilegateway.MGSDKConfigurationState;
import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.listener.PushServiceListener;
import com.gemalto.mfs.mwsdk.provisioning.model.EnrollmentStatus;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceError;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceMessage;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.EnrollingBusinessService;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.ProvisioningBusinessService;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.enrollment.TshEnrollmentState;
import com.thalesgroup.tshpaysample.sdk.helpers.InternalNotificationsUtils;
import com.thalesgroup.tshpaysample.sdk.init.TshInitState;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TshPush implements PushServiceListener {

    //region Defines

    private static final String TAG = TshPush.class.getSimpleName();
    private static final Type TOKEN_TYPE = new TypeToken<List<ArrayMap<String, String>>>() {
    }.getType();

    private static final String SENDER_ID_CPS = "CPS";
    private static final String SENDER_ID_TNS = "TNS";
    private static final String SENDER_MESSAGE_KEY = "sender";

    private static final String SHARED_PREFERENCE_NAME = "PUSH_HELPER_STORAGE";
    private static final String PUSH_TOKEN_LOCAL_KEY = "PUSH_TOKEN_LOCAL";
    private static final String PUSH_TOKEN_REMOTE_KEY = "PUSH_TOKEN_REMOTE";
    private static final String UNPROCESSED_NOTIFICATIONS_KEY = "UNPROCESSED_NOTIFICATIONS";


    private Context mContext;
    private String mCurrentlyUpdatedToken = null;
    private boolean mInitialEnrollment = false;

    //endregion

    //region Public API

    /**
     * Main entry point which need to be called as soon as the app will run.
     */
    public void init(@NonNull final Context context) {
        mContext = context;

        // Decide which push notification service we want to use.
        if (FcmService.isAvailable(context)) {
            // Google play store
            FcmService.init(context);
        } else if (HmsService.isAvailable(context)) {
            // Huawei app gallery.
            HmsService.init();
        } else {
            throw new IllegalStateException(context.getString(R.string.push_provider_missing));
        }

        // Register for SDK state updates, so we can automatically process messages
        // and token changes.
        InternalNotificationsUtils.registerForInitChanges(context, (state, error) -> {
            if (state == TshInitState.INIT_SUCCESSFUL) {
                onSdkInitialized(context);
            }
        });
    }

    /**
     * Last token provided by either SDK locally.
     *
     * @param context Application context.
     * @return Return FCM or HMS token in proper format expected by SDK or null in case
     * it was not given yet.
     */
    public @Nullable
    String getPushTokenLocal(@NonNull final Context context) {
        return getStorage(context).getString(PUSH_TOKEN_LOCAL_KEY, null);
    }

    public void onEnrollmentStateChange(final TshEnrollmentState state) {
        if (state == TshEnrollmentState.DIGITIZATION_ACTIVATION_CODE_AQUIRED_ENROLLMENT_NEEDED) {
            // Enrollment of first card will also enroll to the CPS and update push token on server.
            // We have to track this state and make sure we update tokens accordingly.
            mInitialEnrollment = true;
        } else if (state == TshEnrollmentState.DIGITIZATION_FINISHED) {
            // Original enrollment was successful, we can mark local token as remote.
            if (mInitialEnrollment && mContext != null) {
                setPushTokenRemote(mContext, getPushTokenLocal(mContext));

                // Unlock this status in case that user will enroll more than one card during the same
                // session. In that case no token will be updated.
                mInitialEnrollment = false;
            }
        } else if (state == TshEnrollmentState.DIGITIZATION_ERROR) {
            // Unlock this status in case that user will enroll more than one card during the same
            // session. In that case no token will be updated.
            mInitialEnrollment = false;
        }
    }

    //endregion

    //region Protected API

    protected void updateToken(@NonNull final Context context,
                               @Nullable final String token) {
        // Ignore nullable input.
        if (token == null) {
            return;
        }

        // Update for this token is ongoing. We have to wait for result.
        // Failure does have auto retry mechanism.
        if (token.equalsIgnoreCase(mCurrentlyUpdatedToken)) {
            return;
        }

        // Local value can be stored all the time, there is no point of checking for difference.
        setPushTokenLocal(context, token);

        // Now we want to make sure, that local is different from last successful update of remote one.
        if (token.equalsIgnoreCase(getPushTokenRemote(context))) {
            return;
        }

        // At this point we do have a new token, however, we can update push token only when SDK is initialized.
        if (SdkHelper.getInstance().getInit().getMgSdkState() != MGSDKConfigurationState.CONFIGURED) {
            return;
        }

        // Also MG must be already enrolled. Otherwise the token will get updated during the enrollment.
        final EnrollingBusinessService enrollingService = ProvisioningServiceManager.getEnrollingBusinessService();
        if (enrollingService.isEnrolled() == EnrollmentStatus.ENROLLMENT_COMPLETE) {
            // Mark current update as ongoing so we will not try
            mCurrentlyUpdatedToken = token;

            final ProvisioningBusinessService provisioningService = ProvisioningServiceManager.getProvisioningBusinessService();
            provisioningService.updatePushToken(token, new PushServiceListener() {
                @Override
                public void onError(final ProvisioningServiceError provisioningServiceError) {
                    AppLoggerHelper.error(TAG, provisioningServiceError.getErrorMessage());

                    // Try again after few seconds.
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        // Clean up current token in order to allow run update again.
                        mCurrentlyUpdatedToken = null;

                        updateToken(context, token);
                    }, 2000);
                }

                @Override
                public void onUnsupportedPushContent(final Bundle bundle) {
                    // This method is not relevant for push update method.
                }

                @Override
                public void onServerMessage(final String message,
                                            final ProvisioningServiceMessage provisioningServiceMessage) {
                    // This method is not relevant for push update method.
                }

                @Override
                public void onComplete() {
                    // Clean up currently updated token since this operation is now done.
                    mCurrentlyUpdatedToken = null;

                    // At this point we have confirmation, that server have same token as we do.
                    setPushTokenRemote(context, token);
                }
            });
        }
    }

    protected void onMessageReceived(@NonNull final Context context,
                                     @NonNull final Map<String, String> data) {
        // Transform incoming data to bundle accepted by SDK and find original sender.
        final Bundle bundle = new Bundle();
        String sender = "";
        if (!data.isEmpty()) {
            for (final String key : data.keySet()) {
                AppLoggerHelper.debug(TAG, key + " ---|--- " + data.get(key));
                if (null != data.get(key)) {
                    bundle.putString(key, data.get(key));
                    if (SENDER_MESSAGE_KEY.equalsIgnoreCase(key)) {
                        sender = data.get(key);
                    }
                }
            }
        }

        // We can only process current message if SDK is fully initialized.
        if (SdkHelper.getInstance().getInit().getMgSdkState() == MGSDKConfigurationState.CONFIGURED) {
            if (SENDER_ID_CPS.equalsIgnoreCase(sender)) {
                final ProvisioningBusinessService provService = ProvisioningServiceManager.getProvisioningBusinessService();
                provService.processIncomingMessage(bundle, this);
            } /*else if (SENDER_ID_TNS.equalsIgnoreCase(sender)) {
                // TODO: Handle transaction history
            }*/
        } else if (SENDER_ID_CPS.equalsIgnoreCase(sender) || SENDER_ID_TNS.equalsIgnoreCase(sender)) {
            // SDK is not yet loaded. Store unprocessed notification and use it after init.
            AppLoggerHelper.error(TAG, context.getString(R.string.push_received_sdk_not_initialized));
            storeUnprocessedPushNotification(context, data);
        }
    }

    //endregion

    //region Private Helpers

    private void onSdkInitialized(@NonNull final Context context) {
        // Make sure, that notification token is up to-date.
        updateToken(context, getPushTokenLocal(context));

        // Check for any unprocessed notifications and clean them up.
        for (final Map<String, String> loopNotification : getUnprocessedPushNotification(context)) {
            onMessageReceived(context, loopNotification);
        }
        cleanUnprocessedPushNotification(context);
    }

    @NonNull
    private SharedPreferences getStorage(@NonNull final Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    private void setPushTokenLocal(@NonNull final Context context,
                                   @Nullable final String value) {
        getStorage(context).edit().putString(PUSH_TOKEN_LOCAL_KEY, value).apply();
    }

    private void setPushTokenRemote(@NonNull final Context context,
                                    @Nullable final String value) {
        getStorage(context).edit().putString(PUSH_TOKEN_REMOTE_KEY, value).apply();
    }

    private @Nullable
    String getPushTokenRemote(@NonNull final Context context) {
        return getStorage(context).getString(PUSH_TOKEN_REMOTE_KEY, null);
    }

    private @NonNull
    List<Map<String, String>> getUnprocessedPushNotification(@NonNull final Context context) {
        List<Map<String, String>> retValue = null;

        try {
            final Gson gson = new Gson();
            retValue = gson.fromJson(getStorage(context).getString(UNPROCESSED_NOTIFICATIONS_KEY, null), TOKEN_TYPE);
        } catch (final JsonParseException exception) {
            AppLoggerHelper.exception(TAG, exception.getLocalizedMessage(), exception);
        }

        if (retValue == null) {
            retValue = new ArrayList<>();
        }

        return retValue;
    }

    private void storeUnprocessedPushNotification(@NonNull final Context context,
                                                  @NonNull final Map<String, String> data) {
        // Append data to the current list.
        final List<Map<String, String>> list = getUnprocessedPushNotification(context);
        list.add(data);

        // Convert and store whole list.
        final Gson gson = new Gson();
        getStorage(context).edit().putString(UNPROCESSED_NOTIFICATIONS_KEY, gson.toJson(list)).apply();
    }

    private void cleanUnprocessedPushNotification(@NonNull final Context context) {
        getStorage(context).edit().remove(UNPROCESSED_NOTIFICATIONS_KEY).apply();
    }

    //endregion

    //region PushServiceListener

    @Override
    public void onError(final ProvisioningServiceError provisioningServiceError) {
        AppLoggerHelper.info(TAG, "onError");

        // Notify application about push processing issue.
        InternalNotificationsUtils.onPushReceived(mContext, TshPushType.UNKNOWN, provisioningServiceError.getErrorMessage());
    }

    @Override
    public void onUnsupportedPushContent(final Bundle bundle) {
        AppLoggerHelper.info(TAG, "onUnsupportedPushContent");

        // Irrelevant for sample application.
    }

    @Override
    public void onServerMessage(final String tokenizedCardId,
                                final ProvisioningServiceMessage provisioningServiceMessage) {
        AppLoggerHelper.info(TAG, "onServerMessage");

        // Notify application about incoming push message.
        InternalNotificationsUtils.onPushReceived(mContext, TshPushType.getTypeFromString(provisioningServiceMessage.getMsgCode()), null);
    }

    @Override
    public void onComplete() {
        AppLoggerHelper.info(TAG, "onComplete");

        // Triggered when entire incoming is processed. Application is reacting on individual
        // messages, so we do not need to handle this as well.
    }

    //endregion

}
