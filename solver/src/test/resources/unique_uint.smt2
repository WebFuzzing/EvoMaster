
; Logic
(set-logic QF_LIA)

(push)
; Current values in db for id column: [0, 1], want to retrieve two values for column: id_1 and id_2


; Variable declarations
(declare-const id_1 Int)
(declare-const id_2 Int)
; Unsigned int
(assert (>= id_1 0))
(assert (>= id_2 0))


; Constraints
(assert (not(= id_1 id_2)))
(assert (not(= id_1 1)))
(assert (not(= id_1 0)))
(assert (not(= id_2 1)))
(assert (not(= id_2 0)))

; Solve
(check-sat)
(get-value (id_1 id_2))
(pop)