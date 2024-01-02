
; Logic: Unquantified linear integer arithmetic.
; In essence, Boolean combinations of inequations between linear polynomials over integer variables.
(set-logic QF_LIA)

(push)

; Variable declarations
(declare-const price Int)
(declare-const stock Int)

; Unsigned int
(assert (and (> price 1000) (> stock 5)))

; Constraints

; Solve
(check-sat)
(get-value (price))
(get-value (stock))

(pop)