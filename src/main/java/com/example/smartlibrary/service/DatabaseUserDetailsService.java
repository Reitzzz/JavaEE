package com.example.smartlibrary.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.smartlibrary.mapper.UserAccountMapper;
import com.example.smartlibrary.model.UserAccount;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserAccountMapper userAccountMapper;

    public DatabaseUserDetailsService(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        QueryWrapper<UserAccount> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        UserAccount account = userAccountMapper.selectOne(wrapper);
        if (account == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        
        List<SimpleGrantedAuthority> authorities = userAccountMapper.findRoleNames(account.getId())
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return User.withUsername(account.getUsername())
                .password(account.getPassword())
                .disabled(!account.isEnabled())
                .authorities(authorities)
                .build();
    }
}
