package net.alea.beaconsimulator.util;

import android.util.Log;

import java.nio.ByteBuffer;
import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.List;

public class MathUtils {
    public static byte[] getBytes(long payload) {
        byte[] value = new byte[4];
        value[0] = (byte) (payload & 0x000000FF);
        value[1] = (byte) ((payload & 0x0000FF00) >> 8);
        value[2] = (byte) ((payload & 0x00FF0000) >> 16);
        value[3] = (byte) ((payload & 0xFF000000) >> 24);
        return value;
    }

    public static long getLong(byte[] payload) {
        return getLong(payload, 0);
    }

    public static long getLong(byte[] payload, int offset) {
        return (payload[offset] & 0xFF) |
                ((payload[1 + offset] & 0xFF) << 8) |
                ((payload[2 + offset] & 0xFF) << 16) |
                ((payload[3 + offset] & 0xFF) << 24);
    }

    public static byte[] getBytes(int payload) {
        byte[] value = new byte[2];
        value[0] = (byte) (payload & 0xFF);
        value[1] = (byte) ((payload & 0xFF00) >> 8);
        return value;
    }


    public static byte [] getBytes (float value) {
        return ByteBuffer.allocate(4).putFloat(value).array();
    }

    public static void copyBytes(byte[] from, int... values) {
        List<byte[]> data = new ArrayList<>();
        for (int     value : values) {
            data.add(MathUtils.getBytes(value));
        }
        int dataIndex = 0;

        for (int i = 0; i < values.length; i++) {
            try {
                if (i < values.length) {
                    from[dataIndex] = data.get(i)[0];
                    from[dataIndex + 1] = data.get(i)[1];
                    dataIndex += 2;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static int getInt(byte[] payload) {
        return getInt(payload, 0);
    }

    public static int getInt(byte[] payload, int offset) {
        return (payload[offset] & 0xFF) |
                ((payload[1 + offset] & 0xFF) << 8);
    }

    public static byte[] getBytes(boolean payload) {
        byte[] value = new byte[1];
        value[0] = Byte.parseByte(payload ? "1" : "0");
        return value;
    }


    public static int unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size - 1)) != 0) {
            unsigned = -1 * ((1 << size - 1) - (unsigned & ((1 << size - 1) - 1)));
        }
        return unsigned;
    }

}
