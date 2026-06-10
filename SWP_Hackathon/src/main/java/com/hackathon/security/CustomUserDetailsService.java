package com.hackathon.security;

import com.hackathon.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final AccountRepository accountRepository;
    /**
     * Hàm này nhận vào accountName, tìm trong DB và trả về CustomUserDetails.
     * Nếu không thấy sẽ ném ra ngoại lệ tiêu chuẩn của Spring Security.
     */
    @Override
    public UserDetails loadUserByUsername(String accountName) {
        return accountRepository.findByAccountName(accountName)
                .map(CustomUserDetails::new) // Nếu tìm thấy Account -> Bọc vào CustomUserDetails
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản với account name: " + accountName));
    }
}
