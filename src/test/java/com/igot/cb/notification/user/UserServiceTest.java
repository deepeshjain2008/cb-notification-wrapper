package com.igot.cb.notification.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.util.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserService userService;
    private RestTemplate restTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        restTemplate = mock(RestTemplate.class);
        userService = new UserService(restTemplate);

        setPrivateField("sbApiKey", "dummy-api-key");
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = UserService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(userService, value);
    }

    @Test
    void testSearchUsers_nullPayload() {
        ApiResponse response = userService.searchUsers(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals("FAILED", response.getParams().getStatus());
        assertEquals("Invalid or empty payload", response.getParams().getErrMsg());
    }

    @Test
    void testSearchUsers_emptyPayload() {
        JsonNode emptyPayload = mapper.createObjectNode();
        ApiResponse response = userService.searchUsers(emptyPayload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals("FAILED", response.getParams().getStatus());
        assertEquals("Invalid or empty payload", response.getParams().getErrMsg());
    }

    @Test
    void testSearchUsers_success() {
        JsonNode payload = mapper.createObjectNode().put("key", "value");

        Map<String, Object> mockBody = Map.of("result", "some-result");
        ResponseEntity<Map<String, Object>> mockResponse =
                new ResponseEntity<>(mockBody, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                Mockito.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(mockResponse);

        ApiResponse response = userService.searchUsers(payload);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(mockBody, response.getResult());
    }

    @Test
    void testSearchUsers_serviceReturnsNullBody() {
        JsonNode payload = mapper.createObjectNode().put("key", "value");

        ResponseEntity<Map<String, Object>> mockResponse =
                new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                Mockito.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(mockResponse);

        ApiResponse response = userService.searchUsers(payload);

        assertEquals(HttpStatus.NO_CONTENT, response.getResponseCode());
        assertEquals("FAILED", response.getParams().getStatus());
        assertEquals("No content from user service", response.getParams().getErrMsg());
    }

    @Test
    void testSearchUsers_HttpClientErrorException() {
        JsonNode payload = mapper.createObjectNode().put("key", "value");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                Mockito.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request", "Client error".getBytes(), null));

        ApiResponse response = userService.searchUsers(payload);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals("FAILED", response.getParams().getStatus());
        assertTrue(response.getParams().getErrMsg().contains("Client error"));
    }

    @Test
    void testSearchUsers_HttpMessageConversionException() {
        JsonNode payload = mapper.createObjectNode().put("key", "value");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                Mockito.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenThrow(new HttpMessageConversionException("Conversion error"));

        ApiResponse response = userService.searchUsers(payload);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals("FAILED", response.getParams().getStatus());
        assertEquals("Response parsing error", response.getParams().getErrMsg());
    }

    @Test
    void testSearchUsers_genericException() {
        JsonNode payload = mapper.createObjectNode().put("key", "value");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                Mockito.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenThrow(new RuntimeException("Something went wrong"));

        ApiResponse response = userService.searchUsers(payload);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals("FAILED", response.getParams().getStatus());
        assertEquals("Internal server error", response.getParams().getErrMsg());
    }
}
