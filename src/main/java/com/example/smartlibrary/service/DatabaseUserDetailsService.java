package com.example.smartlibrary.service;

import com.example.smartlibrary.model.UserAccount;
import com.example.smartlibrary.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        List<SimpleGrantedAuthority> authorities = userRepository.findRoleNames(account.id())
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return User.withUsername(account.username())
                .password(account.password())
                .disabled(!account.enabled())
                .authorities(authorities)
                .build();
    }
}
