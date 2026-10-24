package com.example.splitease.service;

import com.example.splitease.repo.UserRepo;
import com.google.firebase.messaging.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @InjectMocks
    private NotificationService notificationService;

    private MockedStatic<FirebaseMessaging> staticFirebase;

    @BeforeEach
    void setUp() {
        staticFirebase = mockStatic(FirebaseMessaging.class);
        staticFirebase.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
    }

    @AfterEach
    void tearDown() {
        staticFirebase.close();
    }

    private FirebaseMessagingException fcmError(MessagingErrorCode code) {
        FirebaseMessagingException e = mock(FirebaseMessagingException.class);
        when(e.getMessagingErrorCode()).thenReturn(code);
        return e;
    }

    @Test
    void sendsMessageThroughFirebase() throws Exception {
        notificationService.sendPushNotification("token", "Title", "Body");

        verify(firebaseMessaging).send(any(Message.class));
        verifyNoInteractions(userRepo);
    }

    @Test
    void clearsUnregisteredToken() throws Exception {
        FirebaseMessagingException error = fcmError(MessagingErrorCode.UNREGISTERED);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(error);

        notificationService.sendPushNotification("dead-token", "Title", "Body");

        verify(userRepo).clearFcmTokenByToken("dead-token");
    }

    @Test
    void clearsInvalidToken() throws Exception {
        FirebaseMessagingException error = fcmError(MessagingErrorCode.INVALID_ARGUMENT);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(error);

        notificationService.sendPushNotification("bad-token", "Title", "Body");

        verify(userRepo).clearFcmTokenByToken("bad-token");
    }

    @Test
    void keepsTokenOnTransientFailure() throws Exception {
        FirebaseMessagingException error = fcmError(MessagingErrorCode.UNAVAILABLE);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(error);

        assertThatCode(() -> notificationService.sendPushNotification("token", "Title", "Body"))
                .doesNotThrowAnyException();
        verifyNoInteractions(userRepo);
    }

    @Test
    void doesNotThrowWhenFirebaseIsNotInitialised() {
        staticFirebase.when(FirebaseMessaging::getInstance)
                .thenThrow(new IllegalStateException("FirebaseApp with name [DEFAULT] doesn't exist."));

        assertThatCode(() -> notificationService.sendPushNotification("token", "Title", "Body"))
                .doesNotThrowAnyException();
        verifyNoInteractions(userRepo);
    }
}
