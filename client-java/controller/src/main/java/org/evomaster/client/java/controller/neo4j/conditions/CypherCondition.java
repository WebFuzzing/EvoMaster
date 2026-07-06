package org.evomaster.client.java.controller.neo4j.conditions;

/**
 * Represents a condition extracted from a Cypher query.
 * Conditions can come from:
 * - Node labels: (n:Person) → LabelCondition
 * - Node properties: (n {name: "Alice"}) → PropertyCondition
 * - Edge types: -[:KNOWS]-> → TypeCondition
 * - Edge properties: -[:KNOWS {since: 2020}]-> → PropertyCondition
 * - WHERE clause: WHERE n.age > 25 → ComparisonCondition
 * - Logical operators: AND, OR, NOT
 */
public interface CypherCondition {
}
