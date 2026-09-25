# MongoDB claim audit — 2026-09-25

Rechecked the accumulated claims on test branch commit `d22b0c09bd`, against
the unchanged PR #1785 production code at
`b22ec953ca00823e8bf33174c4d07e89551d58a5`. Attribution was checked against
PR base `bcb6991ee7902cf354fe0d35b074b767f65fa973`. Three agents independently
audited geometry, query/BSON semantics, and instrumentation; the parent reran
the complete selected suites against a fresh MongoDB 7.0.41 container.

No semantic fixture or integration reproduction needed retraction. Several
claims needed narrower wording, recorded below. This is evidence for the
specific fixtures and server version, not exhaustive MongoDB compatibility.

## Fresh results

| Check | Result |
| --- | --- |
| Live oracle | 185 passed: 184 semantic fixtures plus one detailed-line control test |
| Semantic regressions | 126 failing cases, 58 passing controls |
| Existing calculator/geometry tests | 284 active tests passed; three skipped |
| Geometry timing reproduction | Failed on all three attempts: approximately 13.17, 13.46, and 13.14 seconds |
| Selected unit total | 472 tests, 127 failures, zero errors, three skipped |
| Handler regressions | Five intended failures, zero errors |
| Instrumentation regressions | Two intended failures, zero errors |
| Combined live integration total | 192 tests, seven intended failures, zero errors |

The Maven parent retries failures. Counts above count distinct cases, not retry
attempts. A failing case is not a distinct bug: related variants often share a
root cause. The selected unit total includes the timing test; the 185 passing
oracle checks do not mean the implementation passes those semantic checks.

The oracle checks write success and real `find` results, preserves BSON types
with canonical Extended JSON, and drops indexes between scenarios. Legacy
proximity uses `2d`, GeoJSON proximity uses `2dsphere`. MongoDB exceptions are
test errors, never interpreted as false matches. Production changes from the
PR head remain absent; only tests and documentation have been added.

## Claim-by-claim disposition

The exact queries, documents, and positive/negative controls are in the three
fixture providers linked below. Every row is supported by the fresh oracle and
the corresponding evaluator failures, or by the identified integration test.

| Claim group | Audit result and scope |
| --- | --- |
| Containment across holes, polygon interiors, disconnected components | Confirmed for the fixtures; PR-added geometry evaluator checks vertices insufficiently. |
| Legacy stored coordinate pairs | Confirmed for the four tested containment shapes; PR-added containment evaluation rejects the representation. |
| Optional altitude | Confirmed for added LineString/Polygon parsing. The older Point parser's restriction predates the PR. |
| Exact line endpoint | Confirmed exception caused by a nonzero floating-point residual; PR geometry calculation exposes it. |
| Arrays of GeoJSON values | Confirmed immediate-element matching failures. Controls distinguish arrays from MultiPoint/GeometryCollection and nested arrays. |
| Explicit CRS84/EPSG:4326 | Confirmed for the Polygon query fixtures; do not generalize to arbitrary CRS names. |
| Zero-radius circles; zero-width/point boxes | Confirmed valid MongoDB queries rejected by new parsers. |
| Signed-zero ring closure | Confirmed in the two tested longitude/latitude variants. |
| Ordered documents in legacy shape queries | Confirmed for coordinate pairs and outer shape containers in the tested key order. |
| Flat shapes matching non-point GeoJSON | Confirmed nine false positives; Point and spherical-region controls pass. |
| Tiny-segment midpoint | Confirmed underflow at the extreme `1e-170` scale; lower practical priority. |
| LinkedList traversal cost | Confirmed source-level cubic cost and repeated local slowdown. Two seconds is a review threshold, not a product requirement. |
| Logical operators with sibling filters | Confirmed ignored condition or exception depending on query order; pre-existing. |
| `$elemMatch` dispatch and nested-array behavior | Confirmed scalar type/exists, nested scalar comparisons, object-form array handling and numeric-index cases; pre-existing. |
| `$ne` nested arrays and stored regexes | Confirmed distinct matching and predicate/value-role problems; pre-existing. |
| `$type` over arrays | Confirmed missing element-type inspection; pre-existing. |
| Bitwise Binary values, high positions, numeric range and Decimal128 integrality | Confirmed separately for the supplied operands; pre-existing. |
| NaN modulo, Int64 precision, timestamp precision/ordering | Confirmed; pre-existing conversion/comparison paths. |
| Embedded-document ordering and equality | Confirmed; field-name comparison and Java-vs-BSON equality explain the supplied failures. |
| Binary subtype equality | Confirmed identical bytes with different subtypes compare equal incorrectly. |
| Regex operators, options, mixed types, stored regex equality | Confirmed for the supplied `$regex`, `$in`, `$all`, and equality fixtures. |
| Regex engine and string ordering differences | Confirmed tested CR/extended-mode and UTF-8-vs-UTF-16 cases; not a claim about every regex or collation. |
| Legacy `$near` stored coordinates | Confirmed with the required index; pre-existing. |
| Empty document equality and literal `$comments` removal | Confirmed; `$comments` is a literal stored key in these equality operands. |
| MinKey/MaxKey comparisons | Confirmed tested equality and extrema cases; pre-existing. |
| `$size` operand representations | Confirmed integral Double/Int64/Decimal128 failures; Int32 and type-preservation controls pass. |
| Array ordering | Confirmed ordinary, nested, and empty-array comparisons; pre-existing. |
| Infinity, inclusive NaN and tiny finite comparisons | Confirmed; the near-zero case shares the earlier Truthness rounding failure family. |
| ObjectId/Binary range operators | Confirmed exceptions. `_id` pagination impact is inferred from ObjectId ordering, not exercised as a paginated application flow. |
| Symbol, Code, CodeWithScope equality | Confirmed stored-value comparisons. No server-side JavaScript execution is involved. |
| Non-point GeoJSON proximity | Confirmed six matches rejected by both proximity operators; pre-existing Point-only evaluation. |
| Antipodal proximity | Confirmed both operators; current compiled code produces Haversine intermediate `1.0000000000000002` and NaN distance. Pre-existing. |
| Handler cursor leakage | Confirmed a namespace-scoped idle cursor after evaluation; the ordinary consumed-find control closes it. |
| Unfiltered `find()` tracking | Confirmed recorded null query is discarded, unlike explicit `{}`. |
| Driver `Filters.eq` and `BsonDocument` | Confirmed live driver success followed by handler exceptions; equivalent Document control succeeds. |
| Explicit result-class metadata | Confirmed loss of Person fields when the collection type is generic Document. Not a rule that every result DTO should override a concrete storage schema. |
| Projection execution flag | Confirmed actual replacement-returned query succeeds after projection, but recorded execution is false. |
| Cross-database schema collision | Confirmed reported schema is overwritten for identical collection names. Registration order determines the winning schema. See evidence limits below. |

Geometry support added by PR #1785 did not exist at the base, so these are
defects in the newly supported behavior, not necessarily previously passing
queries made to fail. Non-geometry query failures, proximity failures, and all
seven integration failures predate the PR. Existing planar/spherical and
circle-approximation limitations were not counted as newly discovered bugs.

## Evidence limits and corrections

- The two-second timing limit was chosen for this review. The earlier wording
  calling it an EvoMaster requirement was incorrect and has been corrected.
  Historical 58 ms array-backed and roughly 20 s original timings are local
  observations, not portable bounds. The fresh original-code timing is about
  13 seconds; source inspection independently confirms repeated indexed access
  to LinkedList inside nested loops.
- The schema-collision test uses real live queries but synthesized registration
  events. Source review confirms the Spring replacements emit the same
  collection-name-only metadata; it is not a full two-Spring-template test.
- The result-class test concerns generic Document collections requesting a
  concrete, unprojected POJO. The best schema for arbitrary projected result
  DTOs needs a separate design decision.
- Wrong/missing schemas flow to insertion-gene construction in the core code.
  Effects on generated fields are a source-supported inference. These tests
  do not run the generator through an application endpoint to prove a lost
  branch or failed generated insertion.
- No broader application or full-project test suite was run. Runtime evidence
  applies to MongoDB 7.0.41 with the bundled Java driver and these fixtures.

## Reproductions

- [Geometry fixtures](../client-java/controller/src/test/java/org/evomaster/client/java/controller/mongo/MongoGeometryRegressionCases.java)
- [Query/proximity fixtures](../client-java/controller/src/test/java/org/evomaster/client/java/controller/mongo/MongoQueryRegressionCases.java)
- [BSON fixtures](../client-java/controller/src/test/java/org/evomaster/client/java/controller/mongo/MongoBsonRegressionCases.java)
- [Handler tests](../client-java/controller/src/test/java/org/evomaster/client/java/controller/mongo/MongoHandlerRegressionIT.java)
- [Instrumentation tests](../client-java/controller/src/test/java/org/evomaster/client/java/controller/mongo/MongoInstrumentationRegressionIT.java)
- [Timing test](../client-java/controller/src/test/java/org/evomaster/client/java/controller/mongo/geometry/GeoJsonGeometryPerformanceTest.java)
- [Run commands and original findings](mongo-geometry-regressions.md)
