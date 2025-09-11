package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.sql.mysql.DatabaseFakeMySQLSutController;
import org.evomaster.client.java.controller.internal.db.sql.mysql.DatabaseMySQLTestInit;
import org.evomaster.client.java.sql.DbInfoExtractor;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.sql.internal.SqlDbHarvester;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SqlDbHarvesterTest extends DatabaseMySQLTestInit implements DatabaseTestTemplate {

    @BeforeAll
    public static void createTable() throws Exception {
        SqlScriptRunner.runScriptFromResourceFile(connection, "/db_schemas/order.sql");
        SqlScriptRunner.runScriptFromResourceFile(connection, "/db_schemas/order_init.sql");
    }

    @Test
    public void testInitTableSize() throws Exception{
        DbInfoDto dbInfoDto = DbInfoExtractor.extract(getConnection());
        assertEquals(dbInfoDto.tables.size(), 10);

    }

    @Test
    public void testSortTablesByDependency() throws Exception{
        DbInfoDto dbInfoDto = DbInfoExtractor.extract(getConnection());
        List<TableDto> tables = SqlDbHarvester.sortTablesByDependency(dbInfoDto);
        Arrays.asList("coupons","categories","users","products").stream().allMatch(
                s -> tables.subList(0,4).contains(s)
        );
        Arrays.asList("orders","user_addresses").stream().allMatch(
                s -> tables.subList(4,6).contains(s)
        );

        Arrays.asList("user_coupons","reviews","order_items","payments").stream().allMatch(
                s -> tables.subList(6,10).contains(s)
        );
    }
    @Test
    public void testGenerateSelectSqls() throws Exception{
        DbInfoDto dbInfoDto = DbInfoExtractor.extract(getConnection());
        List<TableDto> tables = SqlDbHarvester.sortTablesByDependency(dbInfoDto);
        Map<String, String> selects = SqlDbHarvester.generateSelectSqls(tables, 3);
        List<String> expectedSelectSqls = Arrays.asList("SELECT `coupons`.`coupon_id`, `coupons`.`code`, `coupons`.`discount_percent`, `coupons`.`valid_from`, `coupons`.`valid_until`, `coupons`.`is_active` FROM `coupons` LIMIT 3;",
                "SELECT `categories`.`category_id`, `categories`.`name`, `categories`.`description` FROM `categories` LIMIT 3;",
                "SELECT `users`.`user_id`, `users`.`username`, `users`.`email`, `users`.`created_at` FROM `users` LIMIT 3;",
                "SELECT `products`.`product_id`, `products`.`name`, `products`.`description`, `products`.`price`, `products`.`stock`, `products`.`category_id`, `products`.`created_at` FROM `products` JOIN `categories` ON `products`.`category_id` = `categories`.`category_id` LIMIT 3;",
                "SELECT `orders`.`order_id`, `orders`.`user_id`, `orders`.`total_amount`, `orders`.`status`, `orders`.`order_date` FROM `orders` JOIN `users` ON `orders`.`user_id` = `users`.`user_id` LIMIT 3;",
                "SELECT `user_addresses`.`address_id`, `user_addresses`.`user_id`, `user_addresses`.`address_line`, `user_addresses`.`city`, `user_addresses`.`postal_code`, `user_addresses`.`country`, `user_addresses`.`is_default` FROM `user_addresses` JOIN `users` ON `user_addresses`.`user_id` = `users`.`user_id` LIMIT 3;",
                "SELECT `user_coupons`.`user_coupon_id`, `user_coupons`.`user_id`, `user_coupons`.`coupon_id`, `user_coupons`.`used`, `user_coupons`.`used_at` FROM `user_coupons` JOIN `coupons` ON `user_coupons`.`coupon_id` = `coupons`.`coupon_id` JOIN `users` ON `user_coupons`.`user_id` = `users`.`user_id` LIMIT 3;",
                "SELECT `reviews`.`review_id`, `reviews`.`user_id`, `reviews`.`product_id`, `reviews`.`rating`, `reviews`.`comment`, `reviews`.`created_at` FROM `reviews` JOIN `products` ON `reviews`.`product_id` = `products`.`product_id` JOIN `users` ON `reviews`.`user_id` = `users`.`user_id` LIMIT 3;",
                "SELECT `order_items`.`item_id`, `order_items`.`order_id`, `order_items`.`product_id`, `order_items`.`quantity`, `order_items`.`unit_price` FROM `order_items` JOIN `orders` ON `order_items`.`order_id` = `orders`.`order_id` JOIN `products` ON `order_items`.`product_id` = `products`.`product_id` LIMIT 3;",
                "SELECT `payments`.`payment_id`, `payments`.`order_id`, `payments`.`amount`, `payments`.`payment_method`, `payments`.`payment_status`, `payments`.`transaction_id`, `payments`.`paid_at` FROM `payments` JOIN `orders` ON `payments`.`order_id` = `orders`.`order_id` LIMIT 3;"
                );

        assertEquals(expectedSelectSqls.size(), 10);
        expectedSelectSqls.stream().allMatch(s -> selects.values().contains(s));
//        for (Map.Entry<String, String> entry : selects.entrySet()) {
//            QueryResult qr = SqlScriptRunner.execCommand(getConnection(), entry.getValue());
//        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection);
    }
}
