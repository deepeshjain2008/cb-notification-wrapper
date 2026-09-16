package com.igot.cb.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.notification.enums.*;
import com.igot.cb.notification.request.NotificationRequest;
import com.igot.cb.notification.user.UserService;
import com.igot.cb.producer.Producer;
import com.igot.cb.util.ApiResponse;
import com.igot.cb.util.Constants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

    private NotificationServiceImpl service;
    private UserService userService;
    private ObjectMapper mapper;
    private Producer producer;

    @BeforeEach
    void setUp() throws Exception {
        userService = mock(UserService.class);
        mapper = new ObjectMapper();
        producer = mock(Producer.class);

        service = new NotificationServiceImpl(userService, mapper, producer);

        setPrivateField("topicName", "test-topic");
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = NotificationServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    @Test
    void testCreateAndSendNotifications_withUserIds() {
        NotificationRequest request = buildRequestWithUserIds();

        ApiResponse response = service.createAndSendNotifications(request);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertNotNull(response.getResult());
        verify(producer, atLeastOnce()).push(eq("test-topic"), any());
    }

    @Test
    void testCreateAndSendNotifications_withOrgSearch() {
        NotificationRequest request = buildRequestWithOrgSearch();

        ApiResponse userSearchResponse = new ApiResponse();
        userSearchResponse.setResponseCode(HttpStatus.OK);
        userSearchResponse.setResult(buildUserSearchResult());
        when(userService.searchUsers(any())).thenReturn(userSearchResponse);

        ApiResponse response = service.createAndSendNotifications(request);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertNotNull(response.getResult());
        verify(producer, atLeastOnce()).push(eq("test-topic"), any());
    }

    @Test
    void testCreateAndSendNotifications_withOrgSearchMissingOrgIdOrRoles() {
        NotificationRequest request = buildRequestWithOrgSearch();
        request.setOrgId(null);

        ApiResponse response = service.createAndSendNotifications(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testCreateAndSendNotifications_withOrgSearchNoUsersFound() {
        NotificationRequest request = buildRequestWithOrgSearch();

        ApiResponse userSearchResponse = new ApiResponse();
        userSearchResponse.setResponseCode(HttpStatus.OK);
        userSearchResponse.setResult(Collections.emptyMap());
        when(userService.searchUsers(any())).thenReturn(userSearchResponse);

        ApiResponse response = service.createAndSendNotifications(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
    }

    @Test
    void testCreateAndSendNotifications_withNoUserInfo() {
        NotificationRequest request = buildRequestWithNoUserInfo();

        ApiResponse response = service.createAndSendNotifications(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testCreateAndSendNotifications_withException() {
        NotificationRequest request = buildRequestWithUserIds();
        doThrow(new RuntimeException("kafka error")).when(producer).push(any(), any());

        ApiResponse response = service.createAndSendNotifications(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    /**
     * Utility methods for building requests & mock responses
     */

    private NotificationRequest buildRequestWithUserIds() {
        NotificationRequest.NotificationMessage message =
                new NotificationRequest.NotificationMessage();

        return new NotificationRequest(
                NotificationType.EMAIL,
                NotificationSubType.PROMOTIONAL,
                NotificationCategory.EVENT,
                NotificationSubCategory.LIKED_COMMENT,
                NotificationSource.SYSTEM_CREATED,
                null,
                List.of("user1", "user2"),
                null,
                null,
                message
        );
    }

    private NotificationRequest buildRequestWithOrgSearch() {
        NotificationRequest.NotificationMessage message =
                new NotificationRequest.NotificationMessage();

        return new NotificationRequest(
                NotificationType.EMAIL,
                NotificationSubType.ALERT,
                NotificationCategory.DISCUSSION,
                NotificationSubCategory.CONTENT_PUBLISHED,
                NotificationSource.SYSTEM_CREATED,
                List.of(UserRole.CBP_ADMIN),
                null,
                "org123",
                null,
                message
        );
    }

    private NotificationRequest buildRequestWithNoUserInfo() {
        NotificationRequest.NotificationMessage message =
                new NotificationRequest.NotificationMessage();

        return new NotificationRequest(
                NotificationType.EMAIL,
                NotificationSubType.ALERT,
                NotificationCategory.CONTENT,
                NotificationSubCategory.CONTENT_PUBLISHED,
                NotificationSource.SYSTEM_CREATED,
                null,
                null,
                null,
                null,
                message
        );
    }

    private Map<String, Object> buildUserSearchResult() {
        Map<String, Object> user = Map.of(
                Constants.USERID, "user123",
                Constants.FIRST_NAME, "John"
        );

        return Map.of(
                Constants.RESULT,
                Map.of(Constants.RESPONSE,
                        Map.of(Constants.CONTENT, List.of(user)))
        );
    }

    @Test
    void testExtractUsers_withInvalidStructure() throws Exception {
        ApiResponse resp1 = new ApiResponse();
        resp1.setResult(Map.of("wrongKey", "wrongValue"));
        assertTrue(invokeExtractUsers(resp1).isEmpty());
        ApiResponse resp2 = new ApiResponse();
        resp2.setResult(Map.of(Constants.RESULT, Map.of(Constants.RESPONSE, "not map")));
        assertTrue(invokeExtractUsers(resp2).isEmpty());
        ApiResponse resp3 = new ApiResponse();
        resp3.setResult(Map.of(Constants.RESULT, Map.of(Constants.RESPONSE, Map.of(Constants.CONTENT, "not list"))));
        assertTrue(invokeExtractUsers(resp3).isEmpty());
        ApiResponse resp4 = new ApiResponse();
        resp4.setResult(Map.of(Constants.RESULT,
                Map.of(Constants.RESPONSE, Map.of(Constants.CONTENT, List.of("not a map")))));
        assertTrue(invokeExtractUsers(resp4).isEmpty());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> invokeExtractUsers(ApiResponse resp) throws Exception {
        var m = NotificationServiceImpl.class.getDeclaredMethod("extractUsers", ApiResponse.class);
        m.setAccessible(true);
        return (List<Map<String,Object>>) m.invoke(service, resp);
    }

    @Test
    void testIsOrgSearchRequiredTrueAndFalse() throws Exception {
        var m = NotificationServiceImpl.class.getDeclaredMethod("isOrgSearchRequired", NotificationSubCategory.class);
        m.setAccessible(true);

        boolean resultTrue = (boolean) m.invoke(service, NotificationSubCategory.CONTENT_PUBLISHED);
        boolean resultFalse = (boolean) m.invoke(service, NotificationSubCategory.LIKED_COMMENT); // still true
        boolean resultFalse2 = (boolean) m.invoke(service, NotificationSubCategory.CONTENT_SHARE); // also included
        boolean resultEdge = (boolean) m.invoke(service, NotificationSubCategory.PROGRAM_PUBLISHED); // included too

        assertTrue(resultTrue);
        assertTrue(resultFalse);
        assertTrue(resultFalse2);
        assertTrue(resultEdge);
    }

    @Test
    void testGetUsersFromSearch_exceptionCase() throws Exception {
        setPrivateField("mapper", null);
        var m = NotificationServiceImpl.class.getDeclaredMethod("getUsersFromSearch", String.class, List.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> users = (List<Map<String,Object>>) m.invoke(service, "orgX", List.of(UserRole.CBP_ADMIN));
        assertTrue(users.isEmpty());
    }

    @Test
    void testErrorResponseSetsFields() throws Exception {
        ApiResponse resp = new ApiResponse();
        var m = NotificationServiceImpl.class.getDeclaredMethod("errorResponse", ApiResponse.class, String.class, HttpStatus.class);
        m.setAccessible(true);
        ApiResponse result = (ApiResponse) m.invoke(service, resp, "bad", HttpStatus.BAD_REQUEST);
        assertEquals("bad", result.getParams().getErrMsg());
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
    }

    @Test
    void testCreateAndSendNotifications_defaultsAndPlaceholders() {
        NotificationRequest.NotificationMessage message = new NotificationRequest.NotificationMessage();
        message.setPlaceholders(Map.of("title", "MyContent", "userName", "Bob"));
        message.setData(Map.of("extra", "info"));

        NotificationRequest req = new NotificationRequest(
                null, // type defaults
                null, // subtype defaults
                NotificationCategory.CONTENT,
                NotificationSubCategory.CONTENT_PUBLISHED,
                null, // source defaults
                List.of(UserRole.CBP_ADMIN),
                List.of("user42"),
                "org42",
                "ownerX",
                message
        );

        ApiResponse resp = service.createAndSendNotifications(req);
        assertEquals(HttpStatus.OK, resp.getResponseCode());
        verify(producer).push(eq("test-topic"), any());
    }

    @Test
    void testExtractUsers_exceptionCase() throws Exception {
        ApiResponse resp = mock(ApiResponse.class);
        when(resp.getResult()).thenThrow(new RuntimeException("boom"));
        assertTrue(invokeExtractUsers(resp).isEmpty());
    }

    @Test
    void testCreateAndSendNotifications_emptyCustomizedBody() {
        NotificationRequest.NotificationMessage message = new NotificationRequest.NotificationMessage();
        message.setBody("");
        message.setPlaceholders(Map.of());
        message.setData(Collections.emptyMap());
        NotificationRequest req = new NotificationRequest(
                NotificationType.SMS,
                NotificationSubType.UPDATE,
                NotificationCategory.CONTENT,
                NotificationSubCategory.CONTENT_PUBLISHED,
                NotificationSource.USER_CREATED,
                List.of(UserRole.CBP_ADMIN),
                List.of("u99"),
                "org99",
                "ownerY",
                message
        );
        ApiResponse resp = service.createAndSendNotifications(req);
        assertEquals(HttpStatus.OK, resp.getResponseCode());
        verify(producer).push(eq("test-topic"), any());
    }

    @Test
    void testCreateAndSendNotifications_orgSearchUserWithoutName() {
        NotificationRequest req = buildRequestWithOrgSearch();
        Map<String, Object> user = Map.of(Constants.USERID, "userX");
        ApiResponse userSearchResponse = new ApiResponse();
        userSearchResponse.setResponseCode(HttpStatus.OK);
        userSearchResponse.setResult(Map.of(
                Constants.RESULT, Map.of(Constants.RESPONSE, Map.of(Constants.CONTENT, List.of(user)))
        ));
        when(userService.searchUsers(any())).thenReturn(userSearchResponse);
        ApiResponse resp = service.createAndSendNotifications(req);
        assertEquals(HttpStatus.OK, resp.getResponseCode());
        verify(producer).push(eq("test-topic"), any());
    }

    @Test
    void testErrorResponseNotFound() throws Exception {
        ApiResponse resp = new ApiResponse();
        var m = NotificationServiceImpl.class.getDeclaredMethod(
                "errorResponse", ApiResponse.class, String.class, HttpStatus.class);
        m.setAccessible(true);
        ApiResponse result = (ApiResponse) m.invoke(service, resp, "missing", HttpStatus.NOT_FOUND);
        assertEquals("missing", result.getParams().getErrMsg());
        assertEquals(HttpStatus.NOT_FOUND, result.getResponseCode());
    }

    @Test
    void testIsOrgSearchRequired_withNull_returnsFalse() throws Exception {
        var m = NotificationServiceImpl.class.getDeclaredMethod(
                "isOrgSearchRequired", NotificationSubCategory.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(service, new Object[]{null});
        assertFalse(result);
    }

    @Test
    void testCreateAndSendNotifications_userServiceFailure() {
        NotificationRequest request = buildRequestWithOrgSearch();
        ApiResponse userSearchResponse = new ApiResponse();
        userSearchResponse.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
        when(userService.searchUsers(any())).thenReturn(userSearchResponse);
        ApiResponse response = service.createAndSendNotifications(request);
        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
    }

    @Test
    void testErrorResponseInternalServerError() throws Exception {
        ApiResponse resp = new ApiResponse();
        var m = NotificationServiceImpl.class.getDeclaredMethod(
                "errorResponse", ApiResponse.class, String.class, HttpStatus.class);
        m.setAccessible(true);
        ApiResponse result = (ApiResponse) m.invoke(service, resp, "serverFail", HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals("serverFail", result.getParams().getErrMsg());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getResponseCode());
    }

    @Test
    void testCreateAndSendNotifications_withNullOwnerId() {
        NotificationRequest.NotificationMessage message = new NotificationRequest.NotificationMessage();
        NotificationRequest req = new NotificationRequest(
                NotificationType.EMAIL,
                NotificationSubType.UPDATE,
                NotificationCategory.CONTENT,
                NotificationSubCategory.CONTENT_PUBLISHED,
                NotificationSource.SYSTEM_CREATED,
                List.of(UserRole.CBP_ADMIN),
                List.of("user99"),
                "orgX",
                null,
                message
        );
        ApiResponse resp = service.createAndSendNotifications(req);
        assertEquals(HttpStatus.OK, resp.getResponseCode());
        verify(producer).push(eq("test-topic"), any());
    }

}
