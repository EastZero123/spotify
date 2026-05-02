package com.bd.spotify.serviceImpl;

import com.bd.spotify.dto.request.ForgotPasswordRequest;
import com.bd.spotify.dto.request.LoginUserRequest;
import com.bd.spotify.dto.request.RefreshTokenRequest;
import com.bd.spotify.dto.request.RegisterUserRequest;
import com.bd.spotify.dto.response.AppUserResponse;
import com.bd.spotify.dto.response.MessageResponse;
import com.bd.spotify.entity.AppUser;
import com.bd.spotify.exception.*;
import com.bd.spotify.repository.AppUserRepository;
import com.bd.spotify.service.AuthService;
import com.bd.spotify.service.EmailService;
import com.bd.spotify.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Override
    public MessageResponse registerUser(RegisterUserRequest request) {
        if(appUserRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        String tempPassword = generateTemporaryPassword();
        AppUser appUser = new AppUser();
        appUser.setName(request.getName());
        appUser.setEmail(request.getEmail());
        appUser.setPassword(passwordEncoder.encode(tempPassword));
        appUser.setRole(request.getRole() != null ? request.getRole() : "USER");

        appUserRepository.save(appUser);
        emailService.sendCredentialsEmail(request.getEmail(), request.getName(), tempPassword);

        return new MessageResponse("계정 생성 완료! 입력한 이메일로 임시 비밀번호 전달했습니다.");
    }

    @Override
    public AppUserResponse loginUser(LoginUserRequest request) {
        AppUser appUser = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("비정상 이메일 혹은 패스워드"));

        if(!passwordEncoder.matches(request.getPassword(), appUser.getPassword())) {
            throw new InvalidCredentialsException("비정상 이메일 혹은 패스워드");
        }

        String accessToken = jwtUtil.generateAccessToken(appUser.getId(), appUser.getName(), appUser.getEmail(), appUser.getRole());

        String refreshToken = jwtUtil.generateRefreshToken(appUser.getId(), appUser.getEmail());

        appUser.setRefreshToken(refreshToken);
        appUserRepository.save(appUser);

        return AppUserResponse.fromEntity(appUser, accessToken, refreshToken);
    }

    @Override
    public AppUserResponse refreshAccessToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        String email = jwtUtil.extractEmail(refreshToken);

        if(!jwtUtil.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("비정상 토큰");
        }

        AppUser appUser = appUserRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("비정상 토큰"));

        if(!jwtUtil.validateToken(refreshToken, email)) {
            throw new TokenExpiredException("토큰 만료");
        }

        String accessToken = jwtUtil.generateAccessToken(appUser.getId(), appUser.getName(), appUser.getEmail(), appUser.getRole());

        return AppUserResponse.fromEntity(appUser, accessToken, refreshToken);
    }

    @Override
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        AppUser appUser = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("이메일이 없습니다 " + request.getEmail()));

        String tempPassword = generateTemporaryPassword();

        appUser.setPassword(passwordEncoder.encode(tempPassword));
        appUserRepository.save(appUser);

        emailService.sendCredentialsEmail(request.getEmail(), appUser.getName(), tempPassword);

        return new MessageResponse("임시 비밀번호가 전달되었습니다.");
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(10);

        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}
