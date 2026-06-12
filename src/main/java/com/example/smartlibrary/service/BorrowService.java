package com.example.smartlibrary.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.smartlibrary.dto.BorrowRequest;
import com.example.smartlibrary.exception.BusinessException;
import com.example.smartlibrary.mapper.BorrowRecordMapper;
import com.example.smartlibrary.mapper.UserAccountMapper;
import com.example.smartlibrary.model.Book;
import com.example.smartlibrary.model.BorrowRecord;
import com.example.smartlibrary.model.UserAccount;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BorrowService {

    private final BorrowRecordMapper borrowRecordMapper;
    private final BookService bookService;
    private final UserAccountMapper userAccountMapper;

    public BorrowService(
            BorrowRecordMapper borrowRecordMapper,
            BookService bookService,
            UserAccountMapper userAccountMapper) {
        this.borrowRecordMapper = borrowRecordMapper;
        this.bookService = bookService;
        this.userAccountMapper = userAccountMapper;
    }

    public List<BorrowRecord> findAll() {
        return borrowRecordMapper.findAllWithDetails();
    }

    public List<BorrowRecord> findMine(Authentication authentication) {
        return borrowRecordMapper.findByUserIdWithDetails(currentUser(authentication).getId());
    }

    @Transactional
    public BorrowRecord borrow(Authentication authentication, BorrowRequest request) {
        if (request == null || request.bookId() == null) {
            throw new BusinessException("请选择要借阅的图书");
        }
        int days = request.days() == null ? 14 : request.days();
        if (days <= 0 || days > 90) {
            throw new BusinessException("借阅天数必须在 1 到 90 天之间");
        }
        UserAccount user = currentUser(authentication);
        if ("BLACKLISTED".equals(user.getStatus())) {
            throw new BusinessException("您的账号已被拉黑，暂无借阅权限");
        }
        bookService.lockById(request.bookId());
        Book book = bookService.findById(request.bookId());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        if (book.getAvailableCopies() <= 0) {
            throw new BusinessException("图书暂无库存");
        }
        
        QueryWrapper<BorrowRecord> query = new QueryWrapper<>();
        query.eq("user_id", user.getId())
             .eq("book_id", book.getId())
             .eq("status", "BORROWED");
        if (borrowRecordMapper.selectCount(query) > 0) {
            throw new BusinessException("你已经借阅了这本书，请先归还");
        }
        
        bookService.decreaseAvailable(book.getId());
        
        BorrowRecord record = new BorrowRecord();
        record.setUserId(user.getId());
        record.setBookId(book.getId());
        record.setBorrowedAt(LocalDateTime.now());
        record.setDueAt(LocalDateTime.now().plusDays(days));
        record.setStatus("BORROWED");
        borrowRecordMapper.insert(record);
        
        return borrowRecordMapper.findByIdWithDetails(record.getId());
    }

    @Transactional
    public void returnBook(Authentication authentication, Long borrowId) {
        BorrowRecord record = borrowRecordMapper.findByIdForUpdate(borrowId);
        if (record == null) {
            throw new BusinessException("借阅记录不存在");
        }
        UserAccount user = currentUser(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isAdmin && !record.getUserId().equals(user.getId())) {
            throw new BusinessException("只能归还自己的借阅记录");
        }
        if (!"BORROWED".equals(record.getStatus())) {
            throw new BusinessException("该记录已归还");
        }
        
        record.setReturnedAt(LocalDateTime.now());
        record.setStatus("RETURNED");
        borrowRecordMapper.updateById(record);
        
        bookService.increaseAvailable(record.getBookId());
    }

    private UserAccount currentUser(Authentication authentication) {
        QueryWrapper<UserAccount> query = new QueryWrapper<>();
        query.eq("username", authentication.getName());
        UserAccount user = userAccountMapper.selectOne(query);
        if (user == null) {
            throw new BusinessException("当前用户不存在");
        }
        return user;
    }
}
