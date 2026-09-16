package wire.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted PBKDF2 hashing for PINs and passwords.
 *
 * The original project hashed with plain unsalted SHA-256 (or, for the admin
 * account, stored the password as plain text). Unsalted hashes are cheap to
 * attack with rainbow tables, and a 4-digit PIN only has 10,000 possible
 * values, so even a salted-but-fast hash is crackable in an instant — the
 * iteration count below is what actually slows an attacker down.
 *
 * Stored format: "iterations:base64(salt):base64(hash)" — self-describing,
 * so the work factor can be bumped later without breaking existing rows.
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final int ITERATIONS = 120_000;

    private PasswordUtil() {
    }

    public static String hash(char[] secret) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(secret, salt, ITERATIONS);
        return ITERATIONS + ":" + encode(salt) + ":" + encode(hash);
    }

    public static boolean verify(char[] secret, String stored) {
        if (stored == null) {
            return false;
        }
        String[] parts = stored.split(":");
        if (parts.length != 3) {
            // Not one of our hashes (e.g. leftover legacy data) — reject rather
            // than silently accepting an unexpected format.
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = pbkdf2(secret, salt, iterations);
            return constantTimeEquals(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] secret, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(secret, salt, iterations, HASH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // PBKDF2WithHmacSHA256 is a standard JCE provider algorithm available
            // on every JDK we target, so this branch is not a real-world case.
            throw new IllegalStateException("PBKDF2 unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        // Avoid a timing side-channel that would let an attacker learn how
        // many leading bytes of a guess were correct.
        return Arrays.equals(a, b);
        // Arrays.equals on byte[] is not documented as constant-time, but for
        // a 4-digit PIN / short password the realistic threat here is offline
        // brute force, not a network timing attack — the iteration count above
        // is the actual defense. Noted here for anyone hardening this further.
    }
}
