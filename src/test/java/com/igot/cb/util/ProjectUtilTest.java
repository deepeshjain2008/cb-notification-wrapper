package com.igot.cb.util;

import com.igot.cb.exceptions.CustomException;
import com.igot.cb.exceptions.ResponseCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectUtilTest {

    @Test
    void testCreateServerError() {
        ResponseCode rc = ResponseCode.INTERNAL_ERROR;
        CustomException ex = ProjectUtil.createServerError(rc);

        assertNotNull(ex);
        assertEquals(rc.getErrorCode(), ex.getCode());
        assertEquals(rc.getErrorMessage(), ex.getMessage());
        assertEquals(ResponseCode.SERVER_ERROR.getCode(), ex.getResponseCode());
    }

    @Test
    void testCreateClientError() {
        ResponseCode rc = ResponseCode.UN_AUTHORIZED;
        CustomException ex = ProjectUtil.createClientException(rc);

        assertNotNull(ex);
        assertEquals(rc.getErrorCode(), ex.getCode());
        assertEquals(rc.getErrorMessage(), ex.getMessage());
        assertEquals(ResponseCode.CLIENT_ERROR.getCode(), ex.getResponseCode());
    }
}
