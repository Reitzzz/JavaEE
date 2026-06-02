CREATE DATABASE IF NOT EXISTS smart_library DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_library;

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(40) NOT NULL UNIQUE,
    description VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(60) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS books (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    isbn VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    author VARCHAR(80) NOT NULL,
    publisher VARCHAR(100) NOT NULL,
    total_copies INT NOT NULL,
    available_copies INT NOT NULL,
    category_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SHELF',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT ck_books_copies CHECK (total_copies >= 0 AND available_copies >= 0 AND available_copies <= total_copies)
);

CREATE TABLE IF NOT EXISTS borrow_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    borrowed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_at TIMESTAMP NOT NULL,
    returned_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'BORROWED',
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES books(id)
);

INSERT IGNORE INTO roles (name, description) VALUES
('ROLE_ADMIN', 'System administrator'),
('ROLE_READER', 'Library reader');

INSERT IGNORE INTO categories (name, description) VALUES
('Java Development', 'Java, Spring Boot and enterprise development books'),
('Artificial Intelligence', 'Machine learning and large language model books'),
('Software Engineering', 'Requirements, architecture, testing and project practice');

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787115546081', 'Spring Boot Enterprise Practice', 'Chen Ming', 'People Posts Press', 6, 6, id, 'ON_SHELF'
FROM categories WHERE name = 'Java Development';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787115612342', 'Large Language Model Application Development', 'Li Hua', 'Electronic Industry Press', 4, 4, id, 'ON_SHELF'
FROM categories WHERE name = 'Artificial Intelligence';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787302550012', 'Software Project Practice', 'Wang Lei', 'Tsinghua University Press', 5, 5, id, 'ON_SHELF'
FROM categories WHERE name = 'Software Engineering';
