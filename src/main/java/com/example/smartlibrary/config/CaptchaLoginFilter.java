package com.example.smartlibrary.config;

import com.example.smartlibrary.service.CaptchaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

public class CaptchaLoginFilter extends OncePerRequestFilter {

    private final CaptchaService captchaService;

    public CaptchaLoginFilter(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isLoginPost(request) && !captchaService.verify(request.getSession(), request.getParameter("captcha"))) {
            response.sendRedirect("/login?error=captcha");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLoginPost(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod()) && "/login".equals(request.getServletPath());
    }
}
