package com.bd.spotify.serviceImpl;

import com.bd.spotify.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendCredentialsEmail(String toEmail, String userName, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Spotify - Your Temporary Password");
            String emailBody = """
        안녕하세요 %s님,
        
        비밀번호 재설정 요청이 접수되었습니다. 아래의 임시 비밀번호를 확인해 주세요.
        
        임시 비밀번호: %s
        
        로그인 후 보안을 위해 반드시 비밀번호를 즉시 변경해 주시기 바랍니다.
        
        감사합니다.
        Spotify 팀 드림
        """.formatted(userName, password);
            message.setText(emailBody);
            mailSender.send(message);

            logger.info("Temporary password email sent to {}: {}", toEmail, password);
        } catch (Exception e) {
            logger.error("Failed to send temporary password email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send temporary password email", e);
        }
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String userName, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to Spotify - Your Account is Ready");

            String emailBody = """
        안녕하세요 %s님,
        
        Spotify의 가족이 되신 것을 진심으로 환영합니다!
        계정이 성공적으로 생성되었으며, 로그인 정보는 다음과 같습니다.
        
        [로그인 정보]
        - 이메일: %s
        - 임시 비밀번호: %s
        
        아래 링크를 통해 서비스에 로그인하실 수 있습니다.
        주소: %s/login
        
        ※ 보안을 위해 첫 로그인 후 마이페이지에서 비밀번호를 반드시 변경해 주세요.
        
        Spotify와 함께 즐거운 감상 되시길 바랍니다.
        
        감사합니다.
        Spotify 팀 드림
        """.formatted(userName, toEmail, password, frontendUrl);
            message.setText(emailBody);
            mailSender.send(message);
            logger.info("Welcome email sent to {}: {}", toEmail, password);
        } catch (Exception e) {
            logger.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }
}
