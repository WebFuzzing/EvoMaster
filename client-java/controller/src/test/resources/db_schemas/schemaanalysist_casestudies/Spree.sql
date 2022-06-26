CREATE TABLE "schema_migrations" ("version" varchar(255) NOT NULL);
CREATE TABLE "spree_activators" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "description" varchar(255), "expires_at" datetime, "starts_at" datetime, "name" varchar(255), "event_name" varchar(255), "type" varchar(255), "usage_limit" integer, "match_policy" varchar(255) DEFAULT 'all', "code" varchar(255), "advertise" boolean DEFAULT 'f', "path" varchar(255), "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_addresses" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "firstname" varchar(255), "lastname" varchar(255), "address1" varchar(255), "address2" varchar(255), "city" varchar(255), "zipcode" varchar(255), "phone" varchar(255), "state_name" varchar(255), "alternative_phone" varchar(255), "company" varchar(255), "state_id" integer, "country_id" integer, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_adjustments" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "source_id" integer, "source_type" varchar(255), "adjustable_id" integer, "adjustable_type" varchar(255), "originator_id" integer, "originator_type" varchar(255), "amount" decimal(8,2), "label" varchar(255), "mandatory" boolean, "locked" boolean, "eligible" boolean DEFAULT 't', "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_assets" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "viewable_id" integer, "viewable_type" varchar(255), "attachment_width" integer, "attachment_height" integer, "attachment_file_size" integer, "position" integer, "attachment_content_type" varchar(255), "attachment_file_name" varchar(255), "type" varchar(75), "attachment_updated_at" datetime, "alt" text);
CREATE TABLE "spree_calculators" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "type" varchar(255), "calculable_id" integer, "calculable_type" varchar(255), "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_configurations" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "name" varchar(255), "type" varchar(50), "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_countries" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "iso_name" varchar(255), "iso" varchar(255), "iso3" varchar(255), "name" varchar(255), "numcode" integer, "states_required" boolean DEFAULT 't');
CREATE TABLE "spree_credit_cards" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "month" varchar(255), "year" varchar(255), "cc_type" varchar(255), "last_digits" varchar(255), "first_name" varchar(255), "last_name" varchar(255), "start_month" varchar(255), "start_year" varchar(255), "issue_number" varchar(255), "address_id" integer, "gateway_customer_profile_id" varchar(255), "gateway_payment_profile_id" varchar(255), "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_gateways" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "type" varchar(255), "name" varchar(255), "description" text, "active" boolean DEFAULT 't', "environment" varchar(255) DEFAULT 'development', "server" varchar(255) DEFAULT 'test', "test_mode" boolean DEFAULT 't', "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_inventory_units" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "lock_version" integer DEFAULT 0, "state" varchar(255), "variant_id" integer, "order_id" integer, "shipment_id" integer, "return_authorization_id" integer, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_line_items" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "variant_id" integer, "order_id" integer, "quantity" integer NOT NULL, "price" decimal(8,2) NOT NULL, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL, "currency" varchar(255));
CREATE TABLE "spree_log_entries" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "source_id" integer, "source_type" varchar(255), "details" text, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_mail_methods" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "environment" varchar(255), "active" boolean DEFAULT 't', "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_option_types" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "name" varchar(100), "presentation" varchar(100), "position" integer DEFAULT 0 NOT NULL, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_option_types_prototypes" ("prototype_id" integer, "option_type_id" integer);
CREATE TABLE "spree_option_values" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "position" integer, "name" varchar(255), "presentation" varchar(255), "option_type_id" integer, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_option_values_variants" ("variant_id" integer, "option_value_id" integer);
CREATE TABLE "spree_orders" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "number" varchar(15), "item_total" decimal(8,2) DEFAULT 0.0 NOT NULL, "total" decimal(8,2) DEFAULT 0.0 NOT NULL, "state" varchar(255), "adjustment_total" decimal(8,2) DEFAULT 0.0 NOT NULL, "user_id" integer, "completed_at" datetime, "bill_address_id" integer, "ship_address_id" integer, "payment_total" decimal(8,2) DEFAULT 0.0, "shipping_method_id" integer, "shipment_state" varchar(255), "payment_state" varchar(255), "email" varchar(255), "special_instructions" text, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL, "currency" varchar(255), "last_ip_address" varchar(255));
CREATE TABLE "spree_payment_methods" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "type" varchar(255), "name" varchar(255), "description" text, "active" boolean DEFAULT 't', "environment" varchar(255) DEFAULT 'development', "deleted_at" datetime, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL, "display_on" varchar(255));
CREATE TABLE "spree_payments" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "amount" decimal(8,2) DEFAULT 0.0 NOT NULL, "order_id" integer, "source_id" integer, "source_type" varchar(255), "payment_method_id" integer, "state" varchar(255), "response_code" varchar(255), "avs_response" varchar(255), "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL, "identifier" varchar(255));
CREATE TABLE "spree_preferences" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "value" text, "key" varchar(255), "value_type" varchar(255), "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_prices" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "variant_id" integer NOT NULL, "amount" decimal(8,2), "currency" varchar(255));
CREATE TABLE "spree_product_option_types" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "position" integer, "product_id" integer, "option_type_id" integer, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_product_properties" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "value" varchar(255), "product_id" integer, "property_id" integer, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL, "position" integer DEFAULT 0);
CREATE TABLE "spree_products" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "name" varchar(255) DEFAULT '' NOT NULL, "description" text, "available_on" datetime, "deleted_at" datetime, "permalink" varchar(255), "meta_description" varchar(255), "meta_keywords" varchar(255), "tax_category_id" integer, "shipping_category_id" integer, "count_on_hand" integer DEFAULT 0, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL, "on_demand" boolean DEFAULT 'f');
CREATE TABLE "spree_products_promotion_rules" ("product_id" integer, "promotion_rule_id" integer);
CREATE TABLE "spree_products_taxons" ("product_id" integer, "taxon_id" integer);
CREATE TABLE "spree_promotion_action_line_items" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "promotion_action_id" integer, "variant_id" integer, "quantity" integer DEFAULT 1);
CREATE TABLE "spree_promotion_actions" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "activator_id" integer, "position" integer, "type" varchar(255));
CREATE TABLE "spree_promotion_rules" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "activator_id" integer, "user_id" integer, "product_group_id" integer, "type" varchar(255), "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_promotion_rules_users" ("user_id" integer, "promotion_rule_id" integer);
CREATE TABLE "spree_properties" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "name" varchar(255), "presentation" varchar(255) NOT NULL, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_properties_prototypes" ("prototype_id" integer, "property_id" integer);
CREATE TABLE "spree_prototypes" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "name" varchar(255), "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_return_authorizations" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "number" varchar(255), "state" varchar(255), "amount" decimal(8,2) DEFAULT 0.0 NOT NULL, "order_id" integer, "reason" text, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_roles" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "name" varchar(255));
CREATE TABLE "spree_roles_users" ("role_id" integer, "user_id" integer);
CREATE TABLE "spree_shipments" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "tracking" varchar(255), "number" varchar(255), "cost" decimal(8,2), "shipped_at" datetime, "order_id" integer, "shipping_method_id" integer, "address_id" integer, "state" varchar(255), "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_shipping_categories" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "name" varchar(255), "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_shipping_methods" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "name" varchar(255), "zone_id" integer, "display_on" varchar(255), "shipping_category_id" integer, "match_none" boolean, "match_all" boolean, "match_one" boolean, "deleted_at" datetime, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_state_changes" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "name" varchar(255), "previous_state" varchar(255), "stateful_id" integer, "user_id" integer, "stateful_type" varchar(255), "next_state" varchar(255), "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_states" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "name" varchar(255), "abbr" varchar(255), "country_id" integer);
CREATE TABLE "spree_tax_categories" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "name" varchar(255), "description" varchar(255), "is_default" boolean DEFAULT 'f', "deleted_at" datetime, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_tax_rates" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "amount" decimal(8,5), "zone_id" integer, "tax_category_id" integer, "included_in_price" boolean DEFAULT 'f', "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL, "name" varchar(255), "show_rate_in_label" boolean DEFAULT 't');
CREATE TABLE "spree_taxonomies" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "name" varchar(255) NOT NULL, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL, "position" integer DEFAULT 0);
CREATE TABLE "spree_taxons" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "parent_id" integer, "position" integer DEFAULT 0, "name" varchar(255) NOT NULL, "permalink" varchar(255), "taxonomy_id" integer, "lft" integer, "rgt" integer, "icon_file_name" varchar(255), "icon_content_type" varchar(255), "icon_file_size" integer, "icon_updated_at" datetime, "description" text, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_tokenized_permissions" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "permissable_id" integer, "permissable_type" varchar(255), "token" varchar(255), "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_trackers" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "environment" varchar(255), "analytics_id" varchar(255), "active" boolean DEFAULT 't', "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_users" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "encrypted_password" varchar(128), "password_salt" varchar(128), "email" varchar(255), "remember_token" varchar(255), "persistence_token" varchar(255), "reset_password_token" varchar(255), "perishable_token" varchar(255), "sign_in_count" integer DEFAULT 0 NOT NULL, "failed_attempts" integer DEFAULT 0 NOT NULL, "last_request_at" datetime, "current_sign_in_at" datetime, "last_sign_in_at" datetime, "current_sign_in_ip" varchar(255), "last_sign_in_ip" varchar(255), "login" varchar(255), "ship_address_id" integer, "bill_address_id" integer, "authentication_token" varchar(255), "unlock_token" varchar(255), "locked_at" datetime, "remember_created_at" datetime, "reset_password_sent_at" datetime, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL, "spree_api_key" varchar(48));
CREATE TABLE "spree_variants" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "sku" varchar(255) DEFAULT '' NOT NULL, "weight" decimal(8,2), "height" decimal(8,2), "width" decimal(8,2), "depth" decimal(8,2), "deleted_at" datetime, "is_master" boolean DEFAULT 'f', "product_id" integer, "count_on_hand" integer DEFAULT 0, "cost_price" decimal(8,2), "position" integer, "lock_version" integer DEFAULT 0, "on_demand" boolean DEFAULT 'f', "cost_currency" varchar(255));
CREATE TABLE "spree_zone_members" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "zoneable_id" integer, "zoneable_type" varchar(255), "zone_id" integer, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE TABLE "spree_zones" ("id" INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, "name" varchar(255), "description" varchar(255), "default_tax" boolean DEFAULT 'f', "zone_members_count" integer DEFAULT 0, "created_at" datetime NOT NULL, "updated_at" datetime NOT NULL);
CREATE INDEX "index_addresses_on_firstname" ON "spree_addresses" ("firstname");
CREATE INDEX "index_addresses_on_lastname" ON "spree_addresses" ("lastname");
CREATE INDEX "index_adjustments_on_order_id" ON "spree_adjustments" ("adjustable_id");
CREATE INDEX "index_assets_on_viewable_id" ON "spree_assets" ("viewable_id");
CREATE INDEX "index_assets_on_viewable_type_and_type" ON "spree_assets" ("viewable_type", "type");
CREATE INDEX "index_inventory_units_on_order_id" ON "spree_inventory_units" ("order_id");
CREATE INDEX "index_inventory_units_on_shipment_id" ON "spree_inventory_units" ("shipment_id");
CREATE INDEX "index_inventory_units_on_variant_id" ON "spree_inventory_units" ("variant_id");
CREATE INDEX "index_option_values_variants_on_variant_id_and_option_value_id" ON "spree_option_values_variants" ("variant_id", "option_value_id");
CREATE INDEX "index_product_properties_on_product_id" ON "spree_product_properties" ("product_id");
CREATE INDEX "index_products_promotion_rules_on_product_id" ON "spree_products_promotion_rules" ("product_id");
CREATE INDEX "index_products_promotion_rules_on_promotion_rule_id" ON "spree_products_promotion_rules" ("promotion_rule_id");
CREATE INDEX "index_promotion_rules_on_product_group_id" ON "spree_promotion_rules" ("product_group_id");
CREATE INDEX "index_promotion_rules_on_user_id" ON "spree_promotion_rules" ("user_id");
CREATE INDEX "index_promotion_rules_users_on_promotion_rule_id" ON "spree_promotion_rules_users" ("promotion_rule_id");
CREATE INDEX "index_promotion_rules_users_on_user_id" ON "spree_promotion_rules_users" ("user_id");
CREATE INDEX "index_shipments_on_number" ON "spree_shipments" ("number");
CREATE INDEX "index_spree_configurations_on_name_and_type" ON "spree_configurations" ("name", "type");
CREATE INDEX "index_spree_line_items_on_order_id" ON "spree_line_items" ("order_id");
CREATE INDEX "index_spree_line_items_on_variant_id" ON "spree_line_items" ("variant_id");
CREATE INDEX "index_spree_option_values_variants_on_variant_id" ON "spree_option_values_variants" ("variant_id");
CREATE INDEX "index_spree_orders_on_number" ON "spree_orders" ("number");
CREATE UNIQUE INDEX "index_spree_preferences_on_key" ON "spree_preferences" ("key");
CREATE INDEX "index_spree_products_on_available_on" ON "spree_products" ("available_on");
CREATE INDEX "index_spree_products_on_deleted_at" ON "spree_products" ("deleted_at");
CREATE INDEX "index_spree_products_on_name" ON "spree_products" ("name");
CREATE INDEX "index_spree_products_on_permalink" ON "spree_products" ("permalink");
CREATE INDEX "index_spree_products_taxons_on_product_id" ON "spree_products_taxons" ("product_id");
CREATE INDEX "index_spree_products_taxons_on_taxon_id" ON "spree_products_taxons" ("taxon_id");
CREATE INDEX "index_spree_roles_users_on_role_id" ON "spree_roles_users" ("role_id");
CREATE INDEX "index_spree_roles_users_on_user_id" ON "spree_roles_users" ("user_id");
CREATE INDEX "index_spree_variants_on_product_id" ON "spree_variants" ("product_id");
CREATE INDEX "index_taxons_on_parent_id" ON "spree_taxons" ("parent_id");
CREATE INDEX "index_taxons_on_permalink" ON "spree_taxons" ("permalink");
CREATE INDEX "index_taxons_on_taxonomy_id" ON "spree_taxons" ("taxonomy_id");
CREATE INDEX "index_tokenized_name_and_type" ON "spree_tokenized_permissions" ("permissable_id", "permissable_type");
CREATE UNIQUE INDEX "unique_schema_migrations" ON "schema_migrations" ("version");
INSERT INTO schema_migrations (version) VALUES ('20130501130153');

INSERT INTO schema_migrations (version) VALUES ('20130501130154');

INSERT INTO schema_migrations (version) VALUES ('20130501130155');

INSERT INTO schema_migrations (version) VALUES ('20130501130156');

INSERT INTO schema_migrations (version) VALUES ('20130501130157');

INSERT INTO schema_migrations (version) VALUES ('20130501130158');

INSERT INTO schema_migrations (version) VALUES ('20130501130159');

INSERT INTO schema_migrations (version) VALUES ('20130501130160');

INSERT INTO schema_migrations (version) VALUES ('20130501130161');

INSERT INTO schema_migrations (version) VALUES ('20130501130162');

INSERT INTO schema_migrations (version) VALUES ('20130501130163');

INSERT INTO schema_migrations (version) VALUES ('20130501130164');

INSERT INTO schema_migrations (version) VALUES ('20130501130165');

INSERT INTO schema_migrations (version) VALUES ('20130501130166');

INSERT INTO schema_migrations (version) VALUES ('20130501130167');

INSERT INTO schema_migrations (version) VALUES ('20130501130168');

INSERT INTO schema_migrations (version) VALUES ('20130501130169');

INSERT INTO schema_migrations (version) VALUES ('20130501130170');

INSERT INTO schema_migrations (version) VALUES ('20130501130171');

INSERT INTO schema_migrations (version) VALUES ('20130501130172');

INSERT INTO schema_migrations (version) VALUES ('20130501130173');

INSERT INTO schema_migrations (version) VALUES ('20130501130174');

INSERT INTO schema_migrations (version) VALUES ('20130501130175');