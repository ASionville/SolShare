package solShareApp;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class UserGroupsManager {
    private static final String FILE_NAME = "user_groups.dat";
    private static final String ALGORITHM = "AES";

    public static void saveUserGroup(String userAddress, BigInteger groupId, String password) {
        List<BigInteger> existing = getUserGroups(userAddress, password);
        if (existing.contains(groupId)) return;

        String data = userAddress + "_" + groupId.toString();
        String encrypted = encrypt(data, password);
        if (encrypted != null) {
            appendToFile(encrypted);
        }
    }

    public static List<BigInteger> getUserGroups(String userAddress, String password) {
        List<BigInteger> groups = new ArrayList<>();
        List<String> lines = readLines();
        for (String line : lines) {
            String decrypted = decrypt(line, password);
            if (decrypted != null && decrypted.startsWith(userAddress + "_")) {
                try {
                    String groupIdStr = decrypted.substring(userAddress.length() + 1);
                    groups.add(new BigInteger(groupIdStr));
                } catch (Exception e) {
                    // Ignore malformed
                }
            }
        }
        return groups;
    }

    private static void appendToFile(String line) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
            bw.write(line);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> readLines() {
        List<String> lines = new ArrayList<>();
        File file = new File(FILE_NAME);
        if (!file.exists()) return lines;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) lines.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private static SecretKeySpec getKey(String myKey) {
        try {
            byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            return new SecretKeySpec(key, ALGORITHM);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String encrypt(String strToEncrypt, String secret) {
        try {
            SecretKeySpec secretKey = getKey(secret);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    private static String decrypt(String strToDecrypt, String secret) {
        try {
            SecretKeySpec secretKey = getKey(secret);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            // Decryption failed (wrong key or bad data), just return null
            return null;
        }
    }
}
