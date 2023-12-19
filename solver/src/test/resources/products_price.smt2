
; Logic: Unquantified linear integer arithmetic.
; In essence, Boolean combinations of inequations between linear polynomials over integer variables.
(set-logic QF_LIA)

(push)

; Variable declarations
(declare-const price Int)

; Unsigned int
(assert (> price 0))

; Constraints

; Solve
(check-sat)
(get-value (price))

(pop)