package com.example.smartlibrary.service;

import com.example.smartlibrary.dto.UserWithStatsDTO;
import com.example.smartlibrary.exception.BusinessException;
import com.example.smartlibrary.mapper.UserAccountMapper;
import com.example.smartlibrary.model.UserAccount;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserAccountMapper userAccountMapper;

    public UserService(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    public List<UserWithStatsDTO> findAllWithStats() {
        return userAccountMapper.findAllWithStats();
    }

    public void banUser(Long id) {
        UserAccount user = userAccountMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus("BLACKLISTED");
        userAccountMapper.updateById(user);
    }

    public void unbanUser(Long id) {
        UserAccount user = userAccountMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus("NORMAL");
        userAccountMapper.updateById(user);
    }
}
