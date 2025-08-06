package me.unknkriod.quickmark.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Утилита для компактного кодирования/декодирования сообщений Quickmark.
 * Формат: бинарные данные + Base85.
 */
public class Base85Encoder {

    // -------------------- UUID --------------------

    public static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID bytesToUuid(byte[] data, int offset) {
        ByteBuffer bb = ByteBuffer.wrap(data, offset, 16);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }

    // -------------------- Base85 --------------------
    // Реализация Ascii85 (Z85) — короче Base64 на ~25%

    private static final char[] ENCODING_TABLE = {
            '0','1','2','3','4','5','6','7','8','9',
            'A','B','C','D','E','F','G','H','I','J',
            'K','L','M','N','O','P','Q','R','S','T',
            'U','V','W','X','Y','Z','a','b','c','d',
            'e','f','g','h','i','j','k','l','m','n',
            'o','p','q','r','s','t','u','v','w','x',
            'y','z','.','-','+','=','^','!','/','*',
            '?','&','<','>','(',')','[',']','{','}',
            '@','%','$','#', ':'
    };

    private static final int[] DECODING_TABLE = new int[256];

    static {
        for (int i = 0; i < ENCODING_TABLE.length; i++) {
            DECODING_TABLE[ENCODING_TABLE[i]] = i;
        }
    }

    public static String encode(byte[] data) {
        // Паддинг до кратности 4
        int padding = (4 - (data.length % 4)) % 4;
        byte[] padded = new byte[data.length + padding];
        System.arraycopy(data, 0, padded, 0, data.length);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padded.length; i += 4) {
            long value = ((padded[i] & 0xFFL) << 24) |
                    ((padded[i + 1] & 0xFFL) << 16) |
                    ((padded[i + 2] & 0xFFL) << 8) |
                    (padded[i + 3] & 0xFFL);
            for (int j = 4; j >= 0; j--) {
                sb.append(ENCODING_TABLE[(int) (value % 85)]);
                value /= 85;
            }
        }
        return sb.toString();
    }

    public static byte[] decode(String text) {
        int length = text.length();
        if (length % 5 != 0) throw new IllegalArgumentException("Invalid Base85 length");

        int size = (length / 5) * 4;
        byte[] result = new byte[size];

        int outIndex = 0;
        for (int i = 0; i < length; i += 5) {
            long value = 0;
            for (int j = 4; j >= 0; j--) {
                value = value * 85 + DECODING_TABLE[text.charAt(i + j)];
            }

            result[outIndex++] = (byte) ((value >> 24) & 0xFF);
            result[outIndex++] = (byte) ((value >> 16) & 0xFF);
            result[outIndex++] = (byte) ((value >> 8) & 0xFF);
            result[outIndex++] = (byte) (value & 0xFF);
        }

        return result;
    }

    // -------------------- Strings --------------------

    public static byte[] stringToBytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static String bytesToString(byte[] data, int offset, int length) {
        return new String(data, offset, length, StandardCharsets.UTF_8);
    }
}
