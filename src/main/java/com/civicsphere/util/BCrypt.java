package com.civicsphere.util;

import java.security.SecureRandom;

/**
 * BCrypt password hashing implementation.
 * Adapted from Damien Miller's OpenBSD-compatible BCrypt implementation.
 * Licensed under the BSD / ISC license.
 */
public class BCrypt {

    private static final int BCRYPT_SALT_LEN = 16;
    private static final int BLOWFISH_NUM_ROUNDS = 16;

    private static final int[] P_orig = {
        0x243f6a88, 0x85a308d3, 0x13198a2e, 0x03707344, 0xa4093822,
        0x299f31d0, 0x082efa98, 0xec4e6c89, 0x452821e6, 0x38d01377,
        0xbe5466cf, 0x34e90c6c, 0xc0ac29b7, 0xc97c50dd, 0x3f84d5b5,
        0xb5470917, 0x9216d5d9, 0x8979fb1b
    };

    private static final int[] S_orig = {
        0xd1310ba6, 0x98dfb5ac, 0x2ffd72db, 0xd01adfb7, 0xb8e1afed,
        0x6a267e96, 0xba7c9045, 0xf12c7f99, 0x24a19947, 0xb3916cf7,
        0x0801f2e2, 0x858efc16, 0x636920d8, 0x71574e69, 0xa458fea3,
        0xf4933d7e, 0x0d95748f, 0x728eb658, 0x718bcd58, 0x82154aee,
        0x7b54a41d, 0xc25a59b5, 0x9c30d539, 0x2af26013, 0xc5d1b023,
        0x286085f0, 0xca417918, 0xb8db38ef, 0x8e79dcb0, 0x603a180e,
        0x6c9e0e8b, 0xb01e8a3e, 0xd71577c1, 0xbd314b27, 0x78af2fda,
        0x55605c60, 0xe65525f3, 0xaa55ab94, 0x57489862, 0x63e81440,
        0x55ca396a, 0x2aab10b6, 0xb4cc5c34, 0x1141e8ce, 0xa15486af,
        0x7c72e993, 0xb3ee1411, 0x636fbc2a, 0x2ba9c55d, 0x741831f6,
        0xce5c3e16, 0x9b87931e, 0xafd6ba33, 0x6c24cf5c, 0x7a325381,
        0x28958677, 0x3b8f4898, 0x6b4bb9af, 0xc4bfe81b, 0x66282193,
        0x61d809cc, 0xfb21a991, 0x487cac60, 0x5dec8032, 0xef845d5d,
        0xe98575b1, 0xdc262302, 0xeb651b88, 0x23893e81, 0xd396acc5,
        0x0f6d6ff3, 0x83f44239, 0x2e0b4482, 0xa4842004, 0x69c8f04a,
        0x9e1f9b5e, 0x21c66842, 0xf6e96c9a, 0x670c9c61, 0xabd388f0,
        0x6a51a0d2, 0xd8542f68, 0x960fa728, 0xab5133a3, 0x6eef0b6c,
        0x137a3be4, 0xba3bf050, 0x7efb2bbe, 0x9524f114, 0x3f72cdff,
        0x45a96028, 0x41f92c10, 0x27f543e4, 0x12c44822, 0x153288a8,
        0xefb09f7a, 0x1f5d6061, 0x0fc44fa4, 0xfe301c27, 0xd6b7556a,
        0xbf41846b, 0x79975b96, 0x3344b58c, 0x9629b3ae, 0x5a18a5fd,
        0x77fa231e, 0x247a3e20, 0x475fdf97, 0x02891392, 0xcd693247,
        0x0e28f110, 0x897914e6, 0x8bfb40a5, 0x746d84f2, 0x3099955e,
        0x76b6e492, 0x6e097d64, 0xd587a8f8, 0x58784d1a, 0xd984f884,
        0x0e7c5417, 0xb5d878cb, 0x36ce850f, 0xee769931, 0x1d467dfa,
        0xd1849a0b, 0x5b364497, 0x44b2074c, 0x5ca7fa71, 0x42ffae29,
        0xbf302e1c, 0x05f88921, 0x8035ee82, 0xf6269986, 0x1d75d40a,
        0x20551061, 0x51c36087, 0x9541a37c, 0x789b5247, 0xa2c62c3f,
        0xf70e28a6, 0x10156d95, 0x599da056, 0xa33ecfa0, 0x78912d02,
        0x37a9359e, 0x7896ffeb, 0x38680326, 0xdf84d4fb, 0x69680326,
        0x93309e5c, 0x07f43db4, 0x58249fc3, 0x0854497a, 0x3bf92f7c,
        0x2c6f1402, 0x05856b3b, 0xd46816cb, 0xb48243be, 0x9ff5f2cb,
        0x9dfdc626, 0xc1ff3d93, 0xbe239a58, 0xe22e20ff, 0x06117d7b,
        0x356345ec, 0x1a87c538, 0x56a64096, 0x66f64e13, 0xc8701048,
        0x09dd8727, 0x4b7b223d, 0x83959b8a, 0x633e7d5d, 0x90432326,
        0x973a3a4e, 0x153173d0, 0x98a1cc0a, 0xa4c68be1, 0x2287c8a6,
        0x9698c0b2, 0x2f9547d7, 0xa37418a0, 0xd986bfb0, 0xd2c0d80c,
        0xa7f54714, 0x181057ac, 0x217d8487, 0x3d70b793, 0xdf2d5d8e,
        0x45f65f7c, 0xdfd3a77d, 0xd06c98ea, 0x9d4948a2, 0x868b082e,
        0x53109182, 0x7a69fe90, 0x47d29241, 0xd470a1a4, 0xd14e9f3b,
        0x3882772e, 0x4864d4ec, 0x68ec052e, 0x66dd8b9f, 0x81e979dc,
        0x4808c5c7, 0x4e69976e, 0xb28383a3, 0x7475f49e, 0x5eef6b1e,
        0x6a21a7b2, 0xa2c39e22, 0x43b2f50c, 0x05637213, 0x8543f052,
        0x72036cb5, 0x3a4f6c70, 0x67bcb306, 0x3c473052, 0xb2663660,
        0x4534014e, 0xcafda4ae, 0x49910d54, 0xbb8174ec, 0xb9e9e6bb,
        0x1929454b, 0x0c83b0cb, 0xa5fc36d0, 0xbe370da3, 0x7230c436,
        0x562a20e3, 0x9a6ec55f, 0xf028d4d9, 0x61545636, 0x7f102008,
        0x30970733, 0x537b341f, 0x5d024b1f, 0x503f74b7, 0xb82f12ac,
        0x1e030f68, 0x48350bc3, 0x757d7784, 0xbe348041, 0x550e0a7e,
        0xa147b521, 0x7a5f6074, 0xd786cf93, 0xce341561, 0x3e22e654,
        0x7879416b, 0xb6233b52, 0x680b1dfb, 0xca0695f3, 0x3566cf9b
    };

    private static final char[] base64_code = {
        '.', '/', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
        'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
        'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9'
    };

    private static final byte[] index_64 = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  0,  1,
        54, 55, 56, 57, 58, 59, 60, 61, 62, 63, -1, -1, -1, -1, -1, -1,
        -1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16,
        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, -1, -1, -1, -1, -1,
        -1, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
        43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, -1, -1, -1, -1, -1
    };

    private static final int[] bf_crypt_ciphertext = {
        0x4f727068, 0x65616e42, 0x65686f6c,
        0x64657253, 0x63727970, 0x7442696e
    };

    private int[] P;
    private int[] S;

    private static void encipher(int[] lr, int off, int[] P, int[] S) {
        int l = lr[off];
        int r = lr[off + 1];

        l ^= P[0];
        for (int i = 0; i <= 14; i += 2) {
            r ^= ((S[(l >>> 24)] + S[0x100 | ((l >>> 16) & 0xff)]) ^ S[0x200 | ((l >>> 8) & 0xff)]) + S[0x300 | (l & 0xff)] ^ P[i + 1];
            l ^= ((S[(r >>> 24)] + S[0x100 | ((r >>> 16) & 0xff)]) ^ S[0x200 | ((r >>> 8) & 0xff)]) + S[0x300 | (r & 0xff)] ^ P[i + 2];
        }
        r ^= ((S[(l >>> 24)] + S[0x100 | ((l >>> 16) & 0xff)]) ^ S[0x200 | ((l >>> 8) & 0xff)]) + S[0x300 | (l & 0xff)] ^ P[16];
        l ^= P[17];

        lr[off] = r;
        lr[off + 1] = l;
    }

    private static int streamtoword(byte[] data, int[] offp) {
        int word = 0;
        int off = offp[0];
        for (int i = 0; i < 4; i++) {
            word = (word << 8) | (data[off] & 0xff);
            off = (off + 1) % data.length;
        }
        offp[0] = off;
        return word;
    }

    private void init_key() {
        P = (int[]) P_orig.clone();
        S = (int[]) S_orig.clone();
    }

    private void ekskey(byte[] data, byte[] key) {
        int off = 0;
        int[] lr = new int[2];

        int[] offp = { 0 };
        for (int i = 0; i < P.length; i++) {
            P[i] = P[i] ^ streamtoword(key, offp);
        }

        offp[0] = 0;
        for (int i = 0; i < P.length; i += 2) {
            lr[0] ^= streamtoword(data, offp);
            lr[1] ^= streamtoword(data, offp);
            encipher(lr, 0, P, S);
            P[i] = lr[0];
            P[i + 1] = lr[1];
        }

        for (int i = 0; i < S.length; i += 2) {
            lr[0] ^= streamtoword(data, offp);
            lr[1] ^= streamtoword(data, offp);
            encipher(lr, 0, P, S);
            S[i] = lr[0];
            S[i + 1] = lr[1];
        }
    }

    private byte[] crypt_raw(byte[] password, byte[] salt, int log_rounds) {
        int rounds = 1 << log_rounds;
        init_key();
        ekskey(salt, password);

        for (int i = 0; i < rounds; i++) {
            byte[] zero = new byte[salt.length];
            ekskey(zero, password);
            ekskey(zero, salt);
        }

        int[] cdata = (int[]) bf_crypt_ciphertext.clone();
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < (cdata.length >> 1); j++) {
                encipher(cdata, j << 1, P, S);
            }
        }

        byte[] ret = new byte[cdata.length * 4];
        for (int i = 0, j = 0; i < cdata.length; i++) {
            ret[j++] = (byte) ((cdata[i] >>> 24) & 0xff);
            ret[j++] = (byte) ((cdata[i] >>> 16) & 0xff);
            ret[j++] = (byte) ((cdata[i] >>> 8) & 0xff);
            ret[j++] = (byte) (cdata[i] & 0xff);
        }
        return ret;
    }

    private static String encode_base64(byte[] d, int len) {
        int off = 0;
        StringBuilder rs = new StringBuilder();
        int c1, c2;

        while (off < len) {
            c1 = d[off++] & 0xff;
            rs.append(base64_code[(c1 >> 2) & 0x3f]);
            c1 = (c1 & 0x03) << 4;
            if (off >= len) {
                rs.append(base64_code[c1 & 0x3f]);
                break;
            }
            c2 = d[off++] & 0xff;
            c1 |= (c2 >> 4) & 0x0f;
            rs.append(base64_code[c1 & 0x3f]);
            c1 = (c2 & 0x0f) << 2;
            if (off >= len) {
                rs.append(base64_code[c1 & 0x3f]);
                break;
            }
            c2 = d[off++] & 0xff;
            c1 |= (c2 >> 6) & 0x03;
            rs.append(base64_code[c1 & 0x3f]);
            rs.append(base64_code[c2 & 0x3f]);
        }
        return rs.toString();
    }

    private static byte[] decode_base64(String s, int maxolen) {
        StringBuilder rs = new StringBuilder();
        int off = 0, slen = s.length(), olen = 0;
        byte[] ret = new byte[maxolen];
        byte c1, c2, c3, c4, o;

        while (off < slen - 1 && olen < maxolen) {
            c1 = (byte) (index_64[s.charAt(off++)] & 0xff);
            c2 = (byte) (index_64[s.charAt(off++)] & 0xff);
            o = (byte) ((c1 << 2) | ((c2 & 0x30) >> 4));
            ret[olen++] = o;
            if (off >= slen || olen >= maxolen) break;
            c3 = (byte) (index_64[s.charAt(off++)] & 0xff);
            o = (byte) (((c2 & 0x0f) << 4) | ((c3 & 0x3c) >> 2));
            ret[olen++] = o;
            if (off >= slen || olen >= maxolen) break;
            c4 = (byte) (index_64[s.charAt(off++)] & 0xff);
            o = (byte) (((c3 & 0x03) << 6) | c4);
            ret[olen++] = o;
        }
        return ret;
    }

    public static String gensalt(int log_rounds) {
        SecureRandom random = new SecureRandom();
        byte[] rnd = new byte[BCRYPT_SALT_LEN];
        random.nextBytes(rnd);

        StringBuilder rs = new StringBuilder();
        rs.append("$2a$");
        if (log_rounds < 10) rs.append("0");
        rs.append(log_rounds);
        rs.append("$");
        rs.append(encode_base64(rnd, rnd.length));
        return rs.toString();
    }

    public static String gensalt() {
        return gensalt(10);
    }

    public static String hashpw(String password, String salt) {
        BCrypt B = new BCrypt();
        int off = 0;

        if (salt == null || salt.length() < 28 || salt.charAt(0) != '$' || salt.charAt(1) != '2') {
            throw new IllegalArgumentException("Invalid salt");
        }

        int minor = salt.charAt(2);
        if (minor != 'a' && minor != 'y' && minor != 'b') {
            throw new IllegalArgumentException("Invalid salt version");
        }

        off = 4;
        int log_rounds = Integer.parseInt(salt.substring(off, off + 2));
        if (log_rounds < 4 || log_rounds > 31) {
            throw new IllegalArgumentException("Invalid log_rounds: " + log_rounds);
        }
        off += 3;

        String real_salt = salt.substring(off, off + 22);
        byte[] salt_bytes = decode_base64(real_salt, BCRYPT_SALT_LEN);

        byte[] password_bytes;
        try {
            password_bytes = (password + "\0").getBytes("UTF-8");
        } catch (Exception e) {
            password_bytes = (password + "\0").getBytes();
        }

        byte[] hashed = B.crypt_raw(password_bytes, salt_bytes, log_rounds);

        StringBuilder rs = new StringBuilder();
        rs.append("$2a$");
        if (log_rounds < 10) rs.append("0");
        rs.append(log_rounds);
        rs.append("$");
        rs.append(encode_base64(salt_bytes, salt_bytes.length));
        rs.append(encode_base64(hashed, bf_crypt_ciphertext.length * 4 - 1));
        return rs.toString();
    }

    public static boolean checkpw(String plaintext, String hashed) {
        if (hashed == null || hashed.length() < 28) return false;
        try {
            return hashpw(plaintext, hashed).equals(hashed);
        } catch (Exception e) {
            return false;
        }
    }
}

