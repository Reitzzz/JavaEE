package com.example.smartlibrary.service;

import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {

    private static final String CAPTCHA_ANSWER = "captchaAnswer";

    private final SecureRandom random = new SecureRandom();

    public Map<String, String> generate(HttpSession session) {
        int left = random.nextInt(10, 50);
        int right = random.nextInt(1, 20);
        session.setAttribute(CAPTCHA_ANSWER, String.valueOf(left + right));
        return Map.of("question", left + " + " + right + " = ?");
    }

    public boolean verify(HttpSession session, String captcha) {
        Object expected = session.getAttribute(CAPTCHA_ANSWER);
        if (expected == null || captcha == null) {
            return false;
        }
        boolean matched = expected.toString().equals(captcha.trim());
        if (matched) {
            session.removeAttribute(CAPTCHA_ANSWER);
        }
        return matched;
    }
}
