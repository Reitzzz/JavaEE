package com.example.smartlibrary.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.smartlibrary.dto.RegisterRequest;
import com.example.smartlibrary.exception.BusinessException;
import com.example.smartlibrary.mapper.UserAccountMapper;
import com.example.smartlibrary.model.UserAccount;
import com.example.smartlibrary.service.CaptchaService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final CaptchaService captchaService;

    public AuthController(
            UserAccountMapper userAccountMapper,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            CaptchaService captchaService
    ) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.captchaService = captchaService;
    }

    @GetMapping("/api/captcha")
    public Map<String, String> captcha(HttpSession session) {
        return captchaService.generate(session);
    }

    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .toList();
        return Map.of("username", authentication.getName(), "roles", roles);
    }

    @PostMapping("/api/register")
    public Map<String, Object> register(@RequestBody RegisterRequest request, HttpSession session) {
        String username = normalize(request.username());
        String password = normalize(request.password());
        String confirmPassword = normalize(request.confirmPassword());
        String displayName = normalize(request.displayName());
        String captcha = normalize(request.captcha());

        if (!captchaService.verify(session, captcha)) {
            throw new BusinessException("验证码错误，请重新输入");
        }
        if (username.length() < 3 || username.length() > 60) {
            throw new BusinessException("用户名长度需为 3-60 个字符");
        }
        if (!username.matches("[A-Za-z0-9_]+")) {
            throw new BusinessException("用户名只能包含字母、数字和下划线");
        }
        if (password.length() < 6 || password.length() > 60) {
            throw new BusinessException("密码长度需为 6-60 个字符");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (displayName.isBlank()) {
            displayName = username;
        }

        QueryWrapper<UserAccount> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        if (userAccountMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在，请换一个");
        }

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setDisplayName(displayName);
        account.setEnabled(true);
        userAccountMapper.insert(account);

        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, "ROLE_READER");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", account.getId(), roleId);
        return Map.of("success", true, "message", "注册成功，请登录");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
