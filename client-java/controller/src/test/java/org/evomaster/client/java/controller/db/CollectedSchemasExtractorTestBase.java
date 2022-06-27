package org.evomaster.client.java.controller.db;

import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.internal.db.SchemaExtractor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;

import static org.junit.Assert.assertNotNull;

public interface CollectedSchemasExtractorTestBase {

    Connection getConnection();

    @ParameterizedTest
    @ValueSource(strings = {"AdmissionsPatient.sql", "AdmissionsPatientRepaired.sql",
            "ArtistSimilarity.sql", "ArtistTerm.sql", "BankAccount.sql",
            "BioSQL.sql", "BookTown.sql", "BookTown-orig.sql",
            "BrowserCookies.sql", "Campaign.sql", "ChromeDB.sql",
            "Cloc.sql", "CoffeeOrders.sql", "Crafts2002.sql",
            "Crafts2002Repaired.sql", "CustomerOrder.sql", "DavilaDjango.sql",
            "DellStore.sql", "DellStore-orig.sql", "DHDBookstore.sql",
            "DHDBookstoreRepaired.sql", "Employee.sql", "Examination.sql",
            "FACAData1997.sql", "ACAData1997Repaired.sql", "Factory2000.sql",
            "Flav_R03_1.sql", "Flav_R03_1Repaired.sql", "Flights.sql",
            "FrenchTowns.sql", "GeoMetadb.sql", "Gitlab.sql", "Gitlab-orig.sql",
            "H1EFileFY2007.sql", "H1EFileFY2007Repaired.sql", "HR_30.sql",
            "Hydat.sql", "HydatRepaired.sql", "Inventory.sql", "Iso3166.sql",
            "IsoFlav_R2.sql", "IsoFlav_R2Repaired.sql", "iTrust.sql",
            "JWhoisServer.sql", "Magento.sql", "MediaWiki.sql", "MediaWiki-orig.sql",
            "MozillaExtensions.sql", "MozillaPermissions.sql", "MozillaPlaces.sql",
            "Mxm.sql", "NistDML181.sql", "NistDML181NotNulls.sql", "NistDML182.sql",
            "NistDML182NotNulls.sql", "NistDML183.sql", "NistDML183Ints.sql",
            "NistDML183IntsNotNulls.sql", "NistDML183NotNulls.sql",
            "NistDML183Varchars.sql", "NistDML183VarcharsNotNulls.sql", "NistWeather.sql",
            "NistXTS748.sql", "NistXTS749.sql", "Northwind.sql",
            "Northwind.sql", "ite3.sql", "Pagila.sql",
            "PagilaPrime.sql", "Person.sql", "Products.sql",
            "ProductSales.sql", "ProductSalesRepaired.sql", "RiskIt.sql",
            "SH_30.sql", "Skype.sql", "SongTrackMetadata.sql",
            "Spree.sql", "SRAMetadb.sql", "StackOverflow.sql",
            "StudentResidence.sql", "TestSchema.sql", "TweetComplete.sql",
            "Twitter.sql", "University.sql", "UnixUsage.sql",
            "Usda.sql", "Voc.sql", "Wikimedia.sql",
            "WordNet.sql", "WordPress.sql", "WordPress-orig.sql",
            "World.sql", "World-orig.sql", "Writers.sql", "WwWordNet.sql"})
    default void testExtraction(String sqlFileName) throws Exception {
        try {
            String resourcePath = "/db_schemas/schemaanalysist_casestudies/" + sqlFileName;
            SqlScriptRunner.runScriptFromResourceFile(getConnection(), resourcePath);
        } catch (Exception ex) {
            // Ignore test case if SQL script fails
            Assumptions.assumeTrue(false,ex.getMessage());
        }
        DbSchemaDto schema = SchemaExtractor.extract(getConnection());
        Assertions.assertNotNull(schema);
    }

}
