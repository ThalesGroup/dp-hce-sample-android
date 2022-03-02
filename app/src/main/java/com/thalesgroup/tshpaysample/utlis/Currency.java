package com.thalesgroup.tshpaysample.utlis;

import java.text.DecimalFormat;

/**
 * Class representing a currency.
 *
 * @author PB
 */
public class Currency {
    /**
     * Position of currency before amount.
     */
    public static final int BEFORE = 0;
    /**
     * Position of currency symbol after amount.
     */
    public static final int AFTER = 1;

    /**
     * The numeric currency code according to ISO 4217.
     * 840 for USD
     */
    private final int mCurrencyNumber;

    /**
     * The numeric currency code according to ISO 4217.
     * ex: 'USD'
     */
    private final String mCurrencyCode;

    /**
     * The currency symbol to be displayed on the UI.
     */
    private final String mCurrencySymbol;
    /**
     * The conversion rate to cents of euros.
     */
    private final int mConversionRate;
    /**
     * Currency symbol display position (before:0 or after:1).
     */
    private final int mPosition;
    /**
     * The amount value will be multiplied by this coefficient before displaying it.
     */
    private int mDisplayMultiplier;
    /**
     * Decimal separator position from left (0=no decimal places, 1=one decimal place, ...).
     * <p/>
     * Also used for parsing from the string where the decimal separator position is implicit.
     */
    private int mDecimalSeparatorPosition;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Building object
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructor.
     *
     * @param currCodeNum
     *         currency code
     *
     * @param currencySymbol
     *         currency symbol
     * @param conversionRate
     *         conversion rate to cents of euros
     */
    public Currency(final int currCodeNum,
                    final String currencyCode,
                    final String currencySymbol,
                    final int conversionRate,
                    final int position) {
        mCurrencyNumber = currCodeNum;
        mCurrencyCode = currencyCode;
        mCurrencySymbol = currencySymbol;
        mConversionRate = conversionRate;
        mPosition = position;
        mDisplayMultiplier = 1;
        mDecimalSeparatorPosition = 2;
    }

    /**
     * Sets the display multiplier value (defaults to {@code 1}).
     *
     * @param multiplier
     *         The display multiplier to set.
     * @return Self for call chaining.
     */
    public Currency setDisplayMultiplier(final int multiplier) {
        mDisplayMultiplier = multiplier;
        return this;
    }

    /**
     * Sets the decimal separator position (defaults to {@code 2}).
     *
     * @param decimalSeparatorPosition
     *         The position of the decimal separator, left aligned (0=no decimal places, 1=one
     *         decimal place, ...).
     * @return Self for call chaining.
     */
    public Currency setDecimalSeparatorPosition(final int decimalSeparatorPosition) {
        mDecimalSeparatorPosition = decimalSeparatorPosition;
        return this;
    }

    /**
     * Formats the amount to the currency specific formatting and adds symbol to it.
     *
     * @param trxAmount Must be two exponent formatted value! E.g. 12.23
     * @return Formatted string for given currency with symbol and correct exponent formatting.
     */
    public String getFormatAmountDisplay(final double trxAmount) {
        String output;
        DecimalFormat decimalFormat;

        switch (mDecimalSeparatorPosition){
            case 0:
                decimalFormat = new DecimalFormat("0");
                break;
            case 1:
                decimalFormat = new DecimalFormat("0.0");
                break;
            case 2:
                decimalFormat = new DecimalFormat("0.00");
                break;
            case 3:
                decimalFormat = new DecimalFormat("0.000");
                break;
            default:
                decimalFormat = new DecimalFormat();
                break;
        }

        if (mPosition == Currency.BEFORE) {
            output = mCurrencySymbol + " " + decimalFormat.format(trxAmount);
        } else {
            output = decimalFormat.format(trxAmount) + " " + mCurrencySymbol;
        }
        return output;
    }

    public String getFormatAmountCodeDisplay(final double trxAmount) {
        String output;
        DecimalFormat decimalFormat;

        switch (mDecimalSeparatorPosition){
            case 0:
                decimalFormat = new DecimalFormat("0");
                break;
            case 1:
                decimalFormat = new DecimalFormat("0.0");
                break;
            case 2:
                decimalFormat = new DecimalFormat("0.00");
                break;
            case 3:
                decimalFormat = new DecimalFormat("0.000");
                break;
            default:
                decimalFormat = new DecimalFormat();
                break;
        }

        if (mPosition == Currency.BEFORE) {
            output = mCurrencyCode + " " + decimalFormat.format(trxAmount);
        } else {
            output = decimalFormat.format(trxAmount) + " " + mCurrencyCode;
        }
        return output;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getting properties
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the display multiplier. The amount value shall be multiplied with this coefficient
     * before displaying it.
     *
     * @return The display multiplier. In most of the cases, it is {@code 1}.
     */
    public int getDisplayMultiplier() {
        return mDisplayMultiplier;
    }

    /**
     * Retrieves the decimal separator position for parsing the currency value.
     *
     * @return The position of the decimal separator, left aligned (0=no decimal places, 1=one
     * decimal place, ...).
     */
    public int getDecimalSeparatorPosition() {
        return mDecimalSeparatorPosition;
    }

    /**
     * Retrieves the numeric currency code according to the ISO 4217.
     *
     * @return The currency code number.
     */
    public int getCode() {
        return mCurrencyNumber;
    }

    /**
     * Retrieves the Alphabetical currency code according to the ISO 4217.
     *
     * @return The currency code.
     */
    public String getCurrencyCode() {
        return mCurrencyCode;
    }

    /**
     * Retrieves the conversion rate.
     *
     * @return The conversion rate.
     */
    public int getConversionRate() {
        return mConversionRate;
    }

    /**
     * Retrieves the currency symbol position.
     *
     * @return {@link #BEFORE} or {@link #AFTER}.
     */
    public int getPosition() {
        return mPosition;
    }

    /**
     * Retrieves the currency symbol for the display.
     *
     * @return The currency symbol.
     */
    public String getCurrencySymbol() {
        return mCurrencySymbol;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Display utilities
    ////////////////////////////////////////////////////////////////////////////////////////////////

    static public String formatAmountDisplay(final int currencyCode, final String amount, final int iType) {
        String display;
        final int index = UtilsCurrenciesConstants.getTableIndex(currencyCode);
        if (index != -1) {
            final String currencySymbol = UtilsCurrenciesConstants.CURRENCY_TABLE[index].mCurrencySymbol;
            if (iType == Currency.BEFORE) {
                display = currencySymbol + amount;
            } else {
                display = amount + currencySymbol;
            }
        } else {
            display = amount;
        }

        return display;
    }

    @Override
    public String toString() {
        return "Currency{" +
                "mCurrencyNumber=" + mCurrencyNumber +
                ", mCurrencyCode='" + mCurrencyCode + '\'' +
                ", mCurrencySymbol='" + mCurrencySymbol + '\'' +
                ", mConversionRate=" + mConversionRate +
                ", mPosition=" + mPosition +
                ", mDisplayMultiplier=" + mDisplayMultiplier +
                ", mDecimalSeparatorPosition=" + mDecimalSeparatorPosition +
                '}';
    }
}
