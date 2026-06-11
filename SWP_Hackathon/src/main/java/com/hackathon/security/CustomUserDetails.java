package com.hackathon.security;

import com.hackathon.entity.Account;
import com.hackathon.entity.enums.AccountStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {
    private final Account account;

    public CustomUserDetails(Account account) {
        this.account = account;
    }

    /**
     * Cấp quyền (Roles) cho user.
     * Thêm tiền tố "ROLE_" để tương thích với các annotation như @PreAuthorize("hasRole('ADMIN')")
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()));
    }

    @Override
    public String getPassword() {
        return account.getPassword();
    }

    @Override
    public String getUsername() {
        return account.getEmail();
    }

    /**
     * Tài khoản có bị hết hạn không? (Mặc định true = Không hết hạn)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Kiểm tra tài khoản có bị khóa không.
     * Ánh xạ logic: Nếu Status KHÁC BANNED thì nghĩa là không bị khóa (true).
     */
    @Override
    public boolean isAccountNonLocked() {
        return account.getStatus() != AccountStatus.BANNED;
    }

    /**
     * Mật khẩu có bị hết hạn không? (Mặc định true)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Tài khoản có đang được kích hoạt không.
     * Ánh xạ logic: Chỉ bằng true khi Status là ACTIVE.
     */
    @Override
    public boolean isEnabled() {
        return account.getStatus() == AccountStatus.ACTIVE;
    }
}
