package com.example.smartlibrary.service;

import com.example.smartlibrary.dto.BorrowRequest;
import com.example.smartlibrary.exception.BusinessException;
import com.example.smartlibrary.model.Book;
import com.example.smartlibrary.model.BorrowRecord;
import com.example.smartlibrary.model.UserAccount;
import com.example.smartlibrary.repository.BookRepository;
import com.example.smartlibrary.repository.BorrowRecordRepository;
import com.example.smartlibrary.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public BorrowService(
            BorrowRecordRepository borrowRecordRepository,
            BookRepository bookRepository,
            UserRepository userRepository) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    public List<BorrowRecord> findAll() {
        return borrowRecordRepository.findAll();
    }

    public List<BorrowRecord> findMine(Authentication authentication) {
        return borrowRecordRepository.findByUserId(currentUser(authentication).id());
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
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new BusinessException("图书不存在"));
        if (book.availableCopies() <= 0) {
            throw new BusinessException("图书暂无库存");
        }
        if (borrowRecordRepository.hasActiveBorrow(user.id(), book.id())) {
            throw new BusinessException("你已经借阅了这本书，请先归还");
        }
        bookRepository.decreaseAvailable(book.id());
        return borrowRecordRepository.create(user.id(), book.id(), days);
    }

    @Transactional
    public void returnBook(Authentication authentication, Long borrowId) {
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new BusinessException("借阅记录不存在"));
        UserAccount user = currentUser(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isAdmin && !record.userId().equals(user.id())) {
            throw new BusinessException("只能归还自己的借阅记录");
        }
        if (!"BORROWED".equals(record.status())) {
            throw new BusinessException("该记录已归还");
        }
        borrowRecordRepository.returnBook(borrowId);
        bookRepository.increaseAvailable(record.bookId());
    }

    private UserAccount currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException("当前用户不存在"));
    }
}
