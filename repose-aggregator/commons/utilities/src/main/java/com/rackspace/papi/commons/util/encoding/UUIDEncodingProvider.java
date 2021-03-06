package com.rackspace.papi.commons.util.encoding;

import java.util.UUID;

public final class UUIDEncodingProvider implements EncodingProvider {
    private static final int QWORD_BYTE_LENGTH = 8, BYTE_BIT_LENGTH = 8;

    private static final int MASK = 0xFF;
    private static final int UUID_BUFFER_SIZE = 8;
    private static final int UUID_BYTE_SIZE = 16;

    private static final UUIDEncodingProvider INSTANCE = new UUIDEncodingProvider();

    public static EncodingProvider getInstance() {
        return INSTANCE;
    }

    private UUIDEncodingProvider() {
    }


    @Override
    public byte[] decode(String hash) {
        final UUID uuid = UUID.fromString(hash);

        final byte[] buffer = new byte[UUID_BYTE_SIZE];

        System.arraycopy(longToQword(uuid.getMostSignificantBits()), 0, buffer, 0, BYTE_BIT_LENGTH);
        System.arraycopy(longToQword(uuid.getLeastSignificantBits()), 0, buffer, BYTE_BIT_LENGTH, BYTE_BIT_LENGTH);

        return buffer;
    }

    @Override
    public String encode(byte[] hash) {
        final byte[] buffer = new byte[UUID_BUFFER_SIZE];

        System.arraycopy(hash, 0, buffer, 0, BYTE_BIT_LENGTH);

        final long msb = qwordToLong(buffer);

        System.arraycopy(hash, BYTE_BIT_LENGTH, buffer, 0, BYTE_BIT_LENGTH);

        final long lsb = qwordToLong(buffer);

        return new UUID(msb, lsb).toString();
    }

    private static byte[] longToQword(long l) {
        final byte[] qWord = new byte[QWORD_BYTE_LENGTH];

        for (int p = 0, shift = 0; p < QWORD_BYTE_LENGTH; p++, shift += BYTE_BIT_LENGTH) {
            qWord[p] = (byte) ((l >> shift) & MASK);
        }

        return qWord;
    }

    private static long qwordToLong(byte[] qWord) {
        long l = 0;

        for (int p = 0, shift = 0; p < QWORD_BYTE_LENGTH; p++, shift += BYTE_BIT_LENGTH) {
            l += (long) (qWord[p] & MASK) << shift;
        }

        return l;
    }

    public static UUID bytesToUUID(byte[] uuidBytes) {
        final byte[] buffer = new byte[UUID_BUFFER_SIZE];

        System.arraycopy(uuidBytes, 0, buffer, 0, BYTE_BIT_LENGTH);

        final long msb = qwordToLong(buffer);

        System.arraycopy(uuidBytes, BYTE_BIT_LENGTH, buffer, 0, BYTE_BIT_LENGTH);

        final long lsb = qwordToLong(buffer);

        return new UUID(msb, lsb);
    }

}
