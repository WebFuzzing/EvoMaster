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

### Further query and handler validation

Another 23 semantic fixtures cover these additional discrepancies:

| Area | Confirmed discrepancy |
| --- | --- |
| Empty document equality | Implicit `{a:{}}` throws; explicit `$eq` works. |
| Literal document fields | Recursive removal of `$comments` changes literal equality, producing both false positives and false negatives. |
| High bit positions | Numeric position 64 is rejected instead of using sign extension. |
| BSON extrema | MinKey equality and comparison of an ordinary number with MaxKey fail. |
| Decimal bitwise values | A nonintegral Decimal128 rounded to an integer through double incorrectly matches. |
| `$size` operand types | Valid integral Double, Int64, and Decimal128 operands throw. |
| Array ordering | Ordered array comparisons reject matching arrays or throw for nested arrays; inclusive empty-array comparison also fails. |

All 128 live MongoDB 7.0.41 oracle checks passed: 127 semantic fixtures plus
the detailed-line check. EvoMaster failed 17 of the 23 new fixtures, bringing
the semantic total to 92 failures and 35 passing controls. The selected unit
run reported 414 tests, 92 failures, no errors, and three skips; all 284 existing
active tests still passed. The unchanged performance test was not repeated.

Fixture snapshots now use canonical Extended JSON to preserve small Int64
values. The oracle uses an insert command so the server can validate stored
dollar-prefixed keys, which the bundled older Java driver's `insertOne` rejects
locally. It checks the insert result before testing the query.

`MongoHandlerRegressionIT` adds two separately verified live regressions:

- Evaluating a 250-document collection leaves an idle server cursor. A fully
  consumed ordinary find is the passing control. The test checks only its unique
  collection namespace and closes leaked cursors during cleanup.
- An unfiltered `find()` recorded with a null query is ignored, whereas
  `find({})` is evaluated and reports the empty collection for data generation.

Both handler tests fail as intended on the unchanged implementation, with no
test errors. These are pre-existing issues, not additions in PR #1785. Run them
separately from the passing oracle:

```sh
mvn -pl client-java/controller -am \
  -Dit.test=MongoHandlerRegressionIT \
  -Dfailsafe.failIfNoSpecifiedTests=false \
  -Devomaster.mongo.uri=mongodb://127.0.0.1:27885 \
  test-compile failsafe:integration-test failsafe:verify
```

Omit the URI to start a test container. An external server must permit the
`$currentOp` inspection used by the cursor test. Tests create and clean up their
own unique database and collection names. Production code remains unchanged.

### Additional geometry, numeric, and driver-filter findings

The next pass added 27 semantic fixtures and two handler cases, all checked
against live MongoDB 7.0.41.

| Finding | Concrete reproduction and impact |
| --- | --- |
| Flat regions accept unsupported stored geometry kinds | `$geoWithin:{$box:[[0,0],[10,10]]}` incorrectly matches a stored GeoJSON LineString `[[4,4],[6,6]]`. The same issue affects `$polygon` and `$center`, with LineString, Polygon, and MultiPoint fixtures. MongoDB rejects all nine matches; Point and `$centerSphere` controls match. |
| Stored regexes are executed by `$ne` | For `{a:[/foo/]}`, `$ne:'foobar'` incorrectly returns false and `$ne:null` throws. The stored regex is a value, not a query predicate. `$eq` and `$nin` controls behave correctly. |
| Infinity equality throws | Both positive and negative infinity fail equality against themselves with an invalid Truthness exception. MongoDB matches them. |
| Inclusive NaN comparison fails | Both `$lte:NaN` and `$gte:NaN` must match a stored NaN; EvoMaster returns false. Equality and strict-comparison controls pass. |
| Tiny finite differences throw | Comparing stored `1e-17` with `$eq:0` throws instead of returning false because the truthness rounds to one on both sides. This extends the earlier geometric residual issue to ordinary numeric queries. |
| Tiny segment intersection underflows | A line `[[0,0],[1e-170,0]]` intersects its midpoint `[5e-171,0]` in MongoDB but not EvoMaster. Squaring the segment length underflows to zero and the projection becomes NaN. This is an extreme-scale, lower-priority case. |
| Standard driver filters crash the handler | `Filters.eq("value",1)` and an equivalent `BsonDocument` both match through the Java driver, but their recorded commands throw NPE and IllegalArgumentException respectively. The equivalent `Document` filter succeeds. |

Relevant source locations in the reviewed PR:

- `GeoWithinSelector.java:80` discards the original shape kind when constructing
  the operation; the new evaluator consequently applies polygon containment to
  flat shapes without the stored-geometry restriction.
- `MongoHeuristicsCalculator.java:298` treats the stored array as query candidates
  during `$ne`, allowing stored regexes to execute.
- `MongoHeuristicsCalculatorHelper.java:291` handles NaN comparisons but rejects
  inclusive ordering; the adjacent numeric conversion/distance path also
  exposes the infinity and near-zero failures.
- `GeoJsonGeometryIntersection.java:260` divides by the squared segment length
  without handling underflow.
- `MongoCollectionClassReplacement` records the original `Bson` filter, while
  `BsonHelper.java:108` only recognizes `org.bson.Document`.

The flat-region and tiny-segment findings concern PR #1785's new geometry
evaluation. The regex, numeric, and driver-filter problems predate this PR.

All 155 oracle checks passed (154 semantic fixtures plus the detailed-line
check). All four handler regressions failed as expected, with no test errors;
the two new cases first assert successful live queries and Document controls.
The semantic suite now has 109 intentional failures and 45 passing controls:
17 of the 27 new cases fail. All 284 existing active calculator and geometry
tests still pass. The selected unit run reported 441 tests, 109 failures, no
errors, and three skips. The unchanged performance test was not repeated.

### BSON, proximity, and instrumentation review

A further parallel pass added 30 semantic fixtures. All 185 live MongoDB 7.0.41
oracle checks passed (184 semantic fixtures plus the detailed-line check).
Seventeen of the new semantic cases fail on EvoMaster: the semantic total is
126 intentional failures and 58 passing controls. All 284 existing active
calculator and geometry tests pass. The selected unit run reports 471 tests,
126 failures, no errors, and three skips. The performance test was not repeated.

| Finding | Reproduction and cause |
| --- | --- |
| ObjectId and Binary range queries throw | `$gt`, `$lt`, and inclusive ObjectId fixtures and Binary ordering fixtures are accepted by MongoDB. `MongoHeuristicsCalculatorHelper.compareBinaryData` throws for all ordering operators. This affects ordinary ObjectId ranges, including `_id` pagination. |
| BSON value equality is missing | A stored BSON Symbol fails equality with the corresponding string; stored JavaScript Code and CodeWithScope fail equality with themselves. The helper falls through to incompatible-type handling. These are stored-value comparisons; no JavaScript is executed. |
| Proximity rejects non-point GeoJSON | Both `$near` and `$nearSphere` reject stored LineString, MultiPoint, and Polygon values that match a one-meter query at `[0,0]` in MongoDB. The fixtures use `2dsphere` indexes. `evaluateDistanceBetweenPoints` only accepts stored Points. Point and distant-line controls pass. |
| Antipodal proximity loses valid matches | Query point `[0,8]`, stored point `[180,-8]`, and maximum distance 21,000,000 meters match in MongoDB for both proximity operators but fail in EvoMaster. `MongoUtils.java:67–73` permits the Haversine intermediate to round above one, producing NaN. A 20,000,000-meter cutoff correctly excludes the point and provides the negative controls. |
| Explicit result class records the wrong schema | `find({}, Person.class)` works in MongoDB but instrumentation records the collection's generic `Document` schema. A collection typed as Person correctly records its fields. `MongoOperationClassReplacement.java:19` derives the schema from the collection and ignores the result-class argument. |
| Projection causes a false execution failure | A POJO has integer `age`, and the stored document has string `age`. Excluding `age` with a projection makes the actual query succeed. However, `MongoCollectionClassReplacement.java:64` eagerly opens the cursor before the projection is applied, catches the preliminary decode failure, and records the successful query as failed. |

These findings predate PR #1785. The Symbol/JavaScript BSON types are
less common than the ObjectId and instrumentation paths. BSON date values at
the signed 64-bit extremes were also checked and behave correctly.

`MongoInstrumentationRegressionIT` invokes the actual method replacements and
verifies successful live-driver controls before checking the recorded schema
and execution flag. Both tests fail only their intended assertions, with no
test errors. Each test resets tracer state and cleans its isolated collections.

```sh
mvn -pl client-java/controller -am \
  -Dit.test=MongoInstrumentationRegressionIT \
  -Dfailsafe.failIfNoSpecifiedTests=false \
  -Devomaster.mongo.uri=mongodb://127.0.0.1:27885 \
  test-compile failsafe:integration-test failsafe:verify
```

The query oracle and semantic regression tests now also consume
`MongoBsonRegressionCases`. Production code remains unchanged.

The same pass confirmed a cross-database schema collision. Two different
databases have a collection with the same name: one stores customers with
`customerEmail`, the other invoices with `invoiceTotal`. Their empty live
queries and per-command fallback schemas are correct. After registering both
collection schemas, the handler reports the invoice schema for both databases.
`MongoHandler.java:102` keys registrations by collection name alone, and
`MongoCollectionSchema` carries no database identifier. This can cause document
generation to insert the wrong fields into the customer database. The fix needs
schema provenance, not just a different lookup expression.

`MongoHandlerRegressionIT` includes this new reproduction, with unique database
names, passing fallback controls, and cleanup. Its five regressions fail as
intended with no test errors, including the four previously reported failures.
Together with the two new instrumentation tests, this pass adds three live
integration regressions beyond its 30 semantic fixtures. All new findings in
this pass are pre-existing issues rather than changes introduced by PR #1785.
