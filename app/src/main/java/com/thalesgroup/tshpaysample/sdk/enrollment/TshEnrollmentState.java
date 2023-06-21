/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.enrollment;

import androidx.annotation.StringRes;

import com.thalesgroup.tshpaysample.R;

public enum TshEnrollmentState {
    INACTIVE(R.string.enrollment_state_inactive),

    // Wallet Secure Enrollment
    WSE_CHECK_START(R.string.enrollment_state_wse_started),
    WSE_CHECK_ERROR(R.string.enrollment_state_wse_failed),

    // Eligibility
    ELIGIBILITY_CHECK_START(R.string.enrollment_state_eligibility_started),
    ELIGIBILITY_CHECK_ERROR(R.string.enrollment_state_eligibility_failed),

    // Terms & Conditions
    ELIGIBILITY_TERMS_AND_CONDITIONS(R.string.enrollment_state_terms_and_conditions),

    // Digitization
    DIGITIZATION_START(R.string.enrollment_state_digitization_started),
    DIGITIZATION_ACTIVATION_CODE_AQUIRED(R.string.enrollment_state_digitization_code_aquired),
    DIGITIZATION_ACTIVATION_CODE_AQUIRED_ENROLLMENT_NEEDED(R.string.enrollment_state_digitization_code_aquired_enrollment_needed),
    DIGITIZATION_ERROR(R.string.enrollment_state_digitization_failed),
    DIGITIZATION_FINISHED(R.string.enrollment_state_digitization_finished), // This event is not always triggered before enrolling starts. Sometimes it's not triggered at all. Do not relay on it.

    // Enrolling
    ENROLLING_CODE_REQUIRED(R.string.enrollment_state_enrollment_code_required),
    ENROLLING_START(R.string.enrollment_state_enrollment_started),
    ENROLLING_ERROR(R.string.enrollment_state_enrollment_failed),
    ENROLLING_FINISHED_WAITING_FOR_SERVER(R.string.enrollment_state_enrollment_finished);

    /**
     * Return whether state represent some error.
     */
    public boolean isErrorState() {
        return this == WSE_CHECK_ERROR ||
                this == ELIGIBILITY_CHECK_ERROR ||
                this == DIGITIZATION_ERROR ||
                this == ENROLLING_ERROR;
    }

    /**
     * Return if status represent ongoing process.
     */
    public boolean isProgressState() {
        return this != TshEnrollmentState.INACTIVE && !this.isErrorState();
    }

    public @StringRes int getActionDescription() {
        return mActionDescription;
    }

    TshEnrollmentState(final @StringRes int actionDescription){
        mActionDescription = actionDescription;
    }

    private final @StringRes int mActionDescription;
}