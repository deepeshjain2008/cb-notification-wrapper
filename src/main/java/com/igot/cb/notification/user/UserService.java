package com.igot.cb.notification.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.util.ApiResponse;
import com.igot.cb.util.ProjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import java.util.Map;

import static com.igot.cb.util.Constants.*;

@RestController
@RequestMapping("/notify")
@Slf4j
public class UserService {

    @Value("${sb.api.key}")
    private String sbApiKey;

    private final RestTemplate restTemplate;

    @Autowired
    public UserService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public ApiResponse searchUsers(JsonNode payload) {
        log.info("UserSearchServiceImpl::searchUsers called");

        ApiResponse outgoingResponse = ProjectUtil.createDefaultResponse("USER_NOTIFICATION_CREATE");

        if (payload == null || payload.isEmpty()) {
            log.warn("Payload is null or empty.");
            updateErrorDetails(outgoingResponse, "Invalid or empty payload", HttpStatus.BAD_REQUEST);
            return outgoingResponse;
        }

        try {
            String uri = BASE_SEARCH_URL + USER_SEARCH_ENDPOINT;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(AUTHORIZATION, sbApiKey);

            HttpEntity<Object> request = new HttpEntity<>(payload, headers);

            log.info("Calling user search API at {}", uri);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    }
            );

            Map<String, Object> serviceResponse = response.getBody();

            if (serviceResponse == null || serviceResponse.isEmpty()) {
                updateErrorDetails(outgoingResponse, "No content from user service", HttpStatus.NO_CONTENT);
                return outgoingResponse;
            }

            outgoingResponse.setResponseCode(HttpStatus.OK);
            outgoingResponse.setResult(serviceResponse);
            return outgoingResponse;

        } catch (HttpClientErrorException hce) {
            log.error("Client error from user search: {}", hce.getResponseBodyAsString(), hce);
            updateErrorDetails(outgoingResponse, hce.getResponseBodyAsString(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (HttpMessageConversionException e) {
            log.error("Error parsing response from user search API", e);
            updateErrorDetails(outgoingResponse, "Response parsing error", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Server error in user search: {}", e.getMessage(), e);
            updateErrorDetails(outgoingResponse, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return outgoingResponse;
    }

    private void updateErrorDetails(ApiResponse response, String errMsg, HttpStatus status) {
        response.getParams().setStatus("FAILED");
        response.getParams().setErrMsg(errMsg);
        response.setResponseCode(status);
    }


}
