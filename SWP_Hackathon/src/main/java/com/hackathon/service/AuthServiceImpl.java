package com.hackathon.service;

import com.hackathon.dto.auth.LoginRequest;
import com.hackathon.dto.auth.AuthResponse;
import com.hackathon.entity.Account;
import com.hackathon.entity.RefreshToken;
import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.exception.ApiException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.RefreshTokenRepository;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"));

        if (account.getStatus() == AccountStatus.INACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Tài khoản chưa xác thực email. Vui lòng kiểm tra hộp thư hoặc gửi lại email xác thực.");
        }
        if (account.getStatus() == AccountStatus.BANNED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(account.getEmail(), request.getPassword())
            );
        } catch (DisabledException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản chưa được kích hoạt");
        }

        return buildAuthResponse(account);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Refresh token không hợp lệ"));
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public AuthResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ"));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token đã hết hạn");
        }

        Account account = refreshToken.getAccount();
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản không hợp lệ");
        }

        String newAccessToken = jwtService.generateAccessToken(account);
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .accountId(account.getAccountId())
                .accountName(account.getAccountName())
                .email(account.getEmail())
                .role(account.getRole())
                .build();
    }

    @Override
    public AuthResponse getCurrentUser(CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .expiresIn(0)
                .accountId(account.getAccountId())
                .accountName(account.getAccountName())
                .email(account.getEmail())
                .role(account.getRole())
                .build();
    }

    private AuthResponse buildAuthResponse(Account account) {
        refreshTokenRepository.revokeAllByAccount(account);

        String accessToken = jwtService.generateAccessToken(account);
        String refreshTokenValue = jwtService.generateRefreshTokenValue();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setAccount(account);
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpirationMs() / 1000));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .accountId(account.getAccountId())
                .accountName(account.getAccountName())
                .email(account.getEmail())
                .role(account.getRole())
                .build();
    }
}