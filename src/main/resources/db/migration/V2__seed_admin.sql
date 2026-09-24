INSERT INTO users (username, password, full_name, email, role, enabled, must_change_password, created_at, updated_at)
VALUES (
           'admin',
           '$2y$10$EsB6fJlRHYRJxBNGrhaQ1.kFNGJPybBN.ffFefUTq5yuqe7LHG3d.',
           'System Administrator',
           'admin@example.com',
           'ADMIN',
           TRUE,
           TRUE,
           NOW(),
           NOW()
       );