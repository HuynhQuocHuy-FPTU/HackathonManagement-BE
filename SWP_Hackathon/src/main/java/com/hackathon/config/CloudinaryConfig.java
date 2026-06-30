package com.hackathon.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {
    //Inject value từ properties
    @Value("${cloudinary.url}")
    private String cloudinaryUrl;

    @Bean
    public Cloudinary cloudinary() {
        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
            throw new IllegalStateException("Cloudinary URL chưa được cấu hình!");
        }
        return new Cloudinary(cloudinaryUrl);
    }

}
