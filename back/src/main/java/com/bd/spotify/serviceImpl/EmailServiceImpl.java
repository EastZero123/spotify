package com.bd.spotify.serviceImpl;

import com.bd.spotify.service.EmailService;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    
    @Override
    public void sendCredentialsEmail(String toEmail, String userName, String password) {

    }

    @Override
    public void sendWelcomeEmail(String toEmail, String userName, String password) {

    }
}
