package com.bd.spotify.service;

import com.bd.spotify.dto.request.ForgotPasswordRequest;
import com.bd.spotify.dto.request.LoginUserRequest;
import com.bd.spotify.dto.request.RefreshTokenRequest;
import com.bd.spotify.dto.request.RegisterUserRequest;
import com.bd.spotify.dto.response.AppUserResponse;
import com.bd.spotify.dto.response.MessageResponse;
import jakarta.validation.Valid;

public interface AuthService {

    MessageResponse registerUser(RegisterUserRequest request);

    AppUserResponse loginUser(LoginUserRequest request);

    AppUserResponse refreshAccessToken(RefreshTokenRequest request);

    MessageResponse forgotPassword(ForgotPasswordRequest request);
}
