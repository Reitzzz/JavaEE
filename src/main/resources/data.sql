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
