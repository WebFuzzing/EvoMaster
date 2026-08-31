-- Corpus of SELECT statements used by SmtLibGeneratorCorpusTest.
--
-- These are synthetic, but deliberately written in the shape an ORM emits: derived table aliases,
-- every projected column listed with an alias, and joins spelled out through the join table. The
-- point is to exercise the same paths in SMTConditionVisitor as production traffic does — equality,
-- comparison, IN, LIKE, LOWER/UPPER, IS NULL, conjunction, disjunction, single and multi-way joins —
-- against the schema in sample-schema.json.
--
-- One query per line. Lines starting with -- are ignored.

SELECT account0_.ID AS ID1_0_ FROM ACCOUNT account0_
SELECT account0_.ID AS ID1_0_, account0_.NAME AS NAME2_0_ FROM ACCOUNT account0_ ORDER BY account0_.NAME ASC
SELECT account0_.ID AS ID1_0_, account0_.NAME AS NAME2_0_ FROM ACCOUNT account0_ WHERE account0_.ID = 42
SELECT account0_.ID AS ID1_0_ FROM ACCOUNT account0_ WHERE account0_.NAME = 'alice'
SELECT account0_.ID AS ID1_0_ FROM ACCOUNT account0_ WHERE account0_.ACTIVE = TRUE
SELECT account0_.ID AS ID1_0_ FROM ACCOUNT account0_ WHERE account0_.LEVEL > 3 AND account0_.LEVEL <= 10
SELECT account0_.ID AS ID1_0_ FROM ACCOUNT account0_ WHERE account0_.SCORE >= 4.5
SELECT account0_.ID AS ID1_0_ FROM ACCOUNT account0_ WHERE account0_.ID IN (1, 2, 3)
SELECT account0_.ID AS ID1_0_ FROM ACCOUNT account0_ WHERE account0_.NAME LIKE 'a%'
SELECT account0_.ID AS ID1_0_ FROM ACCOUNT account0_ WHERE LOWER(account0_.NAME) LIKE 'a%' ORDER BY account0_.NAME ASC
SELECT account0_.ID AS ID1_0_ FROM ACCOUNT account0_ WHERE UPPER(account0_.NAME) = 'ALICE'
SELECT account0_.ID AS ID1_0_ FROM ACCOUNT account0_ WHERE account0_.CREATED_AT > TIMESTAMP '2020-01-01 00:00:00'
SELECT account0_.ID AS ID1_0_ FROM ACCOUNT account0_ WHERE account0_.ACTIVE = TRUE AND account0_.LEVEL >= 5 ORDER BY account0_.SCORE DESC
SELECT project0_.ID AS ID1_1_ FROM PROJECT project0_ WHERE project0_.ACCOUNT_ID = 7
SELECT project0_.ID AS ID1_1_, project0_.TITLE AS TITLE3_1_ FROM PROJECT project0_ WHERE project0_.RANK < 50 OR project0_.TITLE = 'draft'
SELECT project0_.ID AS ID1_1_ FROM PROJECT project0_ INNER JOIN ACCOUNT account1_ ON project0_.ACCOUNT_ID = account1_.ID WHERE account1_.ACTIVE = TRUE
SELECT project0_.ID AS ID1_1_ FROM PROJECT project0_ INNER JOIN ACCOUNT account1_ ON project0_.ACCOUNT_ID = account1_.ID WHERE account1_.NAME = 'alice' AND project0_.RANK > 10
SELECT project0_.ID AS ID1_1_, account1_.NAME AS NAME2_0_ FROM PROJECT project0_ INNER JOIN ACCOUNT account1_ ON project0_.ACCOUNT_ID = account1_.ID ORDER BY account1_.NAME ASC
SELECT label0_.ID AS ID1_2_ FROM LABEL label0_ INNER JOIN PROJECT_LABEL projectlab1_ ON label0_.ID = projectlab1_.LABEL_ID WHERE projectlab1_.PROJECT_ID = 3
SELECT note0_.ID AS ID1_4_ FROM NOTE note0_ INNER JOIN PROJECT project1_ ON note0_.PROJECT_ID = project1_.ID WHERE project1_.ACCOUNT_ID = 7
SELECT note0_.ID AS ID1_4_, note0_.BODY AS BODY3_4_ FROM NOTE note0_ WHERE note0_.VALID_FROM <= TIMESTAMP '2026-01-01 00:00:00'
SELECT note0_.ID AS ID1_4_, note0_.BODY AS BODY3_4_ FROM NOTE note0_ WHERE note0_.PROJECT_ID = 3 AND note0_.VALID_TO IS NULL
-- The two below carry sub-second precision in the timestamp literal, which an ORM produces whenever
-- it compares a column against the current instant. The condition parser accepts only
-- 'yyyy-MM-dd HH:mm:ss', so it throws and the WHOLE WHERE clause is discarded.
SELECT note0_.ID AS ID1_4_ FROM NOTE note0_ WHERE note0_.VALID_TO > TIMESTAMP '2026-01-01 00:00:00.351'
SELECT note0_.ID AS ID1_4_ FROM NOTE note0_ WHERE note0_.VALID_TO IS NULL OR note0_.VALID_TO > TIMESTAMP '2026-01-01 00:00:00.351'
-- Derived tables in FROM/JOIN: an ORM emits these to resolve entity inheritance through a
-- UNION ALL. They carry no schema table of their own, so they contribute no alias; what matters is
-- that the real tables in the same query still get constrained instead of the query being discarded.
SELECT project0_.ID AS ID1_1_ FROM PROJECT project0_ LEFT OUTER JOIN (SELECT ID, PROJECT_ID FROM NOTE UNION ALL SELECT ID, PROJECT_ID FROM NOTE) note1_ ON project0_.ID = note1_.PROJECT_ID WHERE project0_.ID = 1
SELECT sub.ID AS ID1_0_ FROM (SELECT ID FROM ACCOUNT UNION ALL SELECT ID FROM ACCOUNT) sub
