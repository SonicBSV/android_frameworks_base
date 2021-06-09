/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.appsearch.stats;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.NonNull;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.SparseIntArray;

import androidx.test.core.app.ApplicationProvider;

import com.android.server.appsearch.external.localstorage.stats.CallStats;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class PlatformLoggerTest {
    private static final int TEST_MIN_TIME_INTERVAL_BETWEEN_SAMPLES_MILLIS = 100;
    private static final int TEST_DEFAULT_SAMPLING_INTERVAL = 10;
    private static final String TEST_PACKAGE_NAME = "packageName";

    private final Map<UserHandle, PackageManager> mMockPackageManagers = new ArrayMap<>();
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        mContext = new ContextWrapper(context) {
            @Override
            public Context createContextAsUser(UserHandle user, int flags) {
                return new ContextWrapper(super.createContextAsUser(user, flags)) {
                    @Override
                    public PackageManager getPackageManager() {
                        return getMockPackageManager(user);
                    }
                };
            }
        };
    }

    @Test
    public void testCreateExtraStatsLocked_nullSamplingIntervalMap_returnsDefault() {
        PlatformLogger logger = new PlatformLogger(
                ApplicationProvider.getApplicationContext(),
                UserHandle.of(UserHandle.USER_NULL),
                new PlatformLogger.Config(
                        TEST_MIN_TIME_INTERVAL_BETWEEN_SAMPLES_MILLIS,
                        TEST_DEFAULT_SAMPLING_INTERVAL,
                        /*samplingIntervals=*/ new SparseIntArray()));

        // Make sure default sampling interval is used if samplingMap is not provided.
        assertThat(logger.createExtraStatsLocked(TEST_PACKAGE_NAME,
                CallStats.CALL_TYPE_UNKNOWN).mSamplingInterval).isEqualTo(
                TEST_DEFAULT_SAMPLING_INTERVAL);
        assertThat(logger.createExtraStatsLocked(TEST_PACKAGE_NAME,
                CallStats.CALL_TYPE_INITIALIZE).mSamplingInterval).isEqualTo(
                TEST_DEFAULT_SAMPLING_INTERVAL);
        assertThat(logger.createExtraStatsLocked(TEST_PACKAGE_NAME,
                CallStats.CALL_TYPE_SEARCH).mSamplingInterval).isEqualTo(
                TEST_DEFAULT_SAMPLING_INTERVAL);
        assertThat(logger.createExtraStatsLocked(TEST_PACKAGE_NAME,
                CallStats.CALL_TYPE_FLUSH).mSamplingInterval).isEqualTo(
                TEST_DEFAULT_SAMPLING_INTERVAL);
    }


    @Test
    public void testCreateExtraStatsLocked_with_samplingIntervalMap_returnsConfigured() {
        int putDocumentSamplingInterval = 1;
        int querySamplingInterval = 2;
        final SparseIntArray samplingIntervals = new SparseIntArray();
        samplingIntervals.put(CallStats.CALL_TYPE_PUT_DOCUMENT, putDocumentSamplingInterval);
        samplingIntervals.put(CallStats.CALL_TYPE_SEARCH, querySamplingInterval);
        PlatformLogger logger = new PlatformLogger(
                ApplicationProvider.getApplicationContext(),
                UserHandle.of(UserHandle.USER_NULL),
                new PlatformLogger.Config(
                        TEST_MIN_TIME_INTERVAL_BETWEEN_SAMPLES_MILLIS,
                        TEST_DEFAULT_SAMPLING_INTERVAL,
                        samplingIntervals));

        // The default sampling interval should be used if no sampling interval is
        // provided for certain call type.
        assertThat(logger.createExtraStatsLocked(TEST_PACKAGE_NAME,
                CallStats.CALL_TYPE_INITIALIZE).mSamplingInterval).isEqualTo(
                TEST_DEFAULT_SAMPLING_INTERVAL);
        assertThat(logger.createExtraStatsLocked(TEST_PACKAGE_NAME,
                CallStats.CALL_TYPE_FLUSH).mSamplingInterval).isEqualTo(
                TEST_DEFAULT_SAMPLING_INTERVAL);

        // The configured sampling interval is used if sampling interval is available
        // for certain call type.
        assertThat(logger.createExtraStatsLocked(TEST_PACKAGE_NAME,
                CallStats.CALL_TYPE_PUT_DOCUMENT).mSamplingInterval).isEqualTo(
                putDocumentSamplingInterval);
        assertThat(logger.createExtraStatsLocked(TEST_PACKAGE_NAME,
                CallStats.CALL_TYPE_SEARCH).mSamplingInterval).isEqualTo(
                querySamplingInterval);
    }

    @Test
    public void testCalculateHashCode_MD5_int32_shortString()
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final String str1 = "d1";
        final String str2 = "d2";

        int hashCodeForStr1 = PlatformLogger.calculateHashCodeMd5(str1);

        // hashing should be stable
        assertThat(hashCodeForStr1).isEqualTo(
                PlatformLogger.calculateHashCodeMd5(str1));
        assertThat(hashCodeForStr1).isNotEqualTo(
                PlatformLogger.calculateHashCodeMd5(str2));
    }

    @Test
    public void testGetCalculateCode_MD5_int32_mediumString()
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final String str1 = "Siblings";
        final String str2 = "Teheran";

        int hashCodeForStr1 = PlatformLogger.calculateHashCodeMd5(str1);

        // hashing should be stable
        assertThat(hashCodeForStr1).isEqualTo(
                PlatformLogger.calculateHashCodeMd5(str1));
        assertThat(hashCodeForStr1).isNotEqualTo(
                PlatformLogger.calculateHashCodeMd5(str2));
    }

    @Test
    public void testCalculateHashCode_MD5_int32_longString() throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        final String str1 = "abcdefghijkl-mnopqrstuvwxyz";
        final String str2 = "abcdefghijkl-mnopqrstuvwxy123";

        int hashCodeForStr1 = PlatformLogger.calculateHashCodeMd5(str1);

        // hashing should be stable
        assertThat(hashCodeForStr1).isEqualTo(
                PlatformLogger.calculateHashCodeMd5(str1));
        assertThat(hashCodeForStr1).isNotEqualTo(
                PlatformLogger.calculateHashCodeMd5(str2));
    }

    @Test
    public void testCalculateHashCode_MD5_int32_sameAsBigInteger_intValue() throws
            NoSuchAlgorithmException, UnsupportedEncodingException {
        final String emptyStr = "";
        final String shortStr = "a";
        final String mediumStr = "Teheran";
        final String longStr = "abcd-efgh-ijkl-mnop-qrst-uvwx-yz";

        int emptyHashCode = PlatformLogger.calculateHashCodeMd5(emptyStr);
        int shortHashCode = PlatformLogger.calculateHashCodeMd5(shortStr);
        int mediumHashCode = PlatformLogger.calculateHashCodeMd5(mediumStr);
        int longHashCode = PlatformLogger.calculateHashCodeMd5(longStr);

        assertThat(emptyHashCode).isEqualTo(calculateHashCodeMd5withBigInteger(emptyStr));
        assertThat(shortHashCode).isEqualTo(calculateHashCodeMd5withBigInteger(shortStr));
        assertThat(mediumHashCode).isEqualTo(calculateHashCodeMd5withBigInteger(mediumStr));
        assertThat(longHashCode).isEqualTo(calculateHashCodeMd5withBigInteger(longStr));
    }

    @Test
    public void testCalculateHashCode_MD5_strIsNull() throws
            NoSuchAlgorithmException, UnsupportedEncodingException {
        assertThat(PlatformLogger.calculateHashCodeMd5(/*str=*/ null)).isEqualTo(-1);
    }

    @Test
    public void testShouldLogForTypeLocked_trueWhenSampleIntervalIsOne() {
        final int samplingInterval = 1;
        final String testPackageName = "packageName";
        PlatformLogger logger = new PlatformLogger(
                ApplicationProvider.getApplicationContext(),
                UserHandle.of(UserHandle.USER_NULL),
                new PlatformLogger.Config(
                        TEST_MIN_TIME_INTERVAL_BETWEEN_SAMPLES_MILLIS,
                        samplingInterval,
                        /*samplingIntervals=*/ new SparseIntArray()));

        // Sample should always be logged for the first time if sampling is disabled(value is one).
        assertThat(logger.shouldLogForTypeLocked(CallStats.CALL_TYPE_PUT_DOCUMENT)).isTrue();
        assertThat(logger.createExtraStatsLocked(testPackageName,
                CallStats.CALL_TYPE_PUT_DOCUMENT).mSkippedSampleCount).isEqualTo(0);
    }

    @Test
    public void testShouldLogForTypeLocked_falseWhenSampleIntervalIsNegative() {
        final int samplingInterval = -1;
        final String testPackageName = "packageName";
        PlatformLogger logger = new PlatformLogger(
                ApplicationProvider.getApplicationContext(),
                UserHandle.of(UserHandle.USER_NULL),
                new PlatformLogger.Config(
                        TEST_MIN_TIME_INTERVAL_BETWEEN_SAMPLES_MILLIS,
                        samplingInterval,
                        /*samplingIntervals=*/ new SparseIntArray()));

        // Makes sure sample will be excluded due to sampling if sample interval is negative.
        assertThat(logger.shouldLogForTypeLocked(CallStats.CALL_TYPE_PUT_DOCUMENT)).isFalse();
        // Skipped count should be 0 since it doesn't pass the sampling.
        assertThat(logger.createExtraStatsLocked(testPackageName,
                CallStats.CALL_TYPE_PUT_DOCUMENT).mSkippedSampleCount).isEqualTo(0);
    }

    @Test
    public void testShouldLogForTypeLocked_falseWhenWithinCoolOffInterval() {
        // Next sample won't be excluded due to sampling.
        final int samplingInterval = 1;
        // Next sample would guaranteed to be too close.
        final int minTimeIntervalBetweenSamplesMillis = Integer.MAX_VALUE;
        final String testPackageName = "packageName";
        PlatformLogger logger = new PlatformLogger(
                ApplicationProvider.getApplicationContext(),
                UserHandle.of(UserHandle.USER_NULL),
                new PlatformLogger.Config(
                        minTimeIntervalBetweenSamplesMillis,
                        samplingInterval,
                        /*samplingIntervals=*/ new SparseIntArray()));
        logger.setLastPushTimeMillisLocked(SystemClock.elapsedRealtime());

        // Makes sure sample will be excluded due to rate limiting if samples are too close.
        assertThat(logger.shouldLogForTypeLocked(CallStats.CALL_TYPE_PUT_DOCUMENT)).isFalse();
        assertThat(logger.createExtraStatsLocked(testPackageName,
                CallStats.CALL_TYPE_PUT_DOCUMENT).mSkippedSampleCount).isEqualTo(1);
    }

    @Test
    public void testShouldLogForTypeLocked_trueWhenOutsideOfCoolOffInterval() {
        // Next sample won't be excluded due to sampling.
        final int samplingInterval = 1;
        // Next sample would guaranteed to be included.
        final int minTimeIntervalBetweenSamplesMillis = 0;
        final String testPackageName = "packageName";
        PlatformLogger logger = new PlatformLogger(
                ApplicationProvider.getApplicationContext(),
                UserHandle.of(UserHandle.USER_NULL),
                new PlatformLogger.Config(
                        minTimeIntervalBetweenSamplesMillis,
                        samplingInterval,
                        /*samplingIntervals=*/ new SparseIntArray()));
        logger.setLastPushTimeMillisLocked(SystemClock.elapsedRealtime());

        // Makes sure sample will be logged if it is not too close to previous sample.
        assertThat(logger.shouldLogForTypeLocked(CallStats.CALL_TYPE_PUT_DOCUMENT)).isTrue();
        assertThat(logger.createExtraStatsLocked(testPackageName,
                CallStats.CALL_TYPE_PUT_DOCUMENT).mSkippedSampleCount).isEqualTo(0);
    }

    /** Makes sure the caching works while getting the UID for calling package. */
    @Test
    public void testGetPackageUidAsUser() throws Exception {
        final String testPackageName = "packageName";
        final int testUid = 1234;
        PlatformLogger logger = new PlatformLogger(
                mContext,
                mContext.getUser(),
                new PlatformLogger.Config(
                        TEST_MIN_TIME_INTERVAL_BETWEEN_SAMPLES_MILLIS,
                        TEST_DEFAULT_SAMPLING_INTERVAL,
                        /*samplingIntervals=*/ new SparseIntArray()));
        PackageManager mockPackageManager = getMockPackageManager(mContext.getUser());
        when(mockPackageManager.getPackageUid(testPackageName, /*flags=*/0)).thenReturn(testUid);

        // First time, no cache
        PlatformLogger.ExtraStats extraStats = logger.createExtraStatsLocked(testPackageName,
                CallStats.CALL_TYPE_PUT_DOCUMENT);
        verify(mockPackageManager, times(1))
                .getPackageUid(eq(testPackageName), /*flags=*/ anyInt());
        assertThat(extraStats.mPackageUid).isEqualTo(testUid);

        // Second time, we have cache
        extraStats = logger.createExtraStatsLocked(testPackageName,
                CallStats.CALL_TYPE_PUT_DOCUMENT);

        // Count is still one since we will use the cache
        verify(mockPackageManager, times(1))
                .getPackageUid(eq(testPackageName), /*flags=*/ anyInt());
        assertThat(extraStats.mPackageUid).isEqualTo(testUid);

        // Remove the cache and try again
        assertThat(logger.removeCachedUidForPackage(testPackageName)).isEqualTo(testUid);
        extraStats = logger.createExtraStatsLocked(testPackageName,
                CallStats.CALL_TYPE_PUT_DOCUMENT);

        // count increased by 1 since cache is cleared
        verify(mockPackageManager, times(2))
                .getPackageUid(eq(testPackageName), /*flags=*/ anyInt());
        assertThat(extraStats.mPackageUid).isEqualTo(testUid);
    }

    private static int calculateHashCodeMd5withBigInteger(@NonNull String str)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(str.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();
        return new BigInteger(digest).intValue();
    }

    @NonNull
    private PackageManager getMockPackageManager(@NonNull UserHandle user) {
        PackageManager pm = mMockPackageManagers.get(user);
        if (pm == null) {
            pm = Mockito.mock(PackageManager.class);
            mMockPackageManagers.put(user, pm);
        }
        return pm;
    }
}
