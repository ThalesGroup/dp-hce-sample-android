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

package com.thalesgroup.tshpaysample.sdk;

public class SdkConstants {

    /*
    MG configuration API described in our documentation:
    https://thales-dis-dbp.stoplight.io/docs/tsh-hce-android/ZG9jOjM2MTc0MDEz-introduction
     */

    /*
    An ID which identifies the Wallet Provider uniquely.
     */
    public static final String WALLET_PROVIDER_ID = "<TO_BE_CONFIGURED>";

    /*
    The MG URL which the client will connect to.
     */
    public static final String MG_CONNECTION_URL = "<TO_BE_CONFIGURED>";

    /*
    The server URL which the client connect to retrieve the transaction history and notifications.
     */
    public static final String MG_TRANSACTION_HISTORY_CONNECTION_URL = "<TO_BE_CONFIGURED>";

    /*
    The maximum time limit in milliseconds to connect to the server.
     */
    public static final int MG_CONNECTION_TIMEOUT = 0; // <TO_BE_CONFIGURED>

    /*
    The maximum time limit in milliseconds to read the response from the server.
     */
    public static final int MG_CONNECTION_READ_TIMEOUT = 0; // <TO_BE_CONFIGURED>

    /*
    Number of retries that the MG module can perform. Default value is set to 0.
     */
    public static final int MG_CONNECTION_RETRY_COUNT = 0; // <TO_BE_CONFIGURED>

    /*
    The interval in milliseconds before the MG module calls the server. Default value is set to 10000 ms.
     */
    public static final int MG_CONNECTION_RETRY_INTERVAL = 0; // <TO_BE_CONFIGURED>

    /*
    Values used in sample app for calculation encrypted card data.
    Both of them are agreed during on-boarding phase. For more information please visit documentation:
    https://thales-dis-dbp.stoplight.io/docs/tsh-hce-android/ZG9jOjI5NDIzNDU0-onboarding
     */
    public static final String SUBJECT_IDENTIFIER = "<TO_BE_CONFIGURED>";
    public static final String PUBLIC_KEY = "<TO_BE_CONFIGURED>";
}
