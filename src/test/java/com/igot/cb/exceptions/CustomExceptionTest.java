package com.igot.cb.exceptions;

import com.igot.cb.exceptions.CustomException;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import static org.junit.Assert.*;

public class CustomExceptionTest {

    @Test
    public void testDefaultConstructor() {
        CustomException exception = new CustomException();
        assertNull("Code should be null for default constructor", exception.getCode());
        assertNull("Message should be null for default constructor", exception.getMessage());
        assertNull("HttpStatusCode should be null for default constructor", exception.getHttpStatusCode());
    }

    @Test
    public void testParameterizedConstructor() {
        String code = "ERR_001";
        String message = "An error occurred";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        CustomException exception = new CustomException(code, message, status);
        assertEquals("Code should match the provided value", code, exception.getCode());
        assertEquals("Message should match the provided value", message, exception.getMessage());
        assertEquals("HttpStatusCode should match the provided value", status, exception.getHttpStatusCode());
    }

    @Test
    public void testResponseCodeConstructor() {
        String code = "ERR_002";
        String message = "Another error occurred";
        int responseCode = 500;
        CustomException exception = new CustomException(code, message, responseCode);
        assertEquals("Code should match the provided value", code, exception.getCode());
        assertEquals("Message should match the provided value", message, exception.getMessage());
        assertEquals("ResponseCode should match the provided value", responseCode, exception.getResponseCode());
    }

    @Test
    public void testInheritanceFromRuntimeException() {
        CustomException exception = new CustomException();
        assertTrue("CustomException should be an instance of RuntimeException",
                exception instanceof RuntimeException);
    }

    @Test
    public void testWithNullValues() {
        CustomException exception = new CustomException(null, null, null);
        assertNull("Code should be null when initialized with null", exception.getCode());
        assertNull("Message should be null when initialized with null", exception.getMessage());
        assertNull("HttpStatusCode should be null when initialized with null", exception.getHttpStatusCode());
    }
}