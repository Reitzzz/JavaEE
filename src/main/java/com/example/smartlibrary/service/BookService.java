package com.example.smartlibrary.service;

import com.example.smartlibrary.dto.BookRequest;
import com.example.smartlibrary.exception.BusinessException;
import com.example.smartlibrary.mapper.BookMapper;
import com.example.smartlibrary.model.Book;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookMapper bookMapper;

    public BookService(BookMapper bookMapper) {
        this.bookMapper = bookMapper;
    }

    public List<Book> findAll(String keyword) {
        return bookMapper.findAllWithCategory(keyword);
    }

    public Book findById(Long id) {
        return bookMapper.findByIdWithCategory(id);
    }

    public Book create(BookRequest request) {
        validate(request);
        int availableCopies = request.availableCopies() == null ? request.totalCopies() : request.availableCopies();
        Book book = new Book();
        book.setIsbn(request.isbn().trim());
        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        book.setPublisher(request.publisher().trim());
        book.setTotalCopies(request.totalCopies());
        book.setAvailableCopies(availableCopies);
        book.setCategoryId(request.categoryId());
        book.setStatus(normalizeStatus(request.status()));
        bookMapper.insert(book);
        return findById(book.getId());
    }

    public Book update(Long id, BookRequest request) {
        validate(request);
        int availableCopies = request.availableCopies() == null ? request.totalCopies() : request.availableCopies();
        if (availableCopies > request.totalCopies()) {
            throw new BusinessException("可借数量不能大于馆藏总数");
        }
        Book book = new Book();
        book.setId(id);
        book.setIsbn(request.isbn().trim());
        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        book.setPublisher(request.publisher().trim());
        book.setTotalCopies(request.totalCopies());
        book.setAvailableCopies(availableCopies);
        book.setCategoryId(request.categoryId());
        book.setStatus(normalizeStatus(request.status()));
        
        int rows = bookMapper.updateById(book);
        if (rows == 0) {
            throw new BusinessException("图书不存在");
        }
        return findById(id);
    }

    public void delete(Long id) {
        int rows = bookMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException("图书不存在");
        }
    }

    public void decreaseAvailable(Long id) {
        int rows = bookMapper.decreaseAvailable(id);
        if (rows == 0) {
            throw new BusinessException("图书库存不足");
        }
    }

    public void increaseAvailable(Long id) {
        bookMapper.increaseAvailable(id);
    }

    private void validate(BookRequest request) {
        if (request == null
                || isBlank(request.isbn())
                || isBlank(request.title())
                || isBlank(request.author())
                || isBlank(request.publisher())
                || request.categoryId() == null
                || request.totalCopies() == null
                || request.totalCopies() < 0
                || (request.availableCopies() != null && request.availableCopies() < 0)) {
            throw new BusinessException("图书 ISBN、名称、作者、出版社、分类和馆藏数量不能为空");
        }
        if (request.availableCopies() != null && request.availableCopies() > request.totalCopies()) {
            throw new BusinessException("可借数量不能大于馆藏总数");
        }
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? "ON_SHELF" : status.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
