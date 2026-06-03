package com.example.smartlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.smartlibrary.model.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    @Select("<script>" +
            "SELECT b.*, c.name AS category_name " +
            "FROM books b " +
            "INNER JOIN categories c ON c.id = b.category_id " +
            "<where> " +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    LOWER(b.title) LIKE CONCAT('%', LOWER(#{keyword}), '%') " +
            "    OR LOWER(b.author) LIKE CONCAT('%', LOWER(#{keyword}), '%') " +
            "    OR LOWER(b.isbn) LIKE CONCAT('%', LOWER(#{keyword}), '%') " +
            "  </if>" +
            "</where> " +
            "ORDER BY b.id DESC" +
            "</script>")
    List<Book> findAllWithCategory(@Param("keyword") String keyword);

    @Select("SELECT b.*, c.name AS category_name " +
            "FROM books b " +
            "INNER JOIN categories c ON c.id = b.category_id " +
            "WHERE b.id = #{id}")
    Book findByIdWithCategory(@Param("id") Long id);

    @Update("UPDATE books SET available_copies = available_copies - 1 WHERE id = #{id} AND available_copies > 0")
    int decreaseAvailable(@Param("id") Long id);

    @Update("UPDATE books SET available_copies = available_copies + 1 WHERE id = #{id} AND available_copies < total_copies")
    int increaseAvailable(@Param("id") Long id);
}
