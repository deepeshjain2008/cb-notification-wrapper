package com.igot.cb.notification.controller;

import com.igot.cb.notification.request.NotificationRequest;
import com.igot.cb.notification.service.NotificationService;
import com.igot.cb.util.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationControllerTest {

    private NotificationController controller;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        controller = new NotificationController(notificationService);
    }

    @Test
    void testCreateNotifications() {
        NotificationRequest request = new NotificationRequest();
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setResponseCode(HttpStatus.OK);

        when(notificationService.createAndSendNotifications(request)).thenReturn(apiResponse);

        ResponseEntity<ApiResponse> responseEntity = controller.createNotifications(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(apiResponse, responseEntity.getBody());

        verify(notificationService, times(1)).createAndSendNotifications(request);
    }
}
