package security;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

// Secure data class with input validation
class SecureData {
    private final String data;
    private final byte[] hash;

    public SecureData(String data) throws NoSuchAlgorithmException {
        validateInput(data);
        this.data = data;
        this.hash = calculateHash(data);
    }

    private void validateInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        // Prevent special characters that might be used in injection attacks
        if (!input.matches("^[a-zA-Z0-9\\s.-]+$")) {
            throw new IllegalArgumentException("Data contains invalid characters");
        }
    }

    private byte[] calculateHash(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    public boolean verifyIntegrity() throws NoSuchAlgorithmException {
        byte[] currentHash = calculateHash(data);
        return Arrays.equals(hash, currentHash);
    }

    public String getData() {
        return data;
    }
}

// Encryption utility class
class EncryptionUtil {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 256;

    private final SecretKey key;
    private final IvParameterSpec iv;

    public EncryptionUtil() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE);
        this.key = keyGen.generateKey();
        this.iv = generateIV();
    }

    private IvParameterSpec generateIV() {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    public String encrypt(String plaintext) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decrypt(String ciphertext) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(decryptedBytes);
    }
}

// Digital signature utility
class SignatureUtil {
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public SignatureUtil() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        this.privateKey = pair.getPrivate();
        this.publicKey = pair.getPublic();
    }

    public byte[] sign(String data) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes());
        return signature.sign();
    }

    public boolean verify(String data, byte[] signatureBytes) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data.getBytes());
        return signature.verify(signatureBytes);
    }
}

// Secure password handling
class PasswordUtil {
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        return salt;
    }

    public static byte[] hashPassword(String password, byte[] salt) throws GeneralSecurityException {
        KeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        );
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    public static boolean verifyPassword(String password, byte[] salt, byte[] hash)
            throws GeneralSecurityException {
        byte[] testHash = hashPassword(password, salt);
        return Arrays.equals(hash, testHash);
    }
}

// Access control demonstration
class SecureResource {
    private final Map<String, Set<String>> userRoles = new HashMap<>();
    private final Map<String, Set<String>> rolePermissions = new HashMap<>();

    public void addUserRole(String username, String role) {
        userRoles.computeIfAbsent(username, k -> new HashSet<>()).add(role);
    }

    public void addRolePermission(String role, String permission) {
        rolePermissions.computeIfAbsent(role, k -> new HashSet<>()).add(permission);
    }

    public boolean hasPermission(String username, String permission) {
        Set<String> roles = userRoles.get(username);
        if (roles == null) return false;

        return roles.stream()
            .map(rolePermissions::get)
            .filter(Objects::nonNull)
            .anyMatch(permissions -> permissions.contains(permission));
    }
}

public class SecurityDemo {
    public static void main(String[] args) {
        try {
            // Demonstrate secure data handling
            SecureData secureData = new SecureData("Sensitive information");
            System.out.println("Data integrity verified: " + secureData.verifyIntegrity());

            // Demonstrate encryption
            EncryptionUtil encryptionUtil = new EncryptionUtil();
            String plaintext = "Secret message";
            String encrypted = encryptionUtil.encrypt(plaintext);
            String decrypted = encryptionUtil.decrypt(encrypted);
            System.out.println("Original: " + plaintext);
            System.out.println("Encrypted: " + encrypted);
            System.out.println("Decrypted: " + decrypted);

            // Demonstrate digital signatures
            SignatureUtil signatureUtil = new SignatureUtil();
            String message = "Sign this message";
            byte[] signature = signatureUtil.sign(message);
            boolean isValid = signatureUtil.verify(message, signature);
            System.out.println("Signature valid: " + isValid);

            // Demonstrate password hashing
            String password = "MySecurePassword123";
            byte[] salt = PasswordUtil.generateSalt();
            byte[] passwordHash = PasswordUtil.hashPassword(password, salt);
            boolean passwordValid = PasswordUtil.verifyPassword(password, salt, passwordHash);
            System.out.println("Password verification: " + passwordValid);

            // Demonstrate access control
            SecureResource resource = new SecureResource();
            resource.addUserRole("alice", "ADMIN");
            resource.addUserRole("bob", "USER");
            resource.addRolePermission("ADMIN", "DELETE");
            resource.addRolePermission("USER", "READ");

            System.out.println("Alice can delete: " + 
                resource.hasPermission("alice", "DELETE"));
            System.out.println("Bob can delete: " + 
                resource.hasPermission("bob", "DELETE"));
            System.out.println("Bob can read: " + 
                resource.hasPermission("bob", "READ"));

        } catch (Exception e) {
            System.err.println("Security operation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}