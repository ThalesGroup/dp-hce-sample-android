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

package com.thalesgroup.tshpaysample.utlis;

import java.math.BigDecimal;


/**
 * Class representing a list of currencies
 *
 * @author PB
 */
public class UtilsCurrenciesConstants {

//    private static final int DEFAULT_DISPLAY_MULTIPLIER = 1;
//    private static final int DEFAULT_POSITION_OF_FLOATING_POINT = 2;

    /**
     * List of currencies supported
     */
    public static final Currency[] CURRENCY_TABLE = {

            // added by snebesky - start

            // Algerian Dinar
            new Currency(12, "DZD", "\u062F\u002E\u062C", 1000, Currency.AFTER),
            // Angolian Kwanza
            new Currency(973, "AOA", "\u004b\u007a", 1000, Currency.AFTER),
            // Armenian Dram; symbol not supported - \u058f
            new Currency(51, "AMD", "\u0041\u004d\u0044", 1000, Currency.AFTER),
            // Bahrain Dinar; symbol not supported - \u002e\u062f\u002e\u0628
            new Currency(48, "BHD", "\u0042\u0044", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(3),
            // Bangladeshi Taka
            new Currency(50, "BDT", "\u09f3", 1000, Currency.AFTER),
            // Benin CFA Franc BCEAO
            new Currency(952, "XOF", "\u0043\u0046\u0041", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Bhutanese ngultrum
            new Currency(64, "BTN", "\u004e\u0075", 1000, Currency.AFTER),
            // Burundi Franc
            new Currency(108, "BIF", "\u0046\u0042\u0075", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Cameroon CFA Franc BEAC
            new Currency(950, "XAF", "\u0046\u0043\u0046\u0041", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Cape Verde Escudo
            new Currency(132, "CVE", "\u0024", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Chile Unidad de Fomento funds code
            new Currency(990, "CLF", "\u0055\u0046", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Comoro Franc
            new Currency(174, "KMF", "\u0043\u0046", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Democratic Republic of Congo Franc Congolais
            new Currency(976, "CDF", "\u0046\u0043", 1000, Currency.AFTER),
            // Cuban convertibble peso
            new Currency(931, "CUC", "\u0043\u0055\u0043", 1000, Currency.AFTER),
            // Djibouti Franc
            new Currency(262, "DJF", "\u0046\u0064\u006a", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Eritrean Nakfa
            new Currency(232, "ERN", "\u004e\u0066\u006b", 1000, Currency.AFTER),
            // Ethiopian Birr
            new Currency(230, "ETB", "\u0042\u0072", 1000, Currency.AFTER),
            // Haitian Gourde
            new Currency(332, "HTG", "\u0047", 1000, Currency.AFTER),
            // French Polynesian CFP Franc
            new Currency(953, "XPF", "\u0046", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Gambien Dalasi
            new Currency(270, "GMD", "\u0044", 1000, Currency.AFTER),
            // Georgian Lari
            new Currency(981, "GEL", "\u10da\u002e", 1000, Currency.AFTER),
            // Ghanan Cedi
            new Currency(936, "GHS", "\u0047\u0048\u20b5", 1000, Currency.AFTER),
            // Guinea Franc
            new Currency(324, "GNF", "\u0046\u0047", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Iraqi Dinar
            new Currency(368, "IQD", "\u062f\u002e\u0639", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(3),
            // Jordanian Dinar
            new Currency(400, "JOD", "\u062f\u002e\u0623", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(3),
            // Kenyan Shilling
            new Currency(404, "KES", "\u004b\u0053\u0068", 1000, Currency.AFTER),
            // Kuwaiti Dinar; symbol not supported \u062f\u002e\u0643
            new Currency(414, "KWD", "\u004b\u002e\u0042\u002e", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(3),
            // Lesotho RandLoti
            new Currency(426, "LSL", "\u004c", 1000, Currency.AFTER),
            // Libyan Dinar
            // symbol not supported \u0644\u002e\u062f
            new Currency(434, "LYD", "\u004c\u0044", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(3),
            // Macau Pataca
            new Currency(446, "MOP", "\u004d\u004f\u0050\u0024", 1000, Currency.AFTER),
            // Madasgasy Ariary
            new Currency(969, "MGA", "\u0041\u0072", 1000, Currency.AFTER),
            // Malawi Kwacha
            new Currency(454, "MWK", "\u004d\u004b", 1000, Currency.AFTER),
            // Maldives Rufiyaa
            new Currency(462, "MVR", "\u004d\u0052\u0066", 1000, Currency.AFTER),
            // Mauritanian Ouguiya
            new Currency(478, "MRO", "\u0055\u004d", 1000, Currency.AFTER),
            // Moldovan Leu
            new Currency(498, "MDL", "\u004c\u0045\u0049", 1000, Currency.AFTER),
            // Maroccan Dirham; symbol not supported \u062f\u002e\u0645\u002e
            new Currency(504, "MAD", "\u004d\u0041\u0044", 1000, Currency.AFTER),
            // Myanmarian Kyat
            new Currency(104, "MMK", "\u004b", 1000, Currency.AFTER),
            // Papua New Guinean Kina
            new Currency(598, "PGK", "\u004b", 1000, Currency.AFTER),
            // Rwanda Franc
            new Currency(646, "RWF", "\u0046\u0052\u0077", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Samoan Tala
            new Currency(882, "WST", "\u0057\u0053\u0024", 1000, Currency.AFTER),
            // Sao Tome And Principe Dobra
            new Currency(678, "STD", "\u0044\u0062", 1000, Currency.AFTER),
            // Siera Leone
            new Currency(694, "SLL", "\u004c\u0065", 1000, Currency.AFTER),
            // South Sudanese pound
            new Currency(728, "SSP", "\u00A3", 1000, Currency.AFTER),
            // Sudanese Pound
            new Currency(938, "SDG", "\u00a3\u0053\u0064", 1000, Currency.AFTER),
            // Swaziland Lilangeni
            new Currency(748, "SZL", "\u004c", 1000, Currency.AFTER),
            // Tajikistani Somoni
            new Currency(972, "TJS", "\u0441\u043c\u043d\u002e", 1000, Currency.AFTER),
            // Tanzanian Shilling
            new Currency(834, "TZS", "\u0054\u0053\u0068", 1000, Currency.AFTER),
            // Tonga Paanga
            new Currency(776, "TOP", "\u0054\u0024", 1000, Currency.AFTER),
            // Tunisian Dinar
            new Currency(788, "TND", "\u0044\u0054", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(3),
            // Turkish Lira
            // symbol not supported \u20ba
            new Currency(949, "TRY", "\u0054\u0052\u0059", 1000, Currency.AFTER),
            // Turkmenistan Manat
            new Currency(934, "TMT", "\u0054", 1000, Currency.AFTER),
            // Uganda Shilling
            new Currency(800, "UGX", "\u0055\u0073\u0068", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // UAE Dirham
            new Currency(784, "AED", "\u062f\u002e\u0625", 1000, Currency.AFTER),
            // Vanuatu Vatu
            new Currency(548, "VUV", "\u0056\u0054", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Zambian Kwacha
            new Currency(967, "ZMW", "\u005a\u004b", 1000, Currency.AFTER),

            // added by snebesky - end

            // added by jkrivane - begin

            // Uruguay Peso en Unidades Indexadas
            new Currency(940, "UYI", "\u0055\u0059\u0049", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // WIR Euro (complementary currency)
            new Currency(947, "CHE", "\u0043\u0048\u0045", 1000, Currency.AFTER),
            // WIR Franc (complementary currency)
            new Currency(948, "CHW", "\u0043\u0048\u0057", 1000, Currency.AFTER),
            // European Composite Unit (EURCO) (bond market unit)
            new Currency(955, "XBA", "\u0058\u0042\u0041", 1000, Currency.AFTER),
            // European Monetary Unit (E.M.U.-6) (bond market unit)
            new Currency(956, "XBB", "\u0058\u0042\u0042", 1000, Currency.AFTER),
            // European Unit of Account 9 (E.U.A.-9) (bond market unit)
            new Currency(957, "XBC", "\u0058\u0042\u0043", 1000, Currency.AFTER),
            // European Unit of Account 17 (E.U.A.-17) (bond market unit)
            new Currency(958, "XBD", "\u0058\u0042\u0044", 1000, Currency.AFTER),
            // Gold (one troy ounce)
            new Currency(959, "XAU", "\u0058\u0041\u0055", 1000, Currency.AFTER),
            // Special drawing rights
            new Currency(960, "XDR", "\u0058\u0044\u0052", 1000, Currency.AFTER),
            // Silver (one troy ounce)
            new Currency(961, "XAG", "\u0058\u0041\u0047", 1000, Currency.AFTER),
            // Platinum (one troy ounce)
            new Currency(962, "XPT", "\u0058\u0050\u0054", 1000, Currency.AFTER),
            // Code reserved for testing purposes
            new Currency(963, "XTS", "\u0058\u0054\u0053", 1000, Currency.AFTER),
            // Palladium (one troy ounce)
            new Currency(964, "XPD", "\u0058\u0050\u0044", 1000, Currency.AFTER),
            // ADB Unit of Account
            new Currency(965, "XUA", "\u0058\u0055\u0041", 1000, Currency.AFTER),
            // Mexican Unidad de Inversion (UDI) (funds code)
            new Currency(979, "MXV", "\u004D\u0058\u0056", 1000, Currency.AFTER),
            // Bolivian Mvdol (funds code)
            new Currency(984, "BOV", "\u0042\u004F\u0056", 1000, Currency.AFTER),
            // SUCRE
            new Currency(994, "XSU", "\u0058\u0053\u0055", 1000, Currency.AFTER),
            // United States dollar (next day) (funds code)
            new Currency(997, "USN", "\u0055\u0053\u004E", 1000, Currency.AFTER),
            // United States dollar (same day) (funds code)
            new Currency(998, "USS", "\u0055\u0053\u0053", 1000, Currency.AFTER),
            // No currency
            new Currency(999, "XXX", "\u0058\u0058\u0058", 1000, Currency.AFTER),

            // added by jkrivane - end

            // Albanian lek
            new Currency(8, "ALL", "\u004C\u0065\u006B", 1000, Currency.AFTER),
            // Afghan afghani
            new Currency(971, "AFN", "\u060B", 1000, Currency.AFTER),
            // Argentine peso
            new Currency(32, "ARS", "\u0024", 1000, Currency.AFTER),
            // Aruban florin; edit snebesky: symbol not supported \u0192
            new Currency(533, "AWG", "\u0041\u0057\u0047", 1000, Currency.AFTER),
            // Australian dollar
            new Currency(36, "AUD", "\u0024", 1000, Currency.AFTER),
            // Azerbaijani manat; edit snebesky: symbol not supported \u20bc
            new Currency(944, "AZN", "\u0041\u005a\u004e", 1000, Currency.AFTER),
            // Bahamian dollar
            new Currency(44, "BSD", "\u0024", 1000, Currency.AFTER),
            // Barbados dollar
            new Currency(52, "BBD", "\u0024", 1000, Currency.AFTER),
            // Belarusian ruble
            new Currency(974, "BYR", "\u0070\u002E", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Belize dollar
            new Currency(84, "BZD", "\u0042\u005A\u0024", 1000, Currency.AFTER),
            // Bermudian dollar
            new Currency(60, "BMD", "\u0024", 1000, Currency.AFTER),
            // Boliviano
            new Currency(68, "BOB", "\u0042\u0073", 1000, Currency.AFTER),
            // Bosnia and Herzegovina convertible mark
            new Currency(977, "BAM", "\u004B\u004D", 1000, Currency.AFTER),
            // Botswana pula
            new Currency(72, "BWP", "\u0050", 1000, Currency.AFTER),
            // Bulgarian lev
            new Currency(975, "BGN", "\u043B\u0432", 1000, Currency.AFTER),
            // Brazilian real
            new Currency(986, "BRL", "\u0052\u0024", 1000, Currency.AFTER),
            // Brunei dollar
            new Currency(96, "BND", "\u0024", 1000, Currency.AFTER),
            // Cambodian riel; edit snebesky: symbol \u17DB not supported on some phones (S4)??
            new Currency(116, "KHR", "\u004b\u0048\u0052", 1000, Currency.AFTER),
            // Canadian dollar
            new Currency(124, "CAD", "\u0024", 1000, Currency.AFTER),
            // Cayman Islands dollar
            new Currency(136, "KYD", "\u0024", 1000, Currency.AFTER),
            // Chilean peso
            new Currency(152, "CLP", "\u0024", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Renminbi|Chinese yuan
            new Currency(156, "CNY", "\u00A5", 1000, Currency.AFTER),
            // Colombian peso
            new Currency(170, "COP", "\u0024", 1000, Currency.AFTER),
            // Costa Rican colon
            new Currency(188, "CRC", "\u20A1", 1000, Currency.AFTER),
            // Croatian kuna
            new Currency(191, "HRK", "\u006B\u006E", 1000, Currency.AFTER),
            // Cuban peso
            new Currency(192, "CUP", "\u0043\u0055\u0050", 1000, Currency.AFTER),
            // Czech koruna
            new Currency(203, "CZK", "\u004B\u010D", 1000, Currency.AFTER),
            // Danish krone
            new Currency(208, "DKK", "\u006B\u0072", 1000, Currency.AFTER),
            // Dominican peso
            new Currency(214, "DOP", "\u0052\u0044\u0024", 1000, Currency.AFTER),
            // East Caribbean dollar
            new Currency(951, "XCD", "\u0024", 1000, Currency.AFTER),
            // Egyptian pound
            new Currency(818, "EGP", "\u0045\u00A3", 1000, Currency.AFTER),
            // Euro
            new Currency(978, "EUR", "\u20AC", 1000, Currency.AFTER),
            // Falkland Islands pound
            new Currency(238, "FKP", "\u00A3", 1000, Currency.AFTER),
            // Fiji dollar
            new Currency(242, "FJD", "\u0024", 1000, Currency.AFTER),
            // Gibraltar pound
            new Currency(292, "GIP", "\u00A3", 1000, Currency.AFTER),
            // Guatemalan quetzal
            new Currency(320, "GTQ", "\u0051", 1000, Currency.AFTER),
            // Guyanese dollar
            new Currency(328, "GYD", "\u0024", 1000, Currency.AFTER),
            // Honduran lempira
            new Currency(340, "HNL", "\u004C", 1000, Currency.AFTER),
            // Hong Kong dollar
            new Currency(344, "HKD", "\u0024", 1000, Currency.AFTER),
            // Hungarian forint
            new Currency(348, "HUF", "\u0046\u0074", 1000, Currency.AFTER),
            // Icelandic krona
            new Currency(352, "ISK", "\u006B\u0072", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Indian rupee
            new Currency(356, "INR", "\u20B9", 1000, Currency.AFTER),
            // Indonesian rupiah
            new Currency(360, "IDR", "\u0052\u0070", 1000, Currency.AFTER),
            // Iranian rial; edit snebesky: symbol not supported \u0364
            new Currency(364, "IRR", "\u0049\u0052\u0052", 1000, Currency.AFTER),
            // Israeli new shekel
            new Currency(376, "ILS", "\u20AA", 1000, Currency.AFTER),
            // Jamaican dollar
            new Currency(388, "JMD", "\u004A\u0024", 1000, Currency.AFTER),
            // Japanese yen
            new Currency(392, "JPY", "\u00A5", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Kazakhstani tenge
            new Currency(398, "KZT", "\u043B\u0432", 1000, Currency.AFTER),
            // North Korean won; edit: snebesky - symbol not supported \u20A9
            new Currency(408, "KPW", "\u004b\u0050\u0057", 1000, Currency.AFTER),
            // South Korean won; edit: snebesky - symbol not supported \u20A9
            new Currency(410, "KRW", "\u004b\u0052\u0057", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Kyrgyzstani som
            new Currency(417, "KGS", "\u0043", 1000, Currency.AFTER),
            // Lao kip
            new Currency(418, "LAK", "\u20AD", 1000, Currency.AFTER),
            // Latvian lats
            new Currency(428, "LVL", "\u004C\u0073", 1000, Currency.AFTER),
            // Lebanese pound
            new Currency(422, "LBP", "\u004C\u0042\u0050", 1000, Currency.AFTER),
            // Liberian dollar
            new Currency(430, "LRD", "\u0024", 1000, Currency.AFTER),
            // Lithuanian litas
            new Currency(440, "LTL", "\u004C\u0074", 1000, Currency.AFTER),
            // Macedonian denar
            new Currency(807, "MKD", "\u0434\u0435\u043D", 1000, Currency.AFTER),
            // Malaysian ringgit
            new Currency(458, "MYR", "\u0052\u004D", 1000, Currency.AFTER),
            // Mauritian rupee
            new Currency(480, "MUR", "\u20A8", 1000, Currency.AFTER),
            // Mexican peso
            new Currency(484, "MXN", "\u0024", 1000, Currency.AFTER),
            // Mongolian Mongolian tugrik
            new Currency(496, "MNT", "\u20AE", 1000, Currency.AFTER),
            // Mozambican metical
            new Currency(943, "MZN", "\u004D\u0054", 1000, Currency.AFTER),
            // Namibian dollar
            new Currency(516, "NAD", "\u0024", 1000, Currency.AFTER),
            // Nepalese rupee
            new Currency(524, "NPR", "\u20A8", 1000, Currency.AFTER),
            // Netherlands Antillean guilder
            new Currency(532, "ANG", "\u0192", 1000, Currency.AFTER),
            // New Zealand dollar
            new Currency(554, "NZD", "\u0024", 1000, Currency.AFTER),
            // Nicaraguan cordoba
            new Currency(558, "NIO", "\u0043\u0024", 1000, Currency.AFTER),
            // Nigerian naira
            new Currency(566, "NGN", "\u20A6", 1000, Currency.AFTER),
            // Norwegian krone
            new Currency(578, "NOK", "\u006B\u0072", 1000, Currency.AFTER),
            // Omani rial; symbol not supported \u0631\u002e\u0639\u002e
            new Currency(512, "OMR", "\u004f\u004d\u0052", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(3),
            // Pakistani rupee
            new Currency(586, "PKR", "\u20A8", 1000, Currency.AFTER),
            // Panamanian balboa
            new Currency(590, "PAB", "\u0042\u002F\u002E", 1000, Currency.AFTER),
            // Paraguayan guarani
            new Currency(600, "PYG", "\u20b2", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Peruvian nuevo sol
            new Currency(604, "PEI", "\u0053\u002F\u002E", 1000, Currency.AFTER),
            // Philippine; edit snebesky: symbol not supported \u20B1
            new Currency(608, "PHP", "\u0050\u0048\u0050", 1000, Currency.AFTER),
            // Polish zl oty
            new Currency(985, "PLN", "\u007A\u0142", 1000, Currency.AFTER),
            // Qatari riyal
            new Currency(634, "QAR", "\u0631\u002e\u0642", 1000, Currency.AFTER),
            // Romanian Leu|Romanian new leu
            new Currency(946, "RON", "\u006C\u0065\u0069", 1000, Currency.AFTER),
            // Russian rouble
            new Currency(643, "RUB", "\u0440\u0443\u0431", 1000, Currency.AFTER),
            // Saint Helena pound
            new Currency(654, "SHP", "\u00A3", 1000, Currency.AFTER),
            // Saudi riyal; edit snebesky: symbol not supported \uFDFC
            new Currency(682, "SAR", "\u0053\u0041\u0052", 1000, Currency.AFTER),
            // Serbian dinar
            new Currency(941, "RSD", "\u0420\u0421\u0414", 1000, Currency.AFTER),
            // Seychelles rupee
            new Currency(690, "SCR", "\u0053\u0052", 1000, Currency.AFTER),
            // Singapore dollar
            new Currency(702, "SGD", "\u0024", 1000, Currency.AFTER),
            // Solomon Islands dollar
            new Currency(90, "SBD", "\u0024", 1000, Currency.AFTER),
            // Somali shilling
            new Currency(706, "SOS", "\u0053", 1000, Currency.AFTER),
            // South African rand
            new Currency(710, "ZAR", "\u0052", 1000, Currency.AFTER),
            // Sri Lankan rupee
            new Currency(144, "LKR", "\u20A8", 1000, Currency.AFTER),
            // Swedish krona]]/kron
            new Currency(752, "SEK", "\u006B\u0072", 1000, Currency.AFTER),
            // Swiss franc
            new Currency(756, "CHF", "\u0043\u0048\u0046", 1000, Currency.AFTER),
            // Surinamese dollar
            new Currency(968, "SRD", "\u0024", 1000, Currency.AFTER),
            // Syrian pound
            new Currency(760, "SYP", "\u00A3", 1000, Currency.AFTER),
            // New Taiwan dollar
            new Currency(901, "TWD", "\u004E\u0054\u0024", 1000, Currency.AFTER),
            // Thai baht
            new Currency(764, "XPD", "\u0E3F", 1000, Currency.AFTER),
            // Trinidad and Tobago dollar
            new Currency(780, "TTD", "\u0054\u0054\u0024", 1000, Currency.AFTER),
            // Ukrainian hryvnia
            new Currency(980, "UAH", "\u20B4", 1000, Currency.AFTER),
            // Pound sterling
            new Currency(826, "GBP", "\u00A3", 1000, Currency.BEFORE),
            // United States dollar
            new Currency(840, "USD", "\u0024", 1000, Currency.BEFORE),
            // Uruguayan peso
            new Currency(858, "UYU", "\u0024\u0055", 1000, Currency.AFTER),
            // Uzbekistan som; edit snebesky: symbol not supported \u0441\u045e\u043c
            new Currency(860, "UZS", "\u0055\u005a\u0053", 1000, Currency.AFTER),
            // Venezuelan bolivar
            new Currency(937, "VEF", "\u0042\u0073\u002e", 1000, Currency.AFTER),
            // Vietnamese dong
            new Currency(704, "VND", "\u20AB", 1000, Currency.AFTER)
                    .setDecimalSeparatorPosition(0),
            // Yemeni rial; edit snebesky: symbol not supported \uFDFC
            new Currency(886, "YER", "\u0059\u0045\u0052", 1000, Currency.AFTER)
    };

    /**
     * Converts an amount from the currency 1 to the currency 2.
     *
     * @param value              amount in the currency 1 to be converted.
     * @param OriginCurrencyCode the currency o the amount to be converted.
     * @param DestCurrencyCode   the currency in which the amount must be concerted.
     * @return the amount in the currency 2.
     */
    public static final int convert(final int value, final int OriginCurrencyCode,
                                    final int DestCurrencyCode) {
        final int index1 = UtilsCurrenciesConstants.getTableIndex(OriginCurrencyCode);
        final int index2 = UtilsCurrenciesConstants.getTableIndex(DestCurrencyCode);
        int result = 0;

        if (index1 != -1 && index2 != -1) {
            result = (UtilsCurrenciesConstants.CURRENCY_TABLE[index1].getConversionRate() * value)
                    / UtilsCurrenciesConstants.CURRENCY_TABLE[index2].getConversionRate();
        }

        return result;
    }

    /**
     * Indicates if the currency code in parameter is in the currency list.
     *
     * @param currencyCode The ISO 4217 numeric currency code to locate.
     * @return true if the currency code in parameter is in the currency list
     */
    public static boolean isListed(final int currencyCode) {
        return UtilsCurrenciesConstants.getTableIndex(currencyCode) != -1;
    }

    /**
     * Locates the currency object.
     *
     * @param currencyCode The ISO 4217 numeric currency code to locate.
     * @return The currency object of the specified currency code or {@code null}.
     */
    public static Currency getCurrency(final int currencyCode) {
        final int index = UtilsCurrenciesConstants.getTableIndex(currencyCode);
        if (index < 0) {
            return null;
        }
        return UtilsCurrenciesConstants.CURRENCY_TABLE[index];
    }

    public static Currency getCurrency(final String currencyCode) {
        final int index = UtilsCurrenciesConstants.getTableIndex(currencyCode);
        if (index < 0) {
            return null;
        }
        return UtilsCurrenciesConstants.CURRENCY_TABLE[index];
    }

    /**
     * Gets the index of the list corresponding to the currencyCode number passed in parameter.
     *
     * @param currencyCode The ISO 4217 numeric currency code to locate.
     * @return the index of the list corresponding to the currency passed in parameter or -1 if it
     * has not been found.
     */
    public static int getTableIndex(final int currencyCode) {
        int retValue = 0;
        boolean found = false;

        while (!found && retValue < UtilsCurrenciesConstants.CURRENCY_TABLE.length) {
            if (UtilsCurrenciesConstants.CURRENCY_TABLE[retValue].getCode() == currencyCode) {
                found = true;
            } else {
                retValue++;
            }
        }

        if (!found) {
            retValue = -1;
        }

        return retValue;
    }

    /**
     * Gets the index of the list corresponding to the currencyCode passed in parameter.
     *
     * @param currencyCode The ISO 4217 Alphabetic currency code to locate.
     * @return the index of the list corresponding to the currency passed in parameter or -1 if it
     * has not been found.
     */
    public static int getTableIndex(final String currencyCode) {
        int retValue = 0;
        boolean found = false;

        while (!found && retValue < UtilsCurrenciesConstants.CURRENCY_TABLE.length) {
            if (UtilsCurrenciesConstants.CURRENCY_TABLE[retValue].getCurrencyCode().equalsIgnoreCase(currencyCode)) {
                found = true;
            } else {
                retValue++;
            }
        }

        if (!found) {
            retValue = -1;
        }

        return retValue;
    }

    /**
     * Retrieves the symbol for code with a specified string as a default.
     *
     * @param currencyCode The currency code to look up.
     * @param def          The default string to be used as a fallback for an unknown currency code.
     * @return The symbol corresponding to the currency code or a default string.
     */
    public static String getCurrencySymbol(final int currencyCode, final String def) {
        final int idx = UtilsCurrenciesConstants.getTableIndex(currencyCode);
        if (idx < 0 || idx >= UtilsCurrenciesConstants.CURRENCY_TABLE.length) {
            return def;
        }
        return UtilsCurrenciesConstants.CURRENCY_TABLE[idx].getCurrencySymbol().toString();
    }

    /**
     * Retrieves the symbol for code with an empty string as a default.
     *
     * @param currencyCode The currency code to look up.
     * @return The symbol corresponding to the currency code or an empty string.
     */
    public static String getCurrencySymbol(final int currencyCode) {
        return UtilsCurrenciesConstants.getCurrencySymbol(currencyCode, "");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Big decimal operations
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Parses the big decimal from the string taking the currency specifics into consideration.
     *
     * @param currencyCode The currency code to be used for parsing.
     * @param valueStr     The currency value.
     * @param defaultValue The default value to return in case of any error in the input (currency code does not
     *                     exist or invalid string).
     * @return The output value.
     */
    public static BigDecimal parseFromString(final int currencyCode, final String valueStr,
                                             final BigDecimal defaultValue) {
        // Get the currency code.
        final Currency currency = UtilsCurrenciesConstants.getCurrency(currencyCode);
        if (currency == null) {
            return defaultValue;
        }

        // Parse the string
        BigDecimal result;
        try {
            final long resultLong = Long.parseLong(valueStr);
            result = BigDecimal.valueOf(resultLong);
        } catch (final NumberFormatException e) {
            return defaultValue;
        }

        // Shift decimal point
        return result.movePointLeft(currency.getDecimalSeparatorPosition());
    }

    /**
     * Resolves currency from the currency code provided as byte[]
     *
     * @param currencyNumericCode
     * @return Currency instance associated with the given currency code or unknown currency (XXX)
     * if the lookup fails
     */
    public static Currency getCurrency(final byte[] currencyNumericCode) {
        if (currencyNumericCode == null) {
            throw new IllegalArgumentException("currencyNumericCode cannot be null");
        }

        final String strCurrencyNumericCode = CommonUtils.bytesToHex(currencyNumericCode);

        return getCurrency(Integer.parseInt(strCurrencyNumericCode));

    }


}
