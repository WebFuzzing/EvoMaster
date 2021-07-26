# https://dev.mysql.com/doc/refman/8.0/en/enum.html
CREATE TABLE shirts (
                        name VARCHAR(40),
                        size ENUM('x-small', 'small', 'medium', 'large', 'x-large')
);