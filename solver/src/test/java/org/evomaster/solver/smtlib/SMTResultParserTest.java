package org.evomaster.solver.smtlib;

import org.evomaster.solver.smtlib.value.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SMTResultParserTest {
    @Test
    public void testParseEmptyResponse() {
        String response = "";
        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseMalformedResponse() {
        String response = "sat\n(id_1 2)";
        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseSimpleIntValue() {
        String response = "sat\n((id_1 2))";
        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertEquals(1, result.size());
        assertTrue(result.get("id_1") instanceof LongValue);
        assertEquals(2, ((LongValue) result.get("id_1")).getValue());
    }

    @Test
    public void testParseSimpleStringValue() {
        String response = "sat\n((name_1 \"example\"))";
        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertEquals(1, result.size());
        assertTrue(result.get("name_1") instanceof StringValue);
        assertEquals("example", ((StringValue) result.get("name_1")).getValue());
    }

    @Test
    public void testParseSimpleRealValue() {
        String response = "sat\n((pi_1 3.14))";
        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertEquals(1, result.size());
        assertTrue(result.get("pi_1") instanceof RealValue);
        assertEquals(3.14, ((RealValue) result.get("pi_1")).getValue());
    }

    @Test
    public void testParseNegativeValue() {
        String response = "sat\n" +
                "((y 0))\n" +
                "((x (- 4)))";
        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertEquals(2, result.size());
        assertTrue(result.get("y") instanceof LongValue);
        assertEquals(0, ((LongValue) result.get("y")).getValue());
        assertTrue(result.get("x") instanceof LongValue);
        assertEquals(-4, ((LongValue) result.get("x")).getValue());
    }

    @Test
    public void testParseComposedType() {
        String response = "sat\n((users1 (id-document-name-age-points 4 2 \"agus\" 31 7)))";

        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);

        assertEquals(1, result.size());
        assertTrue(result.get("users1") instanceof StructValue);
        StructValue users1 = (StructValue) result.get("users1");
        assertEquals(5, users1.getFields().size());
        assertTrue(users1.getField("ID") instanceof LongValue);
        assertEquals(4, ((LongValue) users1.getField("ID")).getValue());
        assertTrue(users1.getField("DOCUMENT") instanceof LongValue);
        assertEquals(2, ((LongValue) users1.getField("DOCUMENT")).getValue());
        assertTrue(users1.getField("NAME") instanceof StringValue);
        assertEquals("agus", ((StringValue) users1.getField("NAME")).getValue());
        assertTrue(users1.getField("AGE") instanceof LongValue);
        assertEquals(31, ((LongValue) users1.getField("AGE")).getValue());
        assertTrue(users1.getField("POINTS") instanceof LongValue);
        assertEquals(7, ((LongValue) users1.getField("POINTS")).getValue());
    }

    @Test
    public void testParseMultipleEntries() {
        String response = "sat\n" +
                "((products1 (price-min_price-stock-user_id 5 501 8 4)))\n" +
                "((products2 (price-min_price-stock-user_id 9 21739 8 6)))\n" +
                "((users1 (id-document-name-age-points 4 2 \"agus\" 31 7)))\n" +
                "((users2 (id-document-name-age-points 6 3 \"agus\" 91 7)))\n";

        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertEquals(4, result.size());

        assertTrue(result.get("products1") instanceof StructValue);
        StructValue products1 = (StructValue) result.get("products1");
        assertEquals(4, products1.getFields().size());
        assertTrue(products1.getField("PRICE") instanceof LongValue);
        assertEquals(5, ((LongValue) products1.getField("PRICE")).getValue());
        assertTrue(products1.getField("MIN_PRICE") instanceof LongValue);
        assertEquals(501, ((LongValue) products1.getField("MIN_PRICE")).getValue());
        assertTrue(products1.getField("STOCK") instanceof LongValue);
        assertEquals(8, ((LongValue) products1.getField("STOCK")).getValue());
        assertTrue(products1.getField("USER_ID") instanceof LongValue);
        assertEquals(4, ((LongValue) products1.getField("USER_ID")).getValue());

        assertTrue(result.get("products2") instanceof StructValue);
        StructValue products2 = (StructValue) result.get("products2");
        assertEquals(4, products2.getFields().size());
        assertTrue(products2.getField("PRICE") instanceof LongValue);
        assertEquals(9, ((LongValue) products2.getField("PRICE")).getValue());
        assertTrue(products2.getField("MIN_PRICE") instanceof LongValue);
        assertEquals(21739, ((LongValue) products2.getField("MIN_PRICE")).getValue());
        assertTrue(products2.getField("STOCK") instanceof LongValue);
        assertEquals(8, ((LongValue) products2.getField("STOCK")).getValue());
        assertTrue(products2.getField("USER_ID") instanceof LongValue);
        assertEquals(6, ((LongValue) products2.getField("USER_ID")).getValue());

        assertTrue(result.get("users1") instanceof StructValue);
        StructValue users1 = (StructValue) result.get("users1");
        assertEquals(5, users1.getFields().size());
        assertTrue(users1.getField("ID") instanceof LongValue);
        assertEquals(4, ((LongValue) users1.getField("ID")).getValue());
        assertTrue(users1.getField("DOCUMENT") instanceof LongValue);
        assertEquals(2, ((LongValue) users1.getField("DOCUMENT")).getValue());
        assertTrue(users1.getField("NAME") instanceof StringValue);
        assertEquals("agus", ((StringValue) users1.getField("NAME")).getValue());
        assertTrue(users1.getField("AGE") instanceof LongValue);
        assertEquals(31, ((LongValue) users1.getField("AGE")).getValue());
        assertTrue(users1.getField("POINTS") instanceof LongValue);
        assertEquals(7, ((LongValue) users1.getField("POINTS")).getValue());

        assertTrue(result.get("users2") instanceof StructValue);
        StructValue users2 = (StructValue) result.get("users2");
        assertEquals(5, users2.getFields().size());
        assertTrue(users2.getField("ID") instanceof LongValue);
        assertEquals(6, ((LongValue) users2.getField("ID")).getValue());
        assertTrue(users2.getField("DOCUMENT") instanceof LongValue);
        assertEquals(3, ((LongValue) users2.getField("DOCUMENT")).getValue());
        assertTrue(users2.getField("NAME") instanceof StringValue);
        assertEquals("agus", ((StringValue) users2.getField("NAME")).getValue());
        assertTrue(users2.getField("AGE") instanceof LongValue);
        assertEquals(91, ((LongValue) users2.getField("AGE")).getValue());
        assertTrue(users2.getField("POINTS") instanceof LongValue);
        assertEquals(7, ((LongValue) users2.getField("POINTS")).getValue());
    }

    @Test
    public void testParseCaseStudyProblem() {
        String response = "sat\n" +
                "((contributor1 (id-organization_id-snapshot_date-name-organization_name-organizational_commits_count-organizational_projects_count-personal_commits_count-personal_projects_count-url\n" +
                "  3\n" +
                "  4\n" +
                "  5\n" +
                "  \"!0!\"\n" +
                "  \"!0!\"\n" +
                "  6\n" +
                "  7\n" +
                "  8\n" +
                "  9\n" +
                "  \"!0!\")))\n" +
                "((contributor2 (id-organization_id-snapshot_date-name-organization_name-organizational_commits_count-organizational_projects_count-personal_commits_count-personal_projects_count-url\n" +
                "  10\n" +
                "  11\n" +
                "  12\n" +
                "  \"!0!\"\n" +
                "  \"!0!\"\n" +
                "  13\n" +
                "  14\n" +
                "  15\n" +
                "  16\n" +
                "  \"!0!\")))\n" +
                "((language_list1 (project_id-language 17 \"\")))\n" +
                "((language_list2 (project_id-language 17 \"\")))\n" +
                "((maintainers1 (project_id-maintainer 17 \"\")))\n" +
                "((maintainers2 (project_id-maintainer 17 \"\")))\n" +
                "((project1 (id-commits_count-contributors_count-description-forks_count-git_hub_project_id-last_pushed-name-organization_name-primary_language-score-snapshot_date-stars_count-url-title-image-external_contributors_count\n" +
                "  17\n" +
                "  18\n" +
                "  19\n" +
                "  \"!0!\"\n" +
                "  20\n" +
                "  21\n" +
                "  \"!0!\"\n" +
                "  \"!0!\"\n" +
                "  \"!0!\"\n" +
                "  \"!0!\"\n" +
                "  22\n" +
                "  1\n" +
                "  23\n" +
                "  \"!0!\"\n" +
                "  \"!0!\"\n" +
                "  \"!0!\"\n" +
                "  24)))\n" +
                "((project2 (id-commits_count-contributors_count-description-forks_count-git_hub_project_id-last_pushed-name-organization_name-primary_language-score-snapshot_date-stars_count-url-title-image-external_contributors_count\n" +
                "  25\n" +
                "  26\n" +
                "  27\n" +
                "  \"!0!\"\n" +
                "  28\n" +
                "  29\n" +
                "  \"!0!\"\n" +
                "  \"!0!\"\n" +
                "  \"!0!\"\n" +
                "  \"!0!\"\n" +
                "  30\n" +
                "  2\n" +
                "  31\n" +
                "  \"!0!\"\n" +
                "  \"!0!\"\n" +
                "  \"!0!\"\n" +
                "  32)))\n" +
                "((statistics1 (id-snapshot_date-all_contributors_count-all_forks_count-all_size_count-all_stars_count-members_count-organization_name-private_project_count-program_languages_count-public_project_count-tags_count-teams_count-external_contributors_count\n" +
                "  33\n" +
                "  34\n" +
                "  35\n" +
                "  36\n" +
                "  37\n" +
                "  38\n" +
                "  39\n" +
                "  \"!0!\"\n" +
                "  40\n" +
                "  41\n" +
                "  42\n" +
                "  43\n" +
                "  44\n" +
                "  45)))\n" +
                "((statistics2 (id-snapshot_date-all_contributors_count-all_forks_count-all_size_count-all_stars_count-members_count-organization_name-private_project_count-program_languages_count-public_project_count-tags_count-teams_count-external_contributors_count\n" +
                "  46\n" +
                "  47\n" +
                "  48\n" +
                "  49\n" +
                "  50\n" +
                "  51\n" +
                "  52\n" +
                "  \"!0!\"\n" +
                "  53\n" +
                "  54\n" +
                "  55\n" +
                "  56\n" +
                "  57\n" +
                "  58)))\n";
        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertEquals(10, result.size());

        assertTrue(result.get("contributor1") instanceof StructValue);
        StructValue contributor1 = (StructValue) result.get("contributor1");
        assertEquals(10, contributor1.getFields().size());

        assertTrue(contributor1.getField("ID") instanceof LongValue);
        assertEquals(3, ((LongValue) contributor1.getField("ID")).getValue());

        assertTrue(contributor1.getField("ORGANIZATION_ID") instanceof LongValue);
        assertEquals(4, ((LongValue) contributor1.getField("ORGANIZATION_ID")).getValue());

        assertTrue(contributor1.getField("SNAPSHOT_DATE") instanceof LongValue);
        assertEquals(5, ((LongValue) contributor1.getField("SNAPSHOT_DATE")).getValue());

        assertTrue(contributor1.getField("NAME") instanceof StringValue);
        assertEquals("!0!", ((StringValue) contributor1.getField("NAME")).getValue());

        assertTrue(contributor1.getField("ORGANIZATION_NAME") instanceof StringValue);
        assertEquals("!0!", ((StringValue) contributor1.getField("ORGANIZATION_NAME")).getValue());

        assertTrue(contributor1.getField("ORGANIZATIONAL_COMMITS_COUNT") instanceof LongValue);
        assertEquals(6, ((LongValue) contributor1.getField("ORGANIZATIONAL_COMMITS_COUNT")).getValue());

        assertTrue(contributor1.getField("ORGANIZATIONAL_PROJECTS_COUNT") instanceof LongValue);
        assertEquals(7, ((LongValue) contributor1.getField("ORGANIZATIONAL_PROJECTS_COUNT")).getValue());

        assertTrue(contributor1.getField("PERSONAL_COMMITS_COUNT") instanceof LongValue);
        assertEquals(8, ((LongValue) contributor1.getField("PERSONAL_COMMITS_COUNT")).getValue());

        assertTrue(contributor1.getField("PERSONAL_PROJECTS_COUNT") instanceof LongValue);
        assertEquals(9, ((LongValue) contributor1.getField("PERSONAL_PROJECTS_COUNT")).getValue());

        assertTrue(contributor1.getField("URL") instanceof StringValue);
        assertEquals("!0!", ((StringValue) contributor1.getField("URL")).getValue());
    }
}
