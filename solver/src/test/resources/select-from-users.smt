(declare-datatypes () ((UsersRow (id-name-age-points (ID Int) (NAME String) (AGE Int) (POINTS Int) ))))

(declare-const users1 UsersRow)
(declare-const users2 UsersRow)
(check-sat)
(get-value (users1))
(get-value (users2))