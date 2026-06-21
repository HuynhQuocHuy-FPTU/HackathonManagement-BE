package com.hackathon.service;

import com.hackathon.dto.auth.InviteAccountRequest;
import com.hackathon.dto.auth.LoginRequest;
import com.hackathon.dto.auth.AuthResponse;
import com.hackathon.dto.auth.ResetPasswordRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email không đúng"));

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
        } catch (Exception e) {
            System.out.println("====== DEBUG LỖI ĐĂNG NHẬP ======");
            System.out.println("Loại Exception: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("=================================");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Mật khẩu không đúng");
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

    @Override
    @Transactional
    public void forgotPassword(String email) {
        Account account = accountRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản với email này"));

        // Tạo mã OTP 6 số ngẫu nhiên
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        account.setResetPasswordOtp(otp);
        account.setResetPasswordOtpExpiry(LocalDateTime.now().plusMinutes(15));
        accountRepository.save(account);
        // Gửi OTP qua mail
        emailService.sendForgotPasswordEmail(account.getEmail(), otp);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản với email này"));

        if (account.getResetPasswordOtp() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tài khoản không có yêu cầu đổi mật khẩu nào đang chờ xử lý.");
        }

        if (!account.getResetPasswordOtp().equals(request.getOtp())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mã xác thực không chính xác.");
        }

        if (account.getResetPasswordOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mã xác thực đã hết hạn. Vui lòng yêu cầu lại.");
        }

        // Đổi pass và xóa dữ liệu OTP
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        account.setResetPasswordOtp(null);
        account.setResetPasswordOtpExpiry(null);
        accountRepository.save(account);

        // Buộc người dùng phải đăng nhập lại bằng mật khẩu mới trên tất cả thiết bị
        refreshTokenRepository.revokeAllByAccount(account);
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