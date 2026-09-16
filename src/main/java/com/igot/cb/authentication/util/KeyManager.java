package com.igot.cb.authentication.util;

import com.igot.cb.authentication.model.KeyData;

import com.igot.cb.exceptions.CustomException;
import com.igot.cb.util.Constants;
import com.igot.cb.util.PropertiesCache;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


@Component
public class KeyManager {

  private static final Logger logger = LoggerFactory.getLogger(KeyManager.class.getName());
  private static final PropertiesCache propertiesCache = PropertiesCache.getInstance();

  private static final Map<String, KeyData> keyMap = new HashMap<>();

  @PostConstruct
  public void init() {
    // Read the content of the file and load it as a PublicKey
    String basePath = propertiesCache.getProperty(Constants.ACCESS_TOKEN_PUBLICKEY_BASEPATH);
    try (Stream<Path> walk = Files.walk(Paths.get(basePath))) {
      List<String> result =
              walk.filter(Files::isRegularFile).map(Path::toString).toList();
      result.forEach(file -> {
        try {
          Path path = Paths.get(file);
          List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
          String content = String.join(Constants.EMPTY_STRING, lines);
          KeyData keyData = new KeyData(path.getFileName().toString(), loadPublicKey(content));
          // Store the KeyData object in the keyMap
          keyMap.put(path.getFileName().toString(), keyData);
        } catch (Exception e) {
          logger.error("KeyManager:init: exception in reading public keys ", e);
        }
      });
    } catch (Exception e) {
      logger.error("KeyManager:init: exception in loading publickeys ", e);
    }
  }

  public KeyData getPublicKey(String keyId) {
    return keyMap.get(keyId);
  }


  /**
   * Loads a public key from a string representation.
   *
   * @param key The string representation of the public key
   * @return The loaded public key
   * @throws Exception If there's an error during the loading process
   */
  public static PublicKey loadPublicKey(String key) {
    // Remove header and footer from the key string
    try {
    String cleanedKey = key.replaceAll(Constants.PUBLIC_KEY_HEADER, Constants.EMPTY_STRING)
            .replaceAll(Constants.PUBLIC_KEY_FOOTER, Constants.EMPTY_STRING)
            .replaceAll(Constants.NEW_LINE_REGEX, Constants.EMPTY_STRING);
    // Decode Base64 content
    byte[] keyBytes = Base64.getDecoder().decode(cleanedKey);
    // Generate PublicKey object
    X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
          return keyFactory.generatePublic(spec);
      } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
        throw new CustomException("PUBLIC_KEY_LOAD_ERROR",
                "Failed to load public key: " + e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
}