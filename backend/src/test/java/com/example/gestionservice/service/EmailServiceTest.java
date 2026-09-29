package com.example.gestionservice.service;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService — Tests unitaires")
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock MailProperties mailProperties;
    @InjectMocks EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        Session session = Session.getInstance(new Properties());
        mimeMessage = new MimeMessage(session);
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail — envoie l'email via Gmail SMTP quand le username est configuré")
    void shouldSendEmailWhenUsernameConfigured() {
        when(mailProperties.getUsername()).thenReturn("noreply@gmail.com");

        emailService.sendEmail("user@example.com", "Bonjour", "<h1>Hello</h1>");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail — mode dégradé quand le username est vide")
    void shouldDegradeWhenUsernameBlank() {
        when(mailProperties.getUsername()).thenReturn("");

        emailService.sendEmail("user@example.com", "Bonjour", "<h1>Hello</h1>");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmail — mode dégradé quand le username est null")
    void shouldDegradeWhenUsernameNull() {
        when(mailProperties.getUsername()).thenReturn(null);

        emailService.sendEmail("user@example.com", "Bonjour", "<h1>Hello</h1>");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmail — n'envoie pas d'exception en cas d'échec SMTP (best-effort)")
    void shouldNotThrowOnSmtpError() {
        when(mailProperties.getUsername()).thenReturn("noreply@gmail.com");
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> emailService.sendEmail("user@example.com", "Bonjour", "<h1>Hello</h1>"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendEmail — configure correctement from, to et subject")
    void shouldConfigureMessageCorrectly() throws Exception {
        when(mailProperties.getUsername()).thenReturn("noreply@gmail.com");
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendEmail("user@example.com", "Sujet test", "<p>HTML content</p>");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();

        assertThat(sent.getFrom()[0].toString()).isEqualTo("noreply@gmail.com");
        assertThat(sent.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("user@example.com");
        assertThat(sent.getSubject()).isEqualTo("Sujet test");
    }
}
