package com.example.smartlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartlibrary.model.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    @Select("SELECT br.*, u.username, u.display_name, b.title AS book_title, b.author AS book_author " +
            "FROM borrow_records br " +
            "INNER JOIN users u ON u.id = br.user_id " +
            "INNER JOIN books b ON b.id = br.book_id " +
            "ORDER BY br.id DESC")
    List<BorrowRecord> findAllWithDetails();

    @Select("SELECT br.*, u.username, u.display_name, b.title AS book_title, b.author AS book_author " +
            "FROM borrow_records br " +
            "INNER JOIN users u ON u.id = br.user_id " +
            "INNER JOIN books b ON b.id = br.book_id " +
            "WHERE br.user_id = #{userId} " +
            "ORDER BY br.id DESC")
    List<BorrowRecord> findByUserIdWithDetails(@Param("userId") Long userId);

    @Select("SELECT br.*, u.username, u.display_name, b.title AS book_title, b.author AS book_author " +
            "FROM borrow_records br " +
            "INNER JOIN users u ON u.id = br.user_id " +
            "INNER JOIN books b ON b.id = br.book_id " +
            "WHERE br.id = #{id}")
    BorrowRecord findByIdWithDetails(@Param("id") Long id);

    @Select("SELECT * FROM borrow_records WHERE id = #{id} FOR UPDATE")
    BorrowRecord findByIdForUpdate(@Param("id") Long id);
}
