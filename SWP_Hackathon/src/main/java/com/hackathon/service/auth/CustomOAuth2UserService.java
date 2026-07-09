package com.hackathon.service.auth;

import com.hackathon.entity.Account;
import com.hackathon.exception.ApiException;
import com.hackathon.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private AccountRepository accountRepository; // Inject Repository của bạn vào đây

    public OAuth2User loadUser(OAuth2UserRequest userRequest)  {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Lấy thông tin từ Google trả về
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        // Trả về đối tượng user hợp lệ cho Spring Security giữ cấu hình đăng nhập
        return oAuth2User;
    }
}
