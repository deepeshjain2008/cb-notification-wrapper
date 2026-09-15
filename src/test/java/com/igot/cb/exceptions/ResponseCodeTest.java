package com.igot.cb.exceptions;

import com.igot.cb.util.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseCodeTest {

    @Test
    void testEnumValuesAndFields() {
        // test enums with String errorCode & message
        ResponseCode unauthorized = ResponseCode.UN_AUTHORIZED;
        assertEquals("UNAUTHORIZED_USER", unauthorized.getErrorCode());
        assertEquals("You are not authorized.", unauthorized.getErrorMessage());

        ResponseCode internalError = ResponseCode.INTERNAL_ERROR;
        assertEquals("INTERNAL_ERROR", internalError.getErrorCode());
        assertEquals("Process failed, please try again later.", internalError.getErrorMessage());

        // test enums with int responseCode
        ResponseCode ok = ResponseCode.OK;
        assertEquals(200, ok.getResponseCode());

        ResponseCode clientError = ResponseCode.CLIENT_ERROR;
        assertEquals(400, clientError.getResponseCode());

        ResponseCode serverError = ResponseCode.SERVER_ERROR;
        assertEquals(500, serverError.getResponseCode());
    }

    @Test
    void testGetResponseWithNull() {
        assertNull(ResponseCode.getResponse(null));
    }

    @Test
    void testGetResponseWithUnauthorizedConstant() {
        ResponseCode result = ResponseCode.getResponse(Constants.UNAUTHORIZED);
        assertEquals(ResponseCode.UN_AUTHORIZED, result);
    }

    @Test
    void testGetResponseWithMatchingErrorCode() {
        ResponseCode result = ResponseCode.getResponse("UNAUTHORIZED_USER");
        assertEquals(ResponseCode.UN_AUTHORIZED, result);

        result = ResponseCode.getResponse("INTERNAL_ERROR");
        assertEquals(ResponseCode.INTERNAL_ERROR, result);
    }


    @Test
    void testGetMessage() {
        String msg = ResponseCode.OK.getMessage(200);
        assertNotNull(msg);
        assertEquals("", msg);
    }
}
