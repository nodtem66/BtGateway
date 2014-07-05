package org.cardioart.gateway.api;

/**
 * Created by jirawat on 06/07/2014.
 */
public class MyChannel {
    public static final byte ECG_LEAD_I        = 0;
    public static final byte ECG_LEAD_II       = 1;
    public static final byte ECG_LEAD_III      = 2;
    public static final byte ECG_LEAD_AVR      = 3;
    public static final byte ECG_LEAD_AVL      = 4;
    public static final byte ECG_LEAD_AVF      = 5;
    public static final byte ECG_LEAD_V1       = 6;
    public static final byte ECG_LEAD_V2       = 7;
    public static final byte ECG_LEAD_V3       = 8;
    public static final byte ECG_LEAD_V4       = 9;
    public static final byte ECG_LEAD_V5       = 10;
    public static final byte ECG_LEAD_V6       = 11;
    public static final byte SPO2              = 12;
    public static final byte MAX               = 12;

    public static String getName(byte id) {
        switch (id) {
            case ECG_LEAD_I:
                return "ECG_LEAD_I";
            case ECG_LEAD_II:
                return "ECG_LEAD_II";
            case ECG_LEAD_III:
                return "ECG_LEAD_III";
            case ECG_LEAD_AVR:
                return "ECG_LEAD_AVR";
            case ECG_LEAD_AVL:
                return "ECG_LEAD_AVL";
            case ECG_LEAD_AVF:
                return "ECG_LEAD_AVF";
            case ECG_LEAD_V1:
                return "ECG_LEAD_V1";
            case ECG_LEAD_V2:
                return "ECG_LEAD_V2";
            case ECG_LEAD_V3:
                return "ECG_LEAD_V3";
            case ECG_LEAD_V4:
                return "ECG_LEAD_V4";
            case ECG_LEAD_V5:
                return "ECG_LEAD_V5";
            case ECG_LEAD_V6:
                return "ECG_LEAD_V6";
            case SPO2:
                return "SPO2";
            default:
                return null;
        }
    }
    public static String getName(int id) {
        return getName((byte) id);
    }
}
