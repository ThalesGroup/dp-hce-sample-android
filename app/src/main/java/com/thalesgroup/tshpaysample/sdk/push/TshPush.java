/*
 * Copyright © 2021-2022 THALES. All rights reserved.
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
import com.thalesgroup.tshpaysample.sdk.helpers.CardWrapper;
import com.thalesgroup.tshpaysample.sdk.helpers.InternalNotificationsUtils;
import com.thalesgroup.tshpaysample.sdk.init.TshInitState;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public final class TshPush implements PushServiceListener {

    //region Defines

    interface TshMessageDelegate {
        void onMessageProcessed(final Bundle bundle,
                                final TshPushSender sender,
                                final String action,
                                final String digitalizedCardId);
    }

    private static final String TAG = TshPush.class.getSimpleName();
    private static final Type TOKEN_TYPE = new TypeToken<List<ArrayMap<String, String>>>() {
    }.getType();

    private static final String KEY_SENDER = "sender";
    private static final String KEY_ACTION = "action";
    private static final String KEY_DIGITALIZED_CARD_ID = "digitalCardID";
    private static final String KEY_REPLENISHMENT = "MG:ReplenishmentNeededNotification";

    private static final String SHARED_PREFERENCE_NAME = "PUSH_HELPER_STORAGE";
    private static final String PUSH_TOKEN_LOCAL_KEY = "PUSH_TOKEN_LOCAL";
    private static final String PUSH_TOKEN_REMOTE_KEY = "PUSH_TOKEN_REMOTE";
    private static final String UNPROCESSED_NOTIFICATIONS_KEY = "UNPROCESSED_NOTIFICATIONS";


    private Context mContext;
    private String mCurrentlyUpdatedToken = null;
    private boolean mInitialEnrollment = false;

    // Used for tracking server message codes that were processed
    private Stack<String> mPushServerMessageCodes;

    //endregion

    //region Public API

    /**
     * Main entry point which need to be called as soon as the app will run.
     */
    public void init(@NonNull final Context context) {
        mContext = context;
        mPushServerMessageCodes = new Stack<>();

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
        processTshMessage(context, data, (bundle, sender, action, digitalizedCardId) -> {
            switch (sender) {
                case CPS:
                    // Main notification that needs to be handled in order to progress with
                    // enrollment, replenishment, card state changes etc...
                    final ProvisioningBusinessService provService = ProvisioningServiceManager.getProvisioningBusinessService();
                    provService.processIncomingMessage(bundle, TshPush.this);
                    break;
                case MG:
                    // We run out of keys and backend is asking wallet to force replenishment.
                    if (KEY_REPLENISHMENT.equalsIgnoreCase(action) &&
                            digitalizedCardId != null && !digitalizedCardId.isEmpty()) {
                        new CardWrapper(digitalizedCardId).replenishKeysIfNeeded(true);
                    }
                    break;
                case TNS:
                    // Transaction history notification. Not in current scope of sample application.
                    AppLoggerHelper.error(TAG, context.getString(R.string.push_received_transaction_history));
                    break;
                case UNKNOWN:
                    // Notification is not from Tsh. This is example how app can handle
                    // own push notification.
                    AppLoggerHelper.error(TAG, context.getString(R.string.push_received_unknown));
                    break;
            }
        });
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

    private void processTshMessage(@NonNull final Context context,
                                   @NonNull final Map<String, String> data,
                                   @NonNull final TshMessageDelegate delegate) {
        String retSender = "";
        String retDigitalCardID = "";
        String retAction = "";

        final Bundle bundle = new Bundle();
        if (!data.isEmpty()) {
            for (final String loopKey : data.keySet()) {
                final String value = data.get(loopKey);
                AppLoggerHelper.debug(TAG, loopKey + " ---|--- " + value);
                if (null != data.get(loopKey)) {
                    bundle.putString(loopKey, value);
                    if (KEY_SENDER.equalsIgnoreCase(loopKey)) {
                        retSender = value;
                    } else if (KEY_ACTION.equalsIgnoreCase(loopKey)) {
                        retAction = value;
                    } else if (KEY_DIGITALIZED_CARD_ID.equalsIgnoreCase(loopKey)) {
                        retDigitalCardID = value;
                    }
                }
            }
        }

        // We will return data back only in case that SDK is fully initialized,
        // or received message is not from TSH. If the message is Tsh, but SDK is not initialized,
        // we will store data for future processing.
        final TshPushSender pushSender = TshPushSender.senderFromString(retSender);
        final boolean isTshConfigured = SdkHelper.getInstance().getInit().getMgSdkState() == MGSDKConfigurationState.CONFIGURED;
        if (isTshConfigured || pushSender == TshPushSender.UNKNOWN) {
            delegate.onMessageProcessed(bundle, pushSender, retAction, retDigitalCardID);
        } else {
            // SDK is not yet loaded. Store unprocessed notification and use it after init.
            AppLoggerHelper.error(TAG, context.getString(R.string.push_received_sdk_not_initialized));
            storeUnprocessedPushNotification(context, data);
        }
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
                                final ProvisioningServiceMessage msg) {
        AppLoggerHelper.info(TAG, "onServerMessage: " + msg.getMsgCode());

        // add them provisioning message to the list and process them only at the completion time
        mPushServerMessageCodes.push(msg.getMsgCode());
    }

    @Override
    public void onComplete() {
        AppLoggerHelper.info(TAG, "onComplete: " + String.join(",", mPushServerMessageCodes));

        // Notify application about the latest server message processed
        // This is an optimization which avoids for example issuing a replenishment request
        // just after the PROV_CARD command ("msgCode" : "CBP.info.tokenProvisioned") is processed
        TshPushType lastServerMessage = TshPushType.UNKNOWN;
        if(!mPushServerMessageCodes.empty()) {
            lastServerMessage = TshPushType.getTypeFromString(mPushServerMessageCodes.pop());
        }
        InternalNotificationsUtils.onPushReceived(mContext, lastServerMessage, null);

        mPushServerMessageCodes.clear();
        mPushServerMessageCodes = new Stack<>();
    }

    //endregion

}
