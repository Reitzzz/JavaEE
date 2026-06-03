package com.example.smartlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartlibrary.model.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    @Select("SELECT r.name FROM roles r " +
            "INNER JOIN user_roles ur ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId}")
    List<String> findRoleNames(@Param("userId") Long userId);
}
