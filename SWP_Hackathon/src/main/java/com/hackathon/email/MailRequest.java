package com.hackathon.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailRequest {
    private String to;
    private String subject;
    private String content;
    private Map<String, Object> props;
}
