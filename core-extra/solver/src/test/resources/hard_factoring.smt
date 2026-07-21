; Factoring a large semiprime under non-linear integer arithmetic.
; 1000000016000000063 = 1000000007 * 1000000009 (both prime).
; This is hard for Z3, so with a small soft timeout it returns 'unknown'
; rather than deciding sat/unsat.
(declare-const p Int)
(declare-const q Int)
(assert (> p 1))
(assert (> q 1))
(assert (= (* p q) 1000000016000000063))
(check-sat)
