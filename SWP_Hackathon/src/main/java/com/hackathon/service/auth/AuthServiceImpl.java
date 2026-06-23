package com.hackathon.service.auth;

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
import com.hackathon.security.JwtService;
import com.hackathon.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Lớp triển khai các nghiệp vụ cốt lõi về Xác thực (Authentication) và Phân quyền (Authorization).
 * Xử lý các luồng Login, Logout, làm mới Token, và khôi phục mật khẩu.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Xác thực thông tin đăng nhập và cấp phát phiên làm việc (JWT).
     *
     * @param request DTO chứa Email và Password do người dùng nhập
     * @return AuthResponse Gồm Access Token, Refresh Token và thông tin cơ bản để Frontend thiết lập phiên
     */
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"));

        // Kiểm soát trạng thái tài khoản trước khi cho phép xác thực mật khẩu
        if (account.getStatus() == AccountStatus.INACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Tài khoản chưa xác thực email. Vui lòng kiểm tra hộp thư hoặc gửi lại email xác thực.");
        }
        if (account.getStatus() == AccountStatus.BANNED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }

        try {
            // Ủy quyền cho Spring Security AuthenticationManager đối chiếu thông tin băm (hash) BCrypt
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

    /**
     * Hủy bỏ phiên làm việc bằng cách thu hồi Refresh Token trong Cơ sở dữ liệu.
     *
     * @param refreshToken Chuỗi token cần hủy
     */
    @Override
    @Transactional
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Refresh token không hợp lệ"));

        // Đánh dấu token đã bị thu hồi thay vì xóa cứng (Soft delete) để phục vụ Audit/Log
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    /**
     * Cấp phát lại Access Token mới khi token cũ hết hạn, dựa trên Refresh Token hợp lệ.
     *
     * @param refreshTokenValue Chuỗi Refresh Token do Client gửi lên
     * @return AuthResponse chứa cặp Token mới và thông tin Profile mới nhất
     */
    @Override
    @Transactional
    public AuthResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ"));

        // Kiểm tra Token hết hạn (Bảo mật chống Replay Attack với token cũ)
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }

        Account account = refreshToken.getAccount();

        // Đảm bảo trong quá trình token còn hạn, tài khoản không bị khóa đột xuất bởi Admin
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản không hợp lệ hoặc đã bị khóa");
        }

        // Sinh Access Token mới
        String newAccessToken = jwtService.generateAccessToken(account);

        return mapToAuthResponse(account, newAccessToken, refreshTokenValue);
    }

    /**
     * Khởi tạo luồng Khôi phục mật khẩu. Sinh mã OTP và gửi qua Email.
     *
     * @param email Email của tài khoản cần khôi phục
     */
    @Override
    @Transactional
    public void forgotPassword(String email) {
        Account account = accountRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản với email này"));

        // Sinh mã OTP 6 số ngẫu nhiên
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        // Thiết lập thời gian sống (TTL) cho OTP là 15 phút
        account.setResetPasswordOtp(otp);
        account.setResetPasswordOtpExpiry(LocalDateTime.now().plusMinutes(15));

        accountRepository.save(account);

        // Gọi Service gửi Email bất đồng bộ (Nên dùng Message Queue hoặc @Async nếu có thể để tránh block API)
        emailService.sendForgotPasswordEmail(account.getEmail(), otp);
    }

    /**
     * Hoàn tất khôi phục mật khẩu. Kiểm tra OTP và lưu mật khẩu mới.
     *
     * @param request DTO chứa Email, OTP và Mật khẩu mới
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản với email này"));

        // Xác minh tiến trình đổi mật khẩu có đang tồn tại không
        if (account.getResetPasswordOtp() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tài khoản không có yêu cầu đổi mật khẩu nào đang chờ xử lý.");
        }

        // Đối chiếu mã OTP
        if (!account.getResetPasswordOtp().equals(request.getOtp())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mã xác thực không chính xác.");
        }

        // Kiểm tra hiệu lực thời gian của OTP
        if (account.getResetPasswordOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mã xác thực đã hết hạn. Vui lòng yêu cầu lại.");
        }

        // Băm mật khẩu mới bằng BCrypt và xóa OTP để chống sử dụng lại (One-Time-Use)
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        account.setResetPasswordOtp(null);
        account.setResetPasswordOtpExpiry(null);

        accountRepository.save(account);

        // BẢO MẬT NGHIÊM NGẶT: Khi đổi mật khẩu thành công, bắt buộc mọi phiên làm việc cũ (trên các thiết bị khác) phải đăng xuất
        refreshTokenRepository.revokeAllByAccount(account);
    }

    /**
     * Xử lý đóng gói Token khi đăng nhập thành công.
     *
     * @param account Thực thể Account đang thao tác
     * @return AuthResponse hoàn chỉnh
     */
    private AuthResponse buildAuthResponse(Account account) {
        // Thu hồi toàn bộ các Refresh Token cũ của User này (Áp dụng chiến lược Single Device / Session Management)
        refreshTokenRepository.revokeAllByAccount(account);

        String accessToken = jwtService.generateAccessToken(account);
        String refreshTokenValue = jwtService.generateRefreshTokenValue();

        // Lưu trữ Refresh Token xuống Database để quản lý phiên dài hạn
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setAccount(account);
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpirationMs() / 1000));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return mapToAuthResponse(account, accessToken, refreshTokenValue);
    }

    /**
     * Hàm hỗ trợ (Helper Method): Đóng gói đối tượng AuthResponse.
     * Áp dụng nguyên tắc DRY (Don't Repeat Yourself), sử dụng chung cho luồng Login và Refresh Token.
     *
     * @param account           Thực thể Account đang thao tác
     * @param accessToken       JWT Access Token
     * @param refreshTokenValue JWT Refresh Token
     * @return AuthResponse được làm giàu dữ liệu (Enriched Payload) phục vụ cho Frontend
     */
    private AuthResponse mapToAuthResponse(Account account, String accessToken, String refreshTokenValue) {

        // Trích xuất thông tin định danh mở rộng (Full Name, University, Organization) từ các bảng con
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
                .accessToken(accessToken)
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
                .build();
    }
}