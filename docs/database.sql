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
('Software Engineering', 'Requirements, architecture, testing and project practice'),
('Database Systems', 'SQL, database design, transactions and data management'),
('Web Development', 'Frontend, backend and full-stack web development books'),
('Data Structures', 'Algorithms, data structures and programming fundamentals'),
('Cloud Native', 'Microservices, containers, DevOps and cloud architecture'),
('Cybersecurity', 'Secure coding, network security and application protection');

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787115546081', 'Spring Boot 企业级实战', 'Chen Ming', 'People Posts Press', 6, 6, id, 'ON_SHELF'
FROM categories WHERE name = 'Java Development';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787121419026', 'Java 核心技术精要', 'Zhang Wei', 'Electronic Industry Press', 5, 5, id, 'ON_SHELF'
FROM categories WHERE name = 'Java Development';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787302609338', 'Spring Cloud 微服务设计', 'Liu Yang', 'Tsinghua University Press', 4, 4, id, 'ON_SHELF'
FROM categories WHERE name = 'Java Development';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787115612342', '大语言模型应用开发', 'Li Hua', 'Electronic Industry Press', 4, 4, id, 'ON_SHELF'
FROM categories WHERE name = 'Artificial Intelligence';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787115598233', '机器学习实践指南', 'Sun Qiang', 'People Posts Press', 5, 5, id, 'ON_SHELF'
FROM categories WHERE name = 'Artificial Intelligence';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787302585120', '深度学习基础', 'Zhao Ning', 'Tsinghua University Press', 3, 3, id, 'ON_SHELF'
FROM categories WHERE name = 'Artificial Intelligence';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787302550012', '软件项目实践', 'Wang Lei', 'Tsinghua University Press', 5, 5, id, 'ON_SHELF'
FROM categories WHERE name = 'Software Engineering';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787115504188', '整洁架构案例研究', 'Gao Yuan', 'People Posts Press', 4, 4, id, 'ON_SHELF'
FROM categories WHERE name = 'Software Engineering';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787121392282', '敏捷软件测试', 'Hu Mei', 'Electronic Industry Press', 6, 6, id, 'ON_SHELF'
FROM categories WHERE name = 'Software Engineering';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787115521971', '数据库系统概念与实践', 'Lin Tao', 'People Posts Press', 5, 5, id, 'ON_SHELF'
FROM categories WHERE name = 'Database Systems';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787302578917', 'MySQL 性能优化', 'Xu Kai', 'Tsinghua University Press', 4, 4, id, 'ON_SHELF'
FROM categories WHERE name = 'Database Systems';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787121407719', 'Redis 设计与应用', 'Feng Rui', 'Electronic Industry Press', 4, 4, id, 'ON_SHELF'
FROM categories WHERE name = 'Database Systems';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787115583190', '现代 JavaScript 开发', 'Deng Jie', 'People Posts Press', 6, 6, id, 'ON_SHELF'
FROM categories WHERE name = 'Web Development';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787302596119', 'Vue 与 Spring Boot 全栈开发', 'Qian Min', 'Tsinghua University Press', 5, 5, id, 'ON_SHELF'
FROM categories WHERE name = 'Web Development';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787121428890', 'RESTful API 设计实践', 'Ma Jun', 'Electronic Industry Press', 4, 4, id, 'ON_SHELF'
FROM categories WHERE name = 'Web Development';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787115576666', 'Java 数据结构', 'He Chen', 'People Posts Press', 6, 6, id, 'ON_SHELF'
FROM categories WHERE name = 'Data Structures';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787302549801', '算法设计与分析', 'Tang Yi', 'Tsinghua University Press', 5, 5, id, 'ON_SHELF'
FROM categories WHERE name = 'Data Structures';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787121376023', '程序设计竞赛基础', 'Wu Hao', 'Electronic Industry Press', 3, 3, id, 'ON_SHELF'
FROM categories WHERE name = 'Data Structures';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787115601184', 'Docker 与 Kubernetes 实战', 'Zhou Hang', 'People Posts Press', 4, 4, id, 'ON_SHELF'
FROM categories WHERE name = 'Cloud Native';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787302619024', 'DevOps 交付流水线', 'Cai Xin', 'Tsinghua University Press', 4, 4, id, 'ON_SHELF'
FROM categories WHERE name = 'Cloud Native';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787121433207', '云原生架构模式', 'Luo Fei', 'Electronic Industry Press', 3, 3, id, 'ON_SHELF'
FROM categories WHERE name = 'Cloud Native';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787115567008', 'Web 应用安全', 'Pan Yu', 'People Posts Press', 4, 4, id, 'ON_SHELF'
FROM categories WHERE name = 'Cybersecurity';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787302567882', 'Java 安全编码', 'Jiang Nan', 'Tsinghua University Press', 5, 5, id, 'ON_SHELF'
FROM categories WHERE name = 'Cybersecurity';

INSERT IGNORE INTO books (isbn, title, author, publisher, total_copies, available_copies, category_id, status)
SELECT '9787121440151', '网络安全运维', 'Shen Ke', 'Electronic Industry Press', 3, 3, id, 'ON_SHELF'
FROM categories WHERE name = 'Cybersecurity';

UPDATE books SET title = CASE isbn
    WHEN '9787115546081' THEN 'Spring Boot 企业级实战'
    WHEN '9787121419026' THEN 'Java 核心技术精要'
    WHEN '9787302609338' THEN 'Spring Cloud 微服务设计'
    WHEN '9787115612342' THEN '大语言模型应用开发'
    WHEN '9787115598233' THEN '机器学习实践指南'
    WHEN '9787302585120' THEN '深度学习基础'
    WHEN '9787302550012' THEN '软件项目实践'
    WHEN '9787115504188' THEN '整洁架构案例研究'
    WHEN '9787121392282' THEN '敏捷软件测试'
    WHEN '9787115521971' THEN '数据库系统概念与实践'
    WHEN '9787302578917' THEN 'MySQL 性能优化'
    WHEN '9787121407719' THEN 'Redis 设计与应用'
    WHEN '9787115583190' THEN '现代 JavaScript 开发'
    WHEN '9787302596119' THEN 'Vue 与 Spring Boot 全栈开发'
    WHEN '9787121428890' THEN 'RESTful API 设计实践'
    WHEN '9787115576666' THEN 'Java 数据结构'
    WHEN '9787302549801' THEN '算法设计与分析'
    WHEN '9787121376023' THEN '程序设计竞赛基础'
    WHEN '9787115601184' THEN 'Docker 与 Kubernetes 实战'
    WHEN '9787302619024' THEN 'DevOps 交付流水线'
    WHEN '9787121433207' THEN '云原生架构模式'
    WHEN '9787115567008' THEN 'Web 应用安全'
    WHEN '9787302567882' THEN 'Java 安全编码'
    WHEN '9787121440151' THEN '网络安全运维'
    ELSE title
END
WHERE isbn IN (
    '9787115546081', '9787121419026', '9787302609338',
    '9787115612342', '9787115598233', '9787302585120',
    '9787302550012', '9787115504188', '9787121392282',
    '9787115521971', '9787302578917', '9787121407719',
    '9787115583190', '9787302596119', '9787121428890',
    '9787115576666', '9787302549801', '9787121376023',
    '9787115601184', '9787302619024', '9787121433207',
    '9787115567008', '9787302567882', '9787121440151'
);
