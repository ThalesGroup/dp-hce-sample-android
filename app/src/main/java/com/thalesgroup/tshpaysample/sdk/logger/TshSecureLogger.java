/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.logger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.gemalto.mfs.mwsdk.payment.sdkconfig.SDKInitializer;
import com.gemalto.mfs.mwsdk.sdkconfig.SecureLogConstants;
import com.thalesgroup.gemalto.securelog.SecureLog;
import com.thalesgroup.gemalto.securelog.SecureLogConfig;
import com.thalesgroup.gemalto.securelog.SecureLogLevel;
import com.thalesgroup.tshpaysample.BuildConfig;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TshSecureLogger {

    //region Defines

    private static final byte[] SL_MODULUS = {
            (byte) 0x00, (byte) 0xd4, (byte) 0x6d, (byte) 0x5c, (byte) 0x06, (byte) 0x35, (byte) 0xb0,
            (byte) 0x52, (byte) 0x2f, (byte) 0x3e, (byte) 0xf4, (byte) 0x14, (byte) 0xd8, (byte) 0x3d,
            (byte) 0xf2, (byte) 0xd7, (byte) 0xf5, (byte) 0x1b, (byte) 0x54, (byte) 0x7e, (byte) 0x01,
            (byte) 0x0b, (byte) 0x1c, (byte) 0x23, (byte) 0x60, (byte) 0x04, (byte) 0xde, (byte) 0x4c,
            (byte) 0x67, (byte) 0x3e, (byte) 0xf8, (byte) 0x3b, (byte) 0x2b, (byte) 0xdd, (byte) 0xfa,
            (byte) 0x50, (byte) 0x87, (byte) 0xe7, (byte) 0xb3, (byte) 0x03, (byte) 0x22, (byte) 0x93,
            (byte) 0x87, (byte) 0xdd, (byte) 0xaf, (byte) 0x0a, (byte) 0xdd, (byte) 0xf9, (byte) 0xee,
            (byte) 0x8b, (byte) 0x60, (byte) 0x45, (byte) 0x1a, (byte) 0x6b, (byte) 0xf9, (byte) 0x49,
            (byte) 0xfd, (byte) 0x64, (byte) 0x0f, (byte) 0xbd, (byte) 0xe1, (byte) 0x85, (byte) 0x7e,
            (byte) 0x40, (byte) 0xe1, (byte) 0x52, (byte) 0x10, (byte) 0xec, (byte) 0xae, (byte) 0x93,
            (byte) 0xfd, (byte) 0x61, (byte) 0xb7, (byte) 0xfc, (byte) 0xdb, (byte) 0x5f, (byte) 0x60,
            (byte) 0xa0, (byte) 0xbf, (byte) 0x10, (byte) 0x94, (byte) 0x76, (byte) 0x15, (byte) 0x8c,
            (byte) 0x9b, (byte) 0x7c, (byte) 0xcd, (byte) 0xd7, (byte) 0xa7, (byte) 0xa5, (byte) 0x29,
            (byte) 0x1f, (byte) 0x31, (byte) 0x9a, (byte) 0xd0, (byte) 0x2e, (byte) 0xa2, (byte) 0x4f,
            (byte) 0x26, (byte) 0xe9, (byte) 0x14, (byte) 0x98, (byte) 0x99, (byte) 0xa6, (byte) 0x12,
            (byte) 0x1c, (byte) 0xb5, (byte) 0xac, (byte) 0x19, (byte) 0x99, (byte) 0xae, (byte) 0x23,
            (byte) 0xc8, (byte) 0x75, (byte) 0xea, (byte) 0xc0, (byte) 0xe0, (byte) 0x10, (byte) 0x31,
            (byte) 0x02, (byte) 0xf1, (byte) 0x4a, (byte) 0x97, (byte) 0xa5, (byte) 0xe2, (byte) 0xb0,
            (byte) 0xfd, (byte) 0x06, (byte) 0x70, (byte) 0xd2, (byte) 0xa5, (byte) 0x5a, (byte) 0xed,
            (byte) 0xe2, (byte) 0x9e, (byte) 0xea, (byte) 0x6f, (byte) 0x05, (byte) 0x06, (byte) 0x64,
            (byte) 0xa0, (byte) 0xf3, (byte) 0x5d, (byte) 0xba, (byte) 0x48, (byte) 0x4b, (byte) 0x18,
            (byte) 0xd1, (byte) 0x7b, (byte) 0xef, (byte) 0x48, (byte) 0x22, (byte) 0x8f, (byte) 0xdb,
            (byte) 0x5c, (byte) 0x07, (byte) 0xf0, (byte) 0x96, (byte) 0xfe, (byte) 0xfb, (byte) 0xac,
            (byte) 0xf1, (byte) 0xb0, (byte) 0x13, (byte) 0x0d, (byte) 0x3f, (byte) 0xe0, (byte) 0x8e,
            (byte) 0x81, (byte) 0xae, (byte) 0x73, (byte) 0xef, (byte) 0x5c, (byte) 0xd4, (byte) 0x11,
            (byte) 0x37, (byte) 0x85, (byte) 0x80, (byte) 0x9f, (byte) 0xdc, (byte) 0x19, (byte) 0x05,
            (byte) 0x49, (byte) 0xde, (byte) 0x34, (byte) 0xfe, (byte) 0x20, (byte) 0x54, (byte) 0x2d,
            (byte) 0xe6, (byte) 0xcc, (byte) 0x33, (byte) 0x19, (byte) 0x82, (byte) 0x0c, (byte) 0xc5,
            (byte) 0x9e, (byte) 0x42, (byte) 0xbe, (byte) 0x27, (byte) 0xf2, (byte) 0x7b, (byte) 0xaa,
            (byte) 0xfc, (byte) 0x7f, (byte) 0x11, (byte) 0x43, (byte) 0x83, (byte) 0x8c, (byte) 0xde,
            (byte) 0x71, (byte) 0xdd, (byte) 0x8b, (byte) 0xd5, (byte) 0x08, (byte) 0xb7, (byte) 0xcc,
            (byte) 0xc5, (byte) 0x0a, (byte) 0xf9, (byte) 0x91, (byte) 0xdc, (byte) 0x78, (byte) 0x68,
            (byte) 0x12, (byte) 0x64, (byte) 0x9d, (byte) 0x35, (byte) 0x89, (byte) 0x1e, (byte) 0xcc,
            (byte) 0x23, (byte) 0x7a, (byte) 0x11, (byte) 0x21, (byte) 0x77, (byte) 0x2a, (byte) 0xc4,
            (byte) 0xad, (byte) 0xc4, (byte) 0x2f, (byte) 0xcf, (byte) 0xec, (byte) 0x21, (byte) 0x50,
            (byte) 0x9e, (byte) 0x32, (byte) 0xf9, (byte) 0xa3, (byte) 0x2a, (byte) 0x27, (byte) 0x33,
            (byte) 0x27, (byte) 0x4d, (byte) 0x24, (byte) 0x78, (byte) 0x59
    };

    private static final byte[] SL_EXPONENT = {
            (byte) 0x01, (byte) 0x00, (byte) 0x01
    };

    private static final String TAG = TshSecureLogger.class.getSimpleName();

    private SecureLog mSecureLog;

    //endregion

    //region Public API

    /**
     * Main entry point which need to be called as soon as the app will run.
     */
    public void init(@NonNull final Context context) {
        final SecureLogConfig.Builder builder = new SecureLogConfig.Builder(context)
                .directory(new File(context.getFilesDir(), SecureLogConstants.DEFAULT_DIRECTORY))
                .fileID(SecureLogConstants.DEFAULT_FILEID)
                .publicKey(SL_MODULUS, SL_EXPONENT)
                .level(SecureLogConstants.DEFAULT_LEVEL)
                .rollingFileMaxCount(SecureLogConstants.DEFAULT_MAX_FILE_COUNT)
                .rollingFileMaxSizeInKB(SecureLogConstants.DEFAULT_MAX_FILE_SIZE);

        if (BuildConfig.DEBUG) {
            builder.level(SecureLogLevel.ALL);
        } else {
            builder.level(SecureLogLevel.OFF);
        }

        mSecureLog = SDKInitializer.INSTANCE.configureSecureLog(builder.build());
    }

    public void deleteLogs() {
        if (mSecureLog == null) {
            AppLoggerHelper.error(TAG, "Secure logging is not initialised");
            return;
        }

        mSecureLog.deleteFiles();
    }

    public void shareSecureLog(final Activity activity) {
        if (mSecureLog == null) {
            AppLoggerHelper.error(TAG, "Secure logging is not initialised");
            return;
        }

        final List<File> files = mSecureLog.getFiles();
        if (files == null) {
            // No log to share
            return;
        }

        // get parent file as the directory
        final File directory = files.get(0).getParentFile();
        final String outputZipPath = activity.getCacheDir().getAbsolutePath() + "/temp.zip";

        final Thread zipTask = new Thread(() -> {
            final File file = new File(outputZipPath);
            // delete existing cached zip
            if (!file.exists()) {
                file.delete();
            }

            zipFolder(directory.getAbsolutePath(), outputZipPath);

            // try to share the file by email
            final Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"youremail@xxx.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "SecureLog files");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Attachment with logs.");

            if (!file.exists() || !file.canRead()) {
                return;
            }

            final Uri uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            activity.startActivity(Intent.createChooser(emailIntent, "Pick an Email provider"));
        });

        zipTask.run();
    }

    //endregion

    //region Private Helpers

    private void zipFolder(final String inputFolderPath, final String outZipPath) {
        try {
            final FileOutputStream fos = new FileOutputStream(outZipPath);
            final ZipOutputStream zos = new ZipOutputStream(fos);
            final File srcFile = new File(inputFolderPath);
            final File[] files = srcFile.listFiles();

            for (final File file : files) {
                final byte[] buffer = new byte[1024];
                final FileInputStream fis = new FileInputStream(file);
                zos.putNextEntry(new ZipEntry(file.getName()));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
                fis.close();
            }
            zos.close();
        } catch (final IOException ioe) {
            AppLoggerHelper.error(TAG, ioe.getMessage());
        }
    }

    //endregion
}
