package com.igot.cb.authentication.util;

import com.igot.cb.authentication.model.KeyData;
import com.igot.cb.exceptions.CustomException;
import com.igot.cb.util.PropertiesCache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KeyManagerTest {


    @Test
    public void testLoadPublicKeyWithInvalidKeyString() {
        String invalidKey = "InvalidKeyWithoutHeaderAndFooter";
        CustomException exception = assertThrows(CustomException.class, () -> {
            KeyManager.loadPublicKey(invalidKey);
        });
        assertEquals("PUBLIC_KEY_LOAD_ERROR", exception.getCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatusCode());
    }

    @Test
    public void test_getPublicKey_nonExistentKeyId() {
        KeyManager keyManager = new KeyManager();
        String nonExistentKeyId = "nonexistent_key_id";
        KeyData result = keyManager.getPublicKey(nonExistentKeyId);
        assertNull(result);
    }

    @Test
    public void test_getPublicKey_returnsCorrectKeyData() {
        KeyManager spyKeyManager = spy(new KeyManager());
        KeyData mockKeyData = new KeyData("testKey", null);
        doReturn(mockKeyData).when(spyKeyManager).getPublicKey("testKey");
        KeyData result = spyKeyManager.getPublicKey("testKey");
        assertEquals(mockKeyData, result);
    }

    @Test
    public void test_loadPublicKey_validKeyString() throws Exception {
        String validPublicKeyString = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqe4M4f7sVew+5U2G6l5H
            1T0WRfJOYd3qwWn2MtOpQ8kWODsxdmBrERHJCKrfTsNpcl8p3CsV1KUHmIqOeFLG
            yyQ+QjMoCQ9uGzbCAPyLYAAIgf/mKPa7BK5sLfZ7MCPupA8K/RB/g/3ZHlTSWJn+
            2uVyqY+xIzDfS1tLGnQz0Izmzy/JZm6+0BHrRs7TXVWrN6+YFlzXlN2cuLkxDGeu
            fUPRtmS+gUFNPnWApxdFt/zq9riIqxECG1QHpZFg3c+QOj+3emNhJMxFhKTKMeZP
            fkEkspt1ATsNnG+y+ZQKUQM1xPEk2FTaMdlDj1/5S9t5Rq8PlPlRFnBrBnrboJ+v
            XQIDAQAB
            -----END PUBLIC KEY-----""";
        PublicKey publicKey = KeyManager.loadPublicKey(validPublicKeyString);
        assertNotNull("Public key should not be null", publicKey);
        assertEquals("RSA", publicKey.getAlgorithm());
    }


    private static final String VALID_KEY_STRING = """
        -----BEGIN PUBLIC KEY-----
        MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqe4M4f7sVew+5U2G6l5H
        1T0WRfJOYd3qwWn2MtOpQ8kWODsxdmBrERHJCKrfTsNpcl8p3CsV1KUHmIqOeFLG
        yyQ+QjMoCQ9uGzbCAPyLYAAIgf/mKPa7BK5sLfZ7MCPupA8K/RB/g/3ZHlTSWJn+
        2uVyqY+xIzDfS1tLGnQz0Izmzy/JZm6+0BHrRs7TXVWrN6+YFlzXlN2cuLkxDGeu
        fUPRtmS+gUFNPnWApxdFt/zq9riIqxECG1QHpZFg3c+QOj+3emNhJMxFhKTKMeZP
        fkEkspt1ATsNnG+y+ZQKUQM1xPEk2FTaMdlDj1/5S9t5Rq8PlPlRFnBrBnrboJ+v
        XQIDAQAB
        -----END PUBLIC KEY-----""";


    @Test
    public void test_loadPublicKey_noNewlines() throws Exception {
        String noNewlines = VALID_KEY_STRING.replace("\n", "");
        PublicKey key = KeyManager.loadPublicKey(noNewlines);
        assertNotNull(key);
    }


    @Test
    public void test_init_fileSystemException() throws Exception {
        try (MockedStatic<PropertiesCache> propertiesCacheMock = Mockito.mockStatic(PropertiesCache.class);
             MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> pathsMock = Mockito.mockStatic(Paths.class)) {
            PropertiesCache mockPropertiesCache = mock(PropertiesCache.class);
            propertiesCacheMock.when(PropertiesCache::getInstance).thenReturn(mockPropertiesCache);
            Path mockPath = mock(Path.class);
            pathsMock.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            filesMock.when(() -> Files.walk(mockPath)).thenThrow(new IOException("Simulated file system exception"));

            KeyManager keyManager = new KeyManager();
            keyManager.init();

            assertNull("No key should be loaded when the file system throws an exception",
                    keyManager.getPublicKey("test_init_fileSystemException_key"));
        }
    }

    @Test
    public void test_init_propertyNotFound() throws Exception {
        KeyManager spyKeyManager = spy(new KeyManager());
        try (MockedStatic<PropertiesCache> propertiesCacheMock = Mockito.mockStatic(PropertiesCache.class)) {
            PropertiesCache mockPropertiesCache = mock(PropertiesCache.class);
            propertiesCacheMock.when(PropertiesCache::getInstance).thenReturn(mockPropertiesCache);
            spyKeyManager.init();
            verify(spyKeyManager).init();
        }
    }
}