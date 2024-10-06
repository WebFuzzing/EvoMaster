(declare-datatypes () ((ProductsRow (id-name-price (ID Int) (NAME String) (PRICE Real) ))))

(declare-const products1 ProductsRow)
(assert (= (ID products1) 1))
(check-sat)
(get-value (products1))
