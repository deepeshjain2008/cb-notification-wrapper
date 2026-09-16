package com.igot.cb.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void testDefaultConstructor() {
        ApiResponse apiResponse = new ApiResponse();
        assertNull(apiResponse.getId(), "Id should be null for default constructor");
        assertEquals("v1", apiResponse.getVer(), "Version should be v1");
        assertNotNull(apiResponse.getTs(), "Timestamp should not be null");
        assertNotNull(apiResponse.getParams(), "Params should not be null");
        assertNotNull(apiResponse.getResult(), "Result map should not be empty");
        assertTrue(apiResponse.getResult().isEmpty(), "Result map should be empty");
        assertNull(apiResponse.getResponseCode(), "ResponseCode should be null");
    }

    @Test
    void testParameterizedConstructor() {
        String id = "api.test.id";
        ApiResponse apiResponse = new ApiResponse(id);
        assertEquals(id, apiResponse.getId(), "Id should match the one passed in constructor");
        assertEquals("v1", apiResponse.getVer(), "Version should be v1");
        assertNotNull(apiResponse.getTs(), "Timestamp should not be null");
        assertNotNull(apiResponse.getParams(), "Params should not be null");
        assertNotNull(apiResponse.getResult(), "Result map should not be empty");
        assertTrue(apiResponse.getResult().isEmpty(), "Result map should be empty");
        assertNull(apiResponse.getResponseCode(), "ResponseCode should be null");
    }

    @Test
    void testSettersAndGetters() {
        ApiResponse apiResponse = new ApiResponse();
        String id = "api.test.updated";
        String ver = "v2";
        String ts = new Timestamp(System.currentTimeMillis()).toString();
        ApiRespParam params = new ApiRespParam("test-msg-id");
        HttpStatus responseCode = HttpStatus.OK;
        apiResponse.setId(id);
        apiResponse.setVer(ver);
        apiResponse.setTs(ts);
        apiResponse.setParams(params);
        apiResponse.setResponseCode(responseCode);
        assertEquals(id, apiResponse.getId(), "Id should be updated");
        assertEquals(ver, apiResponse.getVer(), "Version should be updated");
        assertEquals(ts, apiResponse.getTs(), "Timestamp should be updated");
        assertEquals(params, apiResponse.getParams(), "Params should be updated");
        assertEquals(responseCode, apiResponse.getResponseCode(), "ResponseCode should be updated");
    }

    @Test
    void testPutMethod() {
        ApiResponse apiResponse = new ApiResponse();
        String key = "testKey";
        String value = "testValue";
        apiResponse.put(key, value);
        assertEquals(value, apiResponse.getResult().get(key), "Result map should contain the added entry");
        assertFalse(apiResponse.getResult().isEmpty(), "Result map should not be empty after adding an entry");
    }

    @Test
    void testGetResultMethod() {
        ApiResponse apiResponse = new ApiResponse();
        String key = "testKey";
        String value = "testValue";
        Map<String, Object> result = apiResponse.getResult();
        result.put(key, value);
        assertEquals(value, apiResponse.getResult().get(key), "Result map should contain the added entry");
    }

    @Test
    void testTimestampFormat() {
        ApiResponse apiResponse = new ApiResponse();
        String ts = apiResponse.getTs();
        assertDoesNotThrow(() -> Timestamp.valueOf(ts), "Timestamp should be in a valid format");
    }

    @Test
    void testParamsInitialization() {
        ApiResponse apiResponse = new ApiResponse();
        ApiRespParam params = apiResponse.getParams();

        assertNotNull(params, "Params should not be null");
        assertNotNull(params.getResMsgId(), "Params should have a resMsgId");
        assertEquals(params.getResMsgId(), params.getMsgId(), "resMsgId and msgId should be equal");
        assertNull(params.getErr(), "Error should be null by default");
        assertNull(params.getErrMsg(), "Error message should be null by default");
        assertNull(params.getStatus(), "Status should be null by default");
    }

    @Test
    void testMultiplePutOperations() {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.put("key1", "value1");
        apiResponse.put("key2", 123);
        apiResponse.put("key3", true);
        assertEquals(3, apiResponse.getResult().size(), "Result map should contain 3 entries");
        assertEquals("value1", apiResponse.getResult().get("key1"), "First value should match");
        assertEquals(123, apiResponse.getResult().get("key2"), "Second value should match");
        assertEquals(true, apiResponse.getResult().get("key3"), "Third value should match");
    }

    @Test
    void testGetMethodReturnsNullWhenKeyNotPresent() {
        ApiResponse apiResponse = new ApiResponse();
        assertNull(apiResponse.get("nonExistingKey"), "Should return null for a missing key");
    }

    @Test
    void testSetResultOverwritesResponse() {
        ApiResponse apiResponse = new ApiResponse();
        Map<String, Object> newMap = new HashMap<>();
        newMap.put("newKey", "newValue");
        apiResponse.setResult(newMap);
        assertEquals("newValue", apiResponse.get("newKey"), "setResult should overwrite the response map");
        assertEquals(1, apiResponse.getResult().size(), "Response should only contain new entries");
    }

    @Test
    void testPutAndGetWithDifferentDataTypes() {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.put("intKey", 10);
        apiResponse.put("doubleKey", 15.5);
        apiResponse.put("booleanKey", false);
        assertEquals(10, apiResponse.get("intKey"));
        assertEquals(15.5, apiResponse.get("doubleKey"));
        assertEquals(false, apiResponse.get("booleanKey"));
    }

    @Test
    void testResponseCodeSetterAndGetter() {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setResponseCode(HttpStatus.BAD_REQUEST);
        assertEquals(HttpStatus.BAD_REQUEST, apiResponse.getResponseCode(), "ResponseCode should be BAD_REQUEST");
    }

    @Test
    void testEqualsAndHashCode() {
        ApiResponse apiResponse1 = new ApiResponse("testId");
        ApiResponse apiResponse2 = new ApiResponse("testId");
        assertNotEquals(apiResponse1, apiResponse2, "Timestamps differ so they should not be equal");
        assertNotEquals(apiResponse1.hashCode(), apiResponse2.hashCode(), "Hash codes should also differ");
    }

    @Test
    void testToStringNotNull() {
        ApiResponse apiResponse = new ApiResponse("someId");
        assertNotNull(apiResponse.toString(), "toString should never return null");
    }

    @Test
    void testGetFromEmptyResponse() {
        ApiResponse apiResponse = new ApiResponse();
        assertNull(apiResponse.get("missingKey"), "Should return null if key not present");
    }

    @Test
    void testSetResultReplacesExistingResponse() {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.put("oldKey", "oldValue");
        Map<String, Object> newMap = new HashMap<>();
        newMap.put("newKey", "newValue");
        apiResponse.setResult(newMap);
        assertNull(apiResponse.get("oldKey"), "Old map should be replaced");
        assertEquals("newValue", apiResponse.get("newKey"), "New map should be used");
    }

    @Test
    void testEqualsAndHashCodeDifferentObjects() {
        ApiResponse apiResponse1 = new ApiResponse("id1");
        ApiResponse apiResponse2 = new ApiResponse("id2");
        assertNotEquals(apiResponse1, apiResponse2, "Different ids → not equal");
        assertNotEquals(apiResponse1.hashCode(), apiResponse2.hashCode(), "Different ids → hashCodes differ");
    }

    @Test
    void testEqualsSameReference() {
        ApiResponse apiResponse = new ApiResponse("sameId");
        assertEquals(apiResponse, apiResponse, "An object should be equal to itself");
    }

    @Test
    void testEqualsWithNullAndDifferentClass() {
        ApiResponse apiResponse = new ApiResponse("id");
        assertNotEquals(null, apiResponse, "Object should not equal null");
        assertNotEquals("string", apiResponse, "Object should not equal different class");
    }

    @Test
    void testToStringContainsIdAndVersion() {
        String id = "customId";
        ApiResponse apiResponse = new ApiResponse(id);
        String toString = apiResponse.toString();
        assertTrue(toString.contains(id), "toString should include id");
        assertTrue(toString.contains("v1"), "toString should include version");
    }

    @Test
    void testEqualsDifferentResponseMap() {
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");
        Map<String, Object> map1 = new HashMap<>();
        map1.put("a", 1);
        apiResponse1.setResult(map1);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("b", 2);
        apiResponse2.setResult(map2);
        assertNotEquals(apiResponse1, apiResponse2, "Different maps should make objects unequal");
    }

    @Test
    void testApiRespParamSettersAndGetters() {
        String msgId = "msg123";
        ApiRespParam param = new ApiRespParam(msgId);
        assertEquals(msgId, param.getMsgId(), "msgId should match constructor arg");
        assertEquals(msgId, param.getResMsgId(), "resMsgId should default to msgId");
        param.setErr("404");
        param.setErrMsg("Not Found");
        param.setStatus("FAILED");
        assertEquals("404", param.getErr());
        assertEquals("Not Found", param.getErrMsg());
        assertEquals("FAILED", param.getStatus());
    }

    @Test
    void testResponseCodeNull() {
        ApiResponse apiResponse = new ApiResponse();
        assertNull(apiResponse.getResponseCode(), "Default responseCode should be null");
    }

    @Test
    void testApiRespParamEqualsHashCodeAndToString() {
        ApiRespParam param3 = new ApiRespParam("id1");
        ApiRespParam param2 = new ApiRespParam("id1");
        assertNotEquals(param3, param2, "Different objects are not equal even with same values");
        assertEquals(param3, param3, "Same reference should be equal");
        assertNotEquals(param3.hashCode(), param2.hashCode(),
                "Hashcodes may differ since equals is not overridden");
        assertNotNull(param3.toString(), "toString should not return null");
    }

    @Test
    void testEqualsAndHashCodeSameValues() {
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");
        apiResponse1.setResponseCode(HttpStatus.OK);
        apiResponse2.setResponseCode(HttpStatus.OK);
        apiResponse1.setResult(new HashMap<>());
        apiResponse2.setResult(new HashMap<>());
        assertNotEquals(apiResponse1, apiResponse2, "Different params objects → not equal");
        assertNotEquals(apiResponse1.hashCode(), apiResponse2.hashCode(), "Hashcodes differ because params differ");
    }

    @Test
    void testEqualsWithNullFields() {
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");
        apiResponse1.setVer(null);
        apiResponse2.setVer(null);
        apiResponse1.setTs(null);
        apiResponse2.setTs(null);
        assertNotEquals(apiResponse1, apiResponse2, "Different params make them unequal even if ver/ts null");
    }

    @Test
    void testEqualsDifferentResponseCode() {
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");
        apiResponse1.setResponseCode(HttpStatus.OK);
        apiResponse2.setResponseCode(HttpStatus.BAD_REQUEST);
        assertNotEquals(apiResponse1, apiResponse2, "Different responseCodes should make objects unequal");
    }

    @Test
    void testApiRespParamWithDifferentFieldsNotEqual() {
        ApiRespParam param1 = new ApiRespParam("id");
        ApiRespParam param2 = new ApiRespParam("id");

        param1.setErr("404");
        param2.setErr("500");

        assertNotEquals(param1, param2, "Different err values → not equal");
    }

    @Test
    void testToStringWithNullFields() {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setId(null);
        apiResponse.setVer(null);
        apiResponse.setTs(null);
        apiResponse.setParams(null);
        apiResponse.setResponseCode(null);

        String str = apiResponse.toString();
        assertNotNull(str);
        assertTrue(str.contains("id=null"));
    }

    @Test
    void testEqualsIdNullInOneObject() {
        ApiResponse apiResponse1 = new ApiResponse();
        ApiResponse apiResponse2 = new ApiResponse();
        apiResponse1.setId(null);
        apiResponse2.setId("nonNull");
        assertNotEquals(apiResponse1, apiResponse2, "Null vs non-null id should not be equal");
    }

    @Test
    void testEqualsDifferentVer() {
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");
        apiResponse1.setVer("v1");
        apiResponse2.setVer("v2");
        assertNotEquals(apiResponse1, apiResponse2, "Different ver values → not equal");
    }

    @Test
    void testEqualsDifferentTimestamp() {
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");
        apiResponse1.setTs("2020-01-01 00:00:00.0");
        apiResponse2.setTs("2021-01-01 00:00:00.0");
        assertNotEquals(apiResponse1, apiResponse2, "Different ts values → not equal");
    }

    @Test
    void testEqualsParamsNullInOneObject() {
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");
        apiResponse1.setParams(null);
        assertNotEquals(apiResponse1, apiResponse2, "Null params vs non-null params → not equal");
    }

    @Test
    void testEqualsResponseNullInOneObject() {
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");
        apiResponse1.setResult(null);
        apiResponse2.setResult(new HashMap<>());
        assertNotEquals(apiResponse1, apiResponse2, "Null vs non-null response map → not equal");
    }

    @Test
    void testEqualsResponseCodeNullVsNonNull() {
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");
        apiResponse1.setResponseCode(null);
        apiResponse2.setResponseCode(HttpStatus.OK);
        assertNotEquals(apiResponse1, apiResponse2, "Null vs non-null responseCode → not equal");
    }

    @Test
    void testEqualsAndHashCodeAllFieldsSame() {
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");
        HttpStatus status = HttpStatus.OK;
        Map<String, Object> map = new HashMap<>();
        map.put("k", "v");
        apiResponse1.setResponseCode(status);
        apiResponse2.setResponseCode(status);
        apiResponse1.setResult(map);
        apiResponse2.setResult(map);
        ApiRespParam param = new ApiRespParam("msg123");
        apiResponse1.setParams(param);
        apiResponse2.setParams(param);
        String ts = "2025-01-01 00:00:00.0";
        apiResponse1.setVer("vX");
        apiResponse2.setVer("vX");
        apiResponse1.setTs(ts);
        apiResponse2.setTs(ts);
        assertEquals(apiResponse1, apiResponse2, "All fields same → objects should be equal");
        assertEquals(apiResponse1.hashCode(), apiResponse2.hashCode(), "Equal objects must have equal hashCodes");
    }

    @Test
    void testEqualsDifferentMapsSameSize() {
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");
        Map<String, Object> map1 = new HashMap<>();
        map1.put("a", 1);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("a", 2); // same key, different value
        apiResponse1.setResult(map1);
        apiResponse2.setResult(map2);
        assertNotEquals(apiResponse1, apiResponse2, "Same keys but different values → not equal");
    }

    @Test
    void testEqualsWithMixedNullFields() {
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");

        apiResponse1.setVer(null);
        apiResponse2.setVer("v1");

        assertNotEquals(apiResponse1, apiResponse2, "One null ver vs non-null ver → not equal");
    }

    @Test
    void testHashCodeWithNullFields() {
        ApiResponse apiResponse = new ApiResponse("id");
        apiResponse.setVer(null);
        apiResponse.setTs(null);
        apiResponse.setParams(null);
        apiResponse.setResult(null);
        apiResponse.setResponseCode(null);

        assertDoesNotThrow(apiResponse::hashCode, "hashCode() should work even with null fields");
    }

    @Test
    void testApiRespParamToStringWithNulls() {
        ApiRespParam param = new ApiRespParam(null);
        param.setErr(null);
        param.setErrMsg(null);
        param.setStatus(null);
        String str = param.toString();
        assertNotNull(str, "toString should not return null even with null fields");
    }

    @Test
    void testApiRespParamEqualsNullAndDifferentClass() {
        ApiRespParam param = new ApiRespParam("id");
        assertNotEquals(null, param, "Param should not equal null");
        assertNotEquals("string", param, "Param should not equal different class");
    }

    @Test
    void testApiRespParamAllNullsDifferentObjectsNotEqual() {
        ApiRespParam param1 = new ApiRespParam(null);
        ApiRespParam param2 = new ApiRespParam(null);
        param1.setErr(null);
        param1.setErrMsg(null);
        param1.setStatus(null);
        param2.setErr(null);
        param2.setErrMsg(null);
        param2.setStatus(null);
        assertNotEquals(param1, param2, "Different objects are not equal even if all fields are null");
    }

    @Test
    void testApiRespParamHashCodeWithNulls() {
        ApiRespParam param = new ApiRespParam(null);
        param.setErr(null);
        param.setErrMsg(null);
        param.setStatus(null);
        assertDoesNotThrow(param::hashCode, "hashCode should not throw even when all fields are null");
    }

    @Test
    void testApiRespParamEqualsSameReference() {
        ApiRespParam param = new ApiRespParam("id");
        assertEquals(param, param, "Same object reference should be equal");
    }

    @Test
    void testApiResponseEqualsAllNullFields() {
        ApiResponse apiResponse1 = new ApiResponse();
        ApiResponse apiResponse2 = new ApiResponse();
        apiResponse1.setId(null);
        apiResponse2.setId(null);
        apiResponse1.setVer(null);
        apiResponse2.setVer(null);
        apiResponse1.setTs(null);
        apiResponse2.setTs(null);
        apiResponse1.setParams(null);
        apiResponse2.setParams(null);
        apiResponse1.setResult(null);
        apiResponse2.setResult(null);
        apiResponse1.setResponseCode(null);
        apiResponse2.setResponseCode(null);
        assertEquals(apiResponse1, apiResponse2, "Two ApiResponse objects with all nulls should be equal");
        assertEquals(apiResponse1.hashCode(), apiResponse2.hashCode(), "Equal objects should have same hashCode");
    }

    @Test
    void testApiResponseToStringPartialNulls() {
        ApiResponse apiResponse = new ApiResponse("id123");
        apiResponse.setVer(null);
        String str = apiResponse.toString();
        assertTrue(str.contains("id123"));
        assertTrue(str.contains("ver=null"));
    }

    @Test
    void testApiRespParamEqualsSameInstance() {
        ApiRespParam param = new ApiRespParam("id");
        assertEquals(param, param, "An object should equal itself");
    }

    @Test
    void testApiRespParamEqualsDifferentClass() {
        ApiRespParam param = new ApiRespParam("id");
        assertNotEquals("not a param", param, "ApiRespParam should not equal an unrelated object");
    }

    @Test
    void testApiRespParamHashCodeConsistency() {
        ApiRespParam param = new ApiRespParam("id");
        int initialHash = param.hashCode();
        assertEquals(initialHash, param.hashCode(), "hashCode should be consistent across calls");
    }

    @Test
    void testApiResponseToStringWithMixedFields() {
        ApiResponse apiResponse = new ApiResponse("mixId");
        apiResponse.setVer(null);
        apiResponse.setParams(new ApiRespParam("msg"));
        String str = apiResponse.toString();
        assertTrue(str.contains("mixId"));
        assertTrue(str.contains("ver=null"));
        assertTrue(str.contains("params="), "toString should still include params");
    }


    @Test
    void testGetWhenResultIsNull() {
        ApiResponse apiResponse = new ApiResponse("id");
        apiResponse.setResult(null);
        assertThrows(NullPointerException.class,
                () -> apiResponse.get("anyKey"),
                "get() should throw NPE when response map is null");
    }

    @Test
    void testPutWhenResultIsNull() {
        ApiResponse apiResponse = new ApiResponse("id");
        apiResponse.setResult(null); // force result to null
        assertThrows(NullPointerException.class, () -> apiResponse.put("key", "value"),
                "put() on null result map should throw NPE");
    }

    @Test
    void testEqualsSameIdDifferentParamsObjects() {
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");
        apiResponse1.setParams(new ApiRespParam("p1"));
        apiResponse2.setParams(new ApiRespParam("p2"));
        assertNotEquals(apiResponse1, apiResponse2, "Different params values should make them unequal");
    }

    @Test
    void testEqualsSameIdSameParamsDifferentResult() {
        ApiRespParam param = new ApiRespParam("msg");
        ApiResponse apiResponse1 = new ApiResponse("id");
        ApiResponse apiResponse2 = new ApiResponse("id");
        apiResponse1.setParams(param);
        apiResponse2.setParams(param);
        apiResponse1.put("k1", "v1");
        apiResponse2.put("k2", "v2");
        assertNotEquals(apiResponse1, apiResponse2,
                "ApiResponse with different result maps should not be equal even if other fields match");
    }


    @Test
    void testToStringWithAllFieldsSet() {
        ApiRespParam param = new ApiRespParam("msgId");
        ApiResponse apiResponse = new ApiResponse("id123");
        apiResponse.setVer("v9");
        apiResponse.setTs("2030-01-01 12:00:00.0");
        apiResponse.setParams(param);
        apiResponse.put("k", "v");
        apiResponse.setResponseCode(HttpStatus.OK);
        String str = apiResponse.toString();
        assertTrue(str.contains("id123"));
        assertTrue(str.contains("v9"));
        assertTrue(str.contains("2030-01-01"));
        assertTrue(str.contains("params="));
        assertTrue(str.contains("OK"));
    }

}