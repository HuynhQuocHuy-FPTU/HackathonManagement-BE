package com.hackathon.service.impl;

import com.hackathon.dto.auth.ChangePasswordRequest;
import com.hackathon.dto.auth.LoginRequest;
import com.hackathon.dto.auth.AuthResponse;
import com.hackathon.dto.auth.ResetPasswordRequest;
import com.hackathon.entity.Account;
import com.hackathon.entity.RefreshToken;
import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.exception.ApiException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.RefreshTokenRepository;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.security.JwtService;
import com.hackathon.service.AuthService;
import com.hackathon.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
// Xử lý đăng nhập, làm mới phiên, đăng xuất và các thao tác liên quan đến mật khẩu.
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Xác thực thư điện tử và mật khẩu, sau đó tạo cặp mã truy cập cho tài khoản hợp lệ.
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
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
        }

        return buildAuthResponse(account);
    }

    // Thu hồi mã làm mới để phiên đăng nhập hiện tại không thể tiếp tục được sử dụng.
    @Override
    @Transactional
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Refresh token không hợp lệ"));
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    // Kiểm tra mã làm mới còn hợp lệ rồi cấp mã truy cập mới cho tài khoản.
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

        String fullName = accountRepository.findFullNameByEmail(account.getEmail()).orElse(null);
        String university = (account.getRole() == AccountRole.STUDENT && account.getStudent() != null)
                ? account.getStudent().getUniversityName() : null;

        String organization = null;
        if (account.getRole() == AccountRole.EXPERT && account.getExpert() != null) {
            organization = account.getExpert().getOrganization();
        } else if (account.getRole() == AccountRole.EVENTCOORDINATOR && account.getEventCoordinator() != null) {
            organization = account.getEventCoordinator().getOrganization();
        }

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .accountId(account.getAccountId())
                .fullName(fullName)
                .email(account.getEmail())
                .role(account.getRole())
                .avatarUrl(account.getAvatarUrl())
                .university(university)
                .organization(organization)
                .createdAt(account.getCreatedAt())
                .accountStatus(account.getStatus())
                .isPasswordChanged(account.isPasswordChanged())
                .build();
    }

    // Tạo mã đặt lại mật khẩu có thời hạn và gửi hướng dẫn đến thư điện tử của người dùng.
    @Override
    @Transactional
    public void forgotPassword(String email) {
        Account account = accountRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản với email này"));

        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        account.setResetPasswordOtp(otp);
        account.setResetPasswordOtpExpiry(LocalDateTime.now().plusMinutes(15));

        accountRepository.save(account);
        emailService.sendForgotPasswordEmail(account.getEmail(), otp);
    }

    // Xác minh mã đặt lại mật khẩu, lưu mật khẩu mới và vô hiệu hóa mã sau khi sử dụng.
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

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        account.setResetPasswordOtp(null);
        account.setResetPasswordOtpExpiry(null);

        accountRepository.saveAndFlush(account);
        refreshTokenRepository.revokeAllByAccount(account);
    }

    // Kiểm tra mật khẩu hiện tại trước khi cập nhật mật khẩu mới cho người đang đăng nhập.
    @Override
    @Transactional
    public void changePassword(CustomUserDetails userDetails, ChangePasswordRequest request) {
        Account account = accountRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));

        if (!passwordEncoder.matches(request.getOldPassword(), account.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không chính xác!");
        }
        if (passwordEncoder.matches(request.getNewPassword(), account.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu hiện tại!");
        }

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // MỞ KHÓA HỆ THỐNG
        if (!account.isPasswordChanged()) {
            account.setPasswordChanged(true);
        }

        accountRepository.saveAndFlush(account);
        refreshTokenRepository.revokeAllByAccount(account); // Xóa token cũ
    }

    // Đăng nhập bằng tài khoản Google đã được liên kết và tạo thông tin xác thực của hệ thống.
    @Override
    @Transactional

    public AuthResponse loginWithGoogle(String email) {
        Account account = accountRepository.findByEmail(email.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Tài khoản chưa tồn tại trong hệ thống.Vui lòng đăng ký trước."));

        if (account.getStatus() == AccountStatus.INACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Tài khoản chưa xác thực email. Vui lòng kiểm tra hộp thư hoặc gửi lại email xác thực.");
        }
        if (account.getStatus() == AccountStatus.BANNED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }
        log.warn("đăng nhập vs gg thành công");

        return buildAuthResponse(account);
    }

    // Tạo tài khoản mới từ thư điện tử Google khi thư điện tử đó chưa tồn tại trong hệ thống.
    @Override
    public void registerWithGoogle(String email) {
        boolean exitsAccount = accountRepository.existsByEmail(email);
        if(exitsAccount){
            throw new ApiException(HttpStatus.CONFLICT, "Email này đã được đăng ký rồi.");
        }

        Account newAcc = new Account();
        newAcc.setEmail(email);
        newAcc.setStatus(AccountStatus.ACTIVE);
        newAcc.setRole(AccountRole.STUDENT);
        newAcc.setPassword(
                passwordEncoder.encode(UUID.randomUUID().toString())
        );
        accountRepository.save(newAcc);
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

        String fullName = accountRepository.findFullNameByEmail(account.getEmail()).orElse(null);
        String university = (account.getRole() == AccountRole.STUDENT && account.getStudent() != null)
                ? account.getStudent().getUniversityName() : null;

        String organization = null;
        if (account.getRole() == AccountRole.EXPERT && account.getExpert() != null) {
            organization = account.getExpert().getOrganization();
        } else if (account.getRole() == AccountRole.EVENTCOORDINATOR && account.getEventCoordinator() != null) {
            organization = account.getEventCoordinator().getOrganization();
        }

        System.out.println(account.getGithubId());
        System.out.println(account.getGithubUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .githubId(account.getGithubId())
                .githubUsername(account.getGithubUsername())
                .accountId(account.getAccountId())
                .fullName(fullName)
                .email(account.getEmail())
                .role(account.getRole())
                .avatarUrl(account.getAvatarUrl())
                .university(university)
                .organization(organization)
                .createdAt(account.getCreatedAt())
                .accountStatus(account.getStatus())
                .isPasswordChanged(account.isPasswordChanged())
                .build();
    }
}
