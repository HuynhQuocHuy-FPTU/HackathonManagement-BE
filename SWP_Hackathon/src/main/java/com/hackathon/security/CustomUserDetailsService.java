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
     * Spring Security gọi hàm này với email làm username.
     */
    @Override
    public UserDetails loadUserByUsername(String email) {
        return accountRepository.findByEmail(email.trim().toLowerCase())
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email));
    }
}
