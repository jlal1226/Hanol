package com.ssafy.hanol.notification.service;

import com.ssafy.hanol.common.exception.CustomException;
import com.ssafy.hanol.notification.domain.NotificationConfiguration;
import com.ssafy.hanol.notification.domain.NotificationType;
import com.ssafy.hanol.notification.exception.NotificationErrorCode;
import com.ssafy.hanol.notification.repository.NotificationRepository;
import com.ssafy.hanol.notification.service.dto.request.NotificationModifyRequest;
import com.ssafy.hanol.notification.service.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationResponse findNotifications(Long memberId) {
        NotificationResponse notificationResponse = notificationRepository.findNotificationResponseByMemberId(memberId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.NOT_FOUND_NOTIFICATION_CONFIGURATION));
        return notificationResponse;
    }

    public NotificationResponse modifyNotification(NotificationModifyRequest notificationModifyRequest, Long memberId) {
        NotificationConfiguration notificationConfiguration = notificationRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.NOT_FOUND_NOTIFICATION_CONFIGURATION));

        // 변경하려는 Notification 유형에 따라 업데이트
        NotificationType notificationType = notificationModifyRequest.getNotificationType();
        if(notificationType.equals(NotificationType.CHECK_ROUTINE)) {
            notificationConfiguration.changeIsCheckRoutineActive(notificationModifyRequest.getIsActive());
        } else if(notificationType.equals(NotificationType.INDIVIDUAL_ROUTINE)) {
            notificationConfiguration.changeIsIndividualRoutineActive(notificationModifyRequest.getIsActive());
        }

        return NotificationResponse.builder()
                .notificationConfigurationId(notificationConfiguration.getId())
                .memberId(memberId)
                .isCheckRoutineActive(notificationConfiguration.getIsCheckRoutineActive())
                .isIndividualRoutineActive(notificationConfiguration.getIsIndividualRoutineActive())
                .build();
    }
}
