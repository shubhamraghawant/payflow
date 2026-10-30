package com.payflow.notification.controller;

import com.payflow.notification.entity.Notification;
import com.payflow.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationRepository notificationRepository;

    @Test
    void shouldReturnNotifications() throws Exception {
        Notification n = Notification.builder()
                .id(UUID.randomUUID())
                .paymentId("pay-123")
                .message("Payment approved")
                .severity(Notification.Severity.INFO)
                .acknowledged(false)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(n)));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].paymentId").value("pay-123"));
    }

    @Test
    void shouldReturnCriticalAlerts() throws Exception {
        when(notificationRepository.findBySeverityOrderByCreatedAtDesc(Notification.Severity.CRITICAL))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notifications/alerts"))
                .andExpect(status().isOk());
    }
}
