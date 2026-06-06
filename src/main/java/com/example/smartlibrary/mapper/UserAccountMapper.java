package com.example.smartlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartlibrary.model.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.smartlibrary.dto.UserWithStatsDTO;
import java.util.List;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    @Select("SELECT r.name FROM roles r " +
            "INNER JOIN user_roles ur ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId}")
    List<String> findRoleNames(@Param("userId") Long userId);

    @Select("SELECT u.id, u.username, u.display_name, u.status, " +
            "COUNT(br.id) AS total_borrows, " +
            "SUM(CASE WHEN br.status = 'BORROWED' THEN 1 ELSE 0 END) AS unreturned_borrows, " +
            "MAX(br.borrowed_at) AS last_borrow_time " +
            "FROM users u " +
            "LEFT JOIN borrow_records br ON br.user_id = u.id " +
            "GROUP BY u.id, u.username, u.display_name, u.status " +
            "ORDER BY u.id DESC")
    List<UserWithStatsDTO> findAllWithStats();
}
