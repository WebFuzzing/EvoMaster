# MongoDB query regression tests

This test-only branch starts at PR #1785's `mongo_geometry` head,
`b22ec953ca00823e8bf33174c4d07e89551d58a5`. Production code is unchanged, so the
regression suite is deliberately expected to fail until the defects are fixed.

`MongoGeometryRegressionCases` and `MongoQueryRegressionCases` supply the same
queries, documents, and expected matches to two independent suites:

- `MongoQueryOracleIT` verifies every expectation against a live MongoDB.
- `MongoQueryRegressionTest` checks EvoMaster's public collection-distance
  calculation, including exceptions and the validity of the resulting score.

The oracle uses a unique temporary database and removes it afterwards. It starts
`mongo:7.0` with Testcontainers by default. Set `-Devomaster.mongo.uri=...` to use
an already running server instead. Server errors fail the test; they are never
interpreted as a nonmatching query. The oracle must pass before interpreting
regression failures as EvoMaster defects.

## Run the live oracle first

From the repository root, with Docker running:

```sh
mvn -pl client-java/controller -am \
  -Dit.test=MongoQueryOracleIT \
  -Dfailsafe.failIfNoSpecifiedTests=false \
  test-compile failsafe:integration-test failsafe:verify
```

For an existing MongoDB server, add, for example,
`-Devomaster.mongo.uri=mongodb://127.0.0.1:27885`.

## Run the failing regressions

```sh
mvn -pl client-java/controller -am \
  -Dtest=MongoQueryRegressionTest,GeoJsonGeometryPerformanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test-compile surefire:test
```

The reactor (`-am`) is needed to avoid accidentally compiling against stale
installed snapshots. The parent POM retries failing tests; this can also repeat
the performance case.

## Geometry defects

| Case | Expected behavior, verified on MongoDB 7.0.41 |
| --- | --- |
| Containment across holes or disconnected polygons | A line or filled polygon must be wholly contained, including its edges/interior. Disconnected points may occupy separate polygons. |
| Legacy stored coordinates | Matching coordinate arrays are accepted by `$box`, `$polygon`, `$center`, and `$centerSphere`. |
| Optional altitude | Supported GeoJSON queries and stored geometries can contain a third coordinate. |
| Exact line endpoint | A point equal to the line's endpoint matches without throwing during distance calculation. |
| Arrays of GeoJSON objects | Either predicate matches if any immediate array element matches; `$not` inverts that result. Nested arrays are not recursively flattened. |
| Explicit default CRS | `CRS84` and `EPSG:4326` declarations are accepted. |
| Zero-radius circles | The center matches; other points do not. |
| Zero-width and point boxes | Points on the resulting segment or point match; other points do not. |
| Signed-zero ring closure | `0.0` and `-0.0` describe the same closing coordinate. |
| Ordered documents in legacy queries | Both coordinate pairs and shape containers can use ordered BSON documents. This was found during the follow-up review. |

`GeoJsonGeometryPerformanceTest` separately checks the previously reported
LinkedList traversal issue. It compares two disjoint 2,000-position lines after
a small warmup, with a synchronous two-second budget (no background worker is
left running on failure). The live oracle checks the same disjoint fixture and
an identical-line positive control. The time budget is an EvoMaster performance
requirement, not a MongoDB semantic assertion. An array-backed version of the
same traversal measured about 58 ms locally; the PR implementation took about
20 seconds. This timing test remains sensitive to machine load.

The fixtures also include passing controls. In particular, a mixed inside/outside
array can match `$geoWithin`, whereas a MultiPoint or GeometryCollection with the
same contents does not. MongoDB 7 also accepts a GeoJSON point in the tested
legacy `$box` query, so that behavior is a positive control rather than a defect.

## Initial geometry validation on the unchanged PR implementation

On 2026-09-25, using MongoDB 7.0.41 and the repository's Maven reactor:

- All 58 oracle checks passed (57 semantic fixtures plus the detailed-line check).
- Of the 57 semantic regression cases, 39 failed and 18 controls passed.
- The performance regression failed its two-second budget, consistently across
  the parent POM's retries.
- All 284 existing active calculator and geometry tests passed; three existing
  tests remained skipped.

The combined unit run reported 345 tests, 40 failures, no errors, and three skips.
These failures are intentional evidence of the unfixed defects, not a passing
implementation. Full-project tests outside the selected modules were not run.


## Other MongoDB findings

These issues are present in the reviewed repository but were not introduced by
PR #1785. `MongoQueryRegressionCases` adds their reproductions and passing
controls. The same live oracle validates them. It now uses `find` rather than
`countDocuments`, because the aggregation underlying `countDocuments` disallows
`$near`. Proximity fixtures create the required `2d` index; each case drops its
isolated collection first so index requirements cannot leak between cases.

| Area | Confirmed discrepancy |
| --- | --- |
| Logical composition | A leading `$or` can discard sibling field predicates; putting a sibling first can instead cause a null-operation exception. |
| `$elemMatch` operator dispatch | Scalar `$type` and `$exists` conditions throw instead of evaluating. |
| `$elemMatch` nested arrays | Scalar comparisons unwrap a nested array that should remain one element; document-style matching rejects valid array elements and numeric indices. |
| `$ne` with array operand | A matching nested array is not excluded, unlike the corresponding `$eq`. |
| `$type` over arrays | Element types are ignored, so an array of integers does not match `$type:"int"`. |
| Binary bitwise values | `$bitsAllSet` rejects supported BSON Binary values. |
| Numeric conversions | NaN becomes zero for `$mod`; double `2^63` is accepted as a signed-long bitwise operand and saturated. |
| Numeric precision | Distinct Int64 values above `2^53`, and distinct timestamp increments, can compare equal after conversion to double. |
| Timestamp ordering | Unsigned timestamp seconds are compared as part of a signed packed long. |
| Embedded documents | Strict ordering and inequality incorrectly require corresponding field names to satisfy the same comparison; Java equality can stop comparison before later differing fields or extra fields. |
| BSON binary equality | Identical bytes with different subtypes incorrectly compare equal. |
| Regex operators | Mixed arrays are rejected wholesale; `$in` ignores flags, stringifies numbers, and throws on null; `$all` treats regexes as equality operands. |
| Regex semantics and stored regex values | Java newline/extended-mode behavior differs from MongoDB; stored regex flags can be ignored or a matching stored regex rejected. |
| String ordering | Java UTF-16 order differs from MongoDB's default UTF-8 order for supplementary characters. |
| Legacy proximity | `$near` accepts a legacy query but rejects the legacy stored coordinate pair, even at zero distance. |

### Expanded validation

The expanded suite added 47 cases. All 105 live MongoDB 7.0.41 checks passed:
104 semantic fixtures and the detailed-line check. EvoMaster failed 36 of the
new cases, bringing the semantic regression total to 75 failures and 29 passing
controls. All 284 existing active calculator and geometry tests still passed;
three existing tests remained skipped. The selected unit run reported 391 tests,
75 failures, no errors, and three skips.

The unchanged performance test was not repeated in this pass; its earlier
failure remains documented above. Production code remains unchanged. The general
suite names are now `MongoQueryRegressionTest` and `MongoQueryOracleIT`, covering
both fixture providers.
