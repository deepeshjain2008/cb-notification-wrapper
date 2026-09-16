package com.igot.cb.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.notification.enums.*;
import com.igot.cb.notification.user.UserService;
import com.igot.cb.producer.Producer;
import com.igot.cb.notification.request.NotificationRequest;
import com.igot.cb.notification.service.NotificationService;

import com.igot.cb.util.ApiResponse;
import com.igot.cb.util.Constants;
import com.igot.cb.util.ProjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static com.igot.cb.util.Constants.*;


@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final UserService userService;
    private final ObjectMapper mapper;
    private final Producer producer;

    @Value("${kafka.topic.name}")
    private String topicName;

    @Autowired
    public NotificationServiceImpl(UserService userService, ObjectMapper mapper, Producer producer) {
        this.userService = userService;
        this.mapper = mapper;
        this.producer = producer;
    }


    @Override
    public ApiResponse createAndSendNotifications(NotificationRequest input) {
        ApiResponse response = ProjectUtil.createDefaultResponse(USER_NOTIFICATION);

        try {
            List<Map<String, Object>> users = new ArrayList<>();
            boolean isSearch = false;

            if (!ObjectUtils.isEmpty(input.getUserIds())) {
                for (String id : input.getUserIds()) {
                    users.add(Map.of(USER_ID, id));
                }

            } else if (isOrgSearchRequired(input.getSubCategory())) {
                if (!StringUtils.hasText(input.getOrgId()) || ObjectUtils.isEmpty(input.getRoles())) {
                    return errorResponse(response, "orgId or roles missing", HttpStatus.BAD_REQUEST);
                }

                users = getUsersFromSearch(input.getOrgId(), input.getRoles());
                if (ObjectUtils.isEmpty(users)) {
                    return errorResponse(response, "No users found", HttpStatus.NOT_FOUND);
                }
                isSearch = true;

            } else {
                return errorResponse(response, "No user information provided", HttpStatus.BAD_REQUEST);
            }
            NotificationSubCategory subCategoryEnum = input.getSubCategory();
            String template = subCategoryEnum.getMessageTemplate();
            NotificationCategory category = subCategoryEnum.getCategory();
            NotificationRequest.NotificationMessage message = input.getMessage();
            Map<String, String> basePlaceholders = message.getPlaceholders() != null
                    ? new HashMap<>(message.getPlaceholders())
                    : new HashMap<>();

            for (Map<String, Object> user : users) {
                String userId = (String) user.getOrDefault(USER_ID, user.get(USERID));
                String userName = "";

                Map<String, String> placeholders = new HashMap<>(basePlaceholders);
                placeholders.put(USERID, userId);

                if (isSearch) {
                    userName = (String) user.getOrDefault(USER_NAME, EMPTY_STRING);
                    placeholders.put(USER_NAME, userName);
                } else {
                    userName = placeholders.getOrDefault(USER_NAME, EMPTY_STRING);
                    placeholders.put(USER_NAME, userName);
                }
                String customizedBody = template;
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    customizedBody = customizedBody.replace("{" + entry.getKey() + "}", Optional.ofNullable(entry.getValue()).orElse(EMPTY_STRING));
                }
                Map<String, Object> messageMap = new HashMap<>();

                if (StringUtils.hasText(customizedBody)) {
                    messageMap.put(BODY, customizedBody);
                }

                if (message.getData() != null && !message.getData().isEmpty()) {
                    messageMap.put(DATA, message.getData());
                }
                if (subCategoryEnum == NotificationSubCategory.CONTENT_RETIREMENT_SEVEN_DAYS
                        || subCategoryEnum == NotificationSubCategory.CONTENT_RETIREMENT_ONE_DAYS
                        || subCategoryEnum == NotificationSubCategory.APPROVED_CONTENT_RETIREMENT) {
                    subCategoryEnum = NotificationSubCategory.CONTENT_RETIRE;
                }
                Map<String, Object> kafkaMessage = Map.of(
                        Constants.USER_IDS, List.of(Map.of(USER_ID, userId)),
                        Constants.TYPE, input.getType() != null ? input.getType().name() : NotificationType.IN_APP,
                        Constants.SUB_TYPE, input.getSubType() != null ? input.getSubType().name() : NotificationSubType.ALERT,
                        Constants.CATEGORY, category,
                        Constants.SUB_CATEGORY, subCategoryEnum.name(),
                        Constants.SOURCE, input.getSource() != null ? input.getSource().name() : NotificationSource.USER_CREATED,
                        Constants.MESSAGE, messageMap

                );


                Map<String, Object> kafkaPayload = Map.of(Constants.REQUEST, kafkaMessage);
                producer.push(topicName, kafkaPayload);

            }

            response.setResponseCode(HttpStatus.OK);
            response.setResult(Map.of("status", "Notification(s) sent to Kafka"));
            return response;

        } catch (Exception e) {
            log.error("Error in createNotifications", e);
            return errorResponse(response, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    private List<Map<String, Object>> getUsersFromSearch(String orgId, List<UserRole> roles) {
        try {
            ObjectNode filters = mapper.createObjectNode();
            filters.put(ROOT_ORG_ID, orgId);
            filters.set(ORGANISATIONS_ROLES, mapper.valueToTree(roles));
            ObjectNode request = mapper.createObjectNode();
            request.set(Constants.REQUEST, mapper.createObjectNode().set(FILTERS, filters));

            ApiResponse userSearchResponse = userService.searchUsers(request);
            if (userSearchResponse.getResponseCode() != HttpStatus.OK) {
                return Collections.emptyList();
            }

            return extractUsers(userSearchResponse);
        } catch (Exception e) {
            log.error("User search failed", e);
            return Collections.emptyList();
        }
    }

    private ApiResponse errorResponse(ApiResponse response, String msg, HttpStatus status) {
        response.getParams().setStatus(Constants.FAILED);
        response.getParams().setErrMsg(msg);
        response.setResponseCode(status);
        return response;
    }

    private boolean isOrgSearchRequired(NotificationSubCategory subCategory) {
        return EnumSet.of(
                NotificationSubCategory.CONTENT_PUBLISHED,
                NotificationSubCategory.CONTENT_SPV_PUBLISHED,
                NotificationSubCategory.CONTENT_REVIEW_REQUEST,
                NotificationSubCategory.CONTENT_EDITED,
                NotificationSubCategory.CONTENT_REJECTED,
                NotificationSubCategory.LIKED_POST,
                NotificationSubCategory.LIKED_COMMENT,
                NotificationSubCategory.REPLIED_POST,
                NotificationSubCategory.POST_COMMENT,
                NotificationSubCategory.REPLIED_COMMENT,
                NotificationSubCategory.SEND_CONNECTION_REQUEST,
                NotificationSubCategory.ACCEPTED_CONNECTION_REQUEST,
                NotificationSubCategory.PROFILE_VERIFICATION,
                NotificationSubCategory.PROFILE_UPDATE,
                NotificationSubCategory.TRANSFER_UPDATE,
                NotificationSubCategory.USER_TRANSFER,
                NotificationSubCategory.CONTENT_SHARE,
                NotificationSubCategory.TAGGED_COMMENT,
                NotificationSubCategory.TAGGED_POST,
                NotificationSubCategory.EVENT_PUBLISHED,
                NotificationSubCategory.EVENT_ENROLLED,
                NotificationSubCategory.COURSE_PUBLISHED,
                NotificationSubCategory.LEARN_DISCUSSION_POST_REPLY,
                NotificationSubCategory.LEARN_DISCUSSION_POST_COMMENT,
                NotificationSubCategory.PROGRAM_PUBLISHED,
                NotificationSubCategory.PROFANITY_CHECK,
                NotificationSubCategory.DELETED_BATCH,
                NotificationSubCategory.BP_ASSIGNMENT_UPLOAD,
                NotificationSubCategory.BP_ASSIGNMENT_EVALUATE,
                NotificationSubCategory.BP_ASSIGNMENT_SUBMIT,
                NotificationSubCategory.INSTRUCTOR_ADD_BATCH,
                NotificationSubCategory.APPROVED_CONTENT_RETIREMENT,
                NotificationSubCategory.CONTENT_RETIREMENT_SEVEN_DAYS,
                NotificationSubCategory.CONTENT_RETIREMENT_ONE_DAYS,
                NotificationSubCategory.CONTENT_RETIRED,
                NotificationSubCategory.RETIRE_REJECTED,
                NotificationSubCategory.RETIRE_APPROVED,
                NotificationSubCategory.EXTERNAL_TRAINING,
                NotificationSubCategory.CONTENT_UN_ENROLLED,
                NotificationSubCategory.CONTENT_RE_ENROLLED
        ).contains(subCategory);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractUsers(ApiResponse response) {
        try {
            Object resultObj = response.getResult();
            if (!(resultObj instanceof Map<?, ?> fullResult)) {
                return Collections.emptyList();
            }

            Object nestedResultObj = fullResult.get(Constants.RESULT);
            if (!(nestedResultObj instanceof Map<?, ?> nestedResult)) {
                return Collections.emptyList();
            }

            Object responseNodeObj = nestedResult.get(Constants.RESPONSE);
            if (!(responseNodeObj instanceof Map<?, ?> responseNode)) {
                return Collections.emptyList();
            }

            Object contentObj = responseNode.get(Constants.CONTENT);
            if (!(contentObj instanceof List<?> contentList)) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> simpleUsers = new ArrayList<>();
            for (Object userObj : contentList) {
                if (userObj instanceof Map<?, ?> userMap) {
                    Map<String, Object> simple = new HashMap<>();
                    simple.put(USERID, userMap.get(USERID));
                    simple.put(USER_NAME, userMap.get(FIRST_NAME));
                    simpleUsers.add(simple);
                }
            }

            return simpleUsers;

        } catch (Exception e) {
            log.error("Failed to extract users from response", e);
            return Collections.emptyList();
        }
    }


}
