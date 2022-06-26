-- SCHEMA - Magento 1.1.1
-- Source: https://www.magentocommerce.com/wiki/2_-_magento_concepts_and_architecture/magento_database_diagram

create table admin_assert
(
   assert_id            int(10) unsigned not null auto_increment,
   assert_type          national varchar(20) not null,
   assert_data          national text,
   primary key (assert_id)
)
type = innodb;

alter table admin_assert comment 'Acl Asserts';

create table admin_role
(
   role_id              int(10) unsigned not null auto_increment,
   parent_id            int(10) unsigned not null default 0,
   tree_level           tinyint(3) unsigned not null default 0,
   sort_order           tinyint(3) unsigned not null default 0,
   role_type            national char(1) not null default '0',
   user_id              int(11) unsigned not null default 0,
   role_name            national varchar(50) not null,
   primary key (role_id)
)
type = innodb;

alter table admin_role comment 'Acl Roles';

create index parent_id on admin_role
(

);

create index tree_level on admin_role
(
   tree_level
);

create table admin_rule
(
   rule_id              int(10) unsigned not null auto_increment,
   role_id              int(10) unsigned not null default 0,
   resource_id          national varchar(255) not null,
   privileges           national varchar(20) not null,
   assert_id            int(10) unsigned not null default 0,
   role_type            national char(1),
   permission           national varchar(10),
   primary key (rule_id)
)
type = innodb;

alter table admin_rule comment 'Acl Rules';

create index resource on admin_rule
(

);

create index role_id on admin_rule
(

);

create table admin_user
(
   user_id              mediumint(9) unsigned not null auto_increment,
   firstname            national varchar(32) not null,
   lastname             national varchar(32) not null,
   email                national varchar(128) not null,
   username             national varchar(40) not null,
   password             national varchar(40) not null,
   created              datetime not null default 0000-00-00 00:00:00,
   modified             datetime,
   logdate              datetime,
   lognum               smallint(5) unsigned not null default 0,
   reload_acl_flag      tinyint(1) not null default 0,
   is_active            tinyint(1) not null default 1,
   extra                national text,
   primary key (user_id)
)
type = innodb;

alter table admin_user comment 'Users';

create table adminnotification_inbox
(
   notification_id      int(10) unsigned not null auto_increment,
   severity             tinyint(3) unsigned not null default 0,
   date_added           datetime not null,
   title                national varchar(255) not null,
   description          national text,
   url                  national varchar(255) not null,
   is_read              tinyint(1) unsigned not null default 0,
   is_remove            tinyint(1) unsigned not null default 0,
   primary key (notification_id)
)
type = innodb type = innodb;

create index idx_is_read on adminnotification_inbox
(
   is_read
);

create index idx_is_remove on adminnotification_inbox
(
   is_remove
);

create index idx_severity on adminnotification_inbox
(
   severity
);

create table api_assert
(
   assert_id            int(10) unsigned not null auto_increment,
   assert_type          national varchar(20) not null,
   assert_data          national text,
   primary key (assert_id)
)
type = innodb type = innodb;

alter table api_assert comment 'Api Acl Asserts';

create table api_role
(
   role_id              int(10) unsigned not null auto_increment,
   parent_id            int(10) unsigned not null default 0,
   tree_level           tinyint(3) unsigned not null default 0,
   sort_order           tinyint(3) unsigned not null default 0,
   role_type            national char(1) not null default '0',
   user_id              int(11) unsigned not null default 0,
   role_name            national varchar(50) not null,
   primary key (role_id)
)
type = innodb type = innodb;

alter table api_role comment 'Api Acl Roles';

create index parent_id on api_role
(

);

create index tree_level on api_role
(
   tree_level
);

create table api_rule
(
   rule_id              int(10) unsigned not null auto_increment,
   role_id              int(10) unsigned not null default 0,
   resource_id          national varchar(255) not null,
   privileges           national varchar(20) not null,
   assert_id            int(10) unsigned not null default 0,
   role_type            national char(1),
   permission           national varchar(10),
   primary key (rule_id)
)
type = innodb type = innodb;

alter table api_rule comment 'Api Acl Rules';

create index resource on api_rule
(

);

create index role_id on api_rule
(

);

create table api_user
(
   user_id              mediumint(9) unsigned not null auto_increment,
   firstname            national varchar(32) not null,
   lastname             national varchar(32) not null,
   email                national varchar(128) not null,
   username             national varchar(40) not null,
   api_key              national varchar(40) not null,
   created              datetime not null default 0000-00-00 00:00:00,
   modified             datetime,
   logdate              datetime,
   lognum               smallint(5) unsigned not null default 0,
   reload_acl_flag      tinyint(1) not null default 0,
   is_active            tinyint(1) not null default 1,
   primary key (user_id)
)
type = innodb type = innodb;

alter table api_user comment 'Api Users';

create table catalog_category_entity
(
   entity_id            int(10) unsigned not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_set_id     smallint(5) unsigned not null default 0,
   parent_id            int(10) unsigned not null default 0,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   path                 national varchar(255) not null,
   position             int(11) not null,
   level                int(11) not null,
   primary key (entity_id)
)
type = innodb;

alter table catalog_category_entity comment 'Category Entities';

create index idx_level on catalog_category_entity
(
   level
);

create table catalog_category_entity_datetime
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                datetime not null default 0000-00-00 00:00:00,
   primary key (value_id)
)
type = innodb;

alter table catalog_category_entity_datetime
   add unique idx_base (entity_type_id, entity_id, attribute_id, store_id);

create table catalog_category_entity_decimal
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                decimal(12,4) not null default 0.0000,
   primary key (value_id)
)
type = innodb;

alter table catalog_category_entity_decimal
   add unique idx_base (entity_type_id, entity_id, attribute_id, store_id);

create table catalog_category_entity_int
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                int(11) not null default 0,
   primary key (value_id)
)
type = innodb;

alter table catalog_category_entity_int
   add unique idx_base (entity_type_id, entity_id, attribute_id, store_id);

create table catalog_category_entity_text
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national text not null,
   primary key (value_id)
)
type = innodb;

alter table catalog_category_entity_text
   add unique idx_base (entity_type_id, entity_id, attribute_id, store_id);

create table catalog_category_entity_varchar
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national varchar(255) not null,
   primary key (value_id)
)
type = innodb;

alter table catalog_category_entity_varchar
   add unique idx_base (entity_type_id, entity_id, attribute_id, store_id);

create table catalog_category_product
(
   category_id          int(10) unsigned not null default 0,
   product_id           int(10) unsigned not null default 0,
   position             int(10) unsigned not null default 0
)
type = innodb;

alter table catalog_category_product
   add unique unq_category_product (category_id, product_id);

create table catalog_category_product_index
(
   category_id          int(10) unsigned not null default 0,
   product_id           int(10) unsigned not null default 0,
   position             int(10) unsigned not null default 0,
   is_parent            tinyint(1) unsigned not null default 0
);

alter table catalog_category_product_index
   add unique unq_category_product (category_id, product_id);

create index idx_category_position on catalog_category_product_index
(

);

create table catalog_compare_item
(
   catalog_compare_item_id int(11) unsigned not null auto_increment,
   visitor_id           int(11) unsigned not null default 0,
   customer_id          int(11) unsigned,
   product_id           int(11) unsigned not null default 0,
   primary key (catalog_compare_item_id)
)
type = innodb;

create index idx_customer_products on catalog_compare_item
(

);

create index idx_visitor_products on catalog_compare_item
(

);

create table catalog_product_bundle_option
(
   option_id            int(10) unsigned not null auto_increment,
   parent_id            int(10) unsigned not null,
   required             tinyint(1) unsigned not null default 0,
   position             int(10) unsigned not null default 0,
   type                 national varchar(255) not null,
   primary key (option_id)
)
type = innodb;

alter table catalog_product_bundle_option comment 'Bundle Options';

create table catalog_product_bundle_option_value
(
   value_id             int(10) unsigned not null auto_increment,
   option_id            int(10) unsigned not null,
   store_id             smallint(5) unsigned not null,
   title                national varchar(255) not null,
   primary key (value_id)
)
type = innodb;

alter table catalog_product_bundle_option_value comment 'Bundle Selections';

create table catalog_product_bundle_selection
(
   selection_id         int(10) unsigned not null auto_increment,
   option_id            int(10) unsigned not null,
   parent_product_id    int(10) unsigned not null,
   product_id           int(10) unsigned not null,
   position             int(10) unsigned not null default 0,
   is_default           tinyint(1) unsigned not null default 0,
   selection_price_type tinyint(1) unsigned not null default 0,
   selection_price_value decimal(12,4) not null default 0.0000,
   selection_qty        decimal(12,4) not null default 0.0000,
   selection_can_change_qty tinyint(1) not null default 0,
   primary key (selection_id)
)
type = innodb type = innodb;

alter table catalog_product_bundle_selection comment 'Bundle Selections';

create table catalog_product_enabled_index
(
   product_id           int(10) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   visibility           smallint(5) unsigned not null default 0
);

alter table catalog_product_enabled_index
   add unique unq_product_store (product_id, store_id);

create index idx_product_visibility_in_store on catalog_product_enabled_index
(

);

create table catalog_product_entity
(
   entity_id            int(10) unsigned not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_set_id     smallint(5) unsigned not null default 0,
   type_id              national varchar(32) not null default simple,
   sku                  national varchar(64),
   category_ids         national text,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   has_options          smallint(1) not null default 0,
   primary key (entity_id)
)
type = innodb;

alter table catalog_product_entity comment 'Product Entities';

create index sku on catalog_product_entity
(
   sku
);

create table catalog_product_entity_datetime
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                datetime not null default 0000-00-00 00:00:00,
   primary key (value_id)
)
type = innodb;

create index idx_attribute_value on catalog_product_entity_datetime
(

);

create table catalog_product_entity_decimal
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                decimal(12,4) not null default 0.0000,
   primary key (value_id)
)
type = innodb;

create index idx_attribute_value on catalog_product_entity_decimal
(

);

create table catalog_product_entity_gallery
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   position             int(11) not null default 0,
   value                national varchar(255) not null,
   primary key (value_id)
)
type = innodb;

create index idx_base on catalog_product_entity_gallery
(

);

create table catalog_product_entity_int
(
   value_id             int(11) not null auto_increment,
   entity_type_id       mediumint(8) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                int(11) not null default 0,
   primary key (value_id)
)
type = innodb;

create index idx_attribute_value on catalog_product_entity_int
(

);

create table catalog_product_entity_media_gallery
(
   value_id             int(11) unsigned not null auto_increment,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national varchar(255),
   primary key (value_id)
)
type = innodb;

alter table catalog_product_entity_media_gallery comment 'Catalog Product Media Gallery';

create table catalog_product_entity_media_gallery_value
(
   value_id             int(11) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   `label`              national varchar(255),
   position             int(11) unsigned,
   disabled             tinyint(1) unsigned not null default 0,
   primary key (value_id, store_id)
)
type = innodb;

alter table catalog_product_entity_media_gallery_value comment 'Catalog Product Media Gallery Values';

create table catalog_product_entity_text
(
   value_id             int(11) not null auto_increment,
   entity_type_id       mediumint(8) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national text not null,
   primary key (value_id)
)
type = innodb;

create index idx_attribute_value on catalog_product_entity_text
(

);

create table catalog_product_entity_tier_price
(
   value_id             int(11) not null auto_increment,
   entity_id            int(10) unsigned not null default 0,
   all_groups           tinyint(1) unsigned not null default 1,
   customer_group_id    smallint(5) unsigned not null default 0,
   qty                  decimal(12,4) not null default 1.0000,
   value                decimal(12,4) not null default 0.0000,
   website_id           smallint(5) unsigned not null,
   primary key (value_id)
)
type = innodb;

create table catalog_product_entity_varchar
(
   value_id             int(11) not null auto_increment,
   entity_type_id       mediumint(8) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national varchar(255) not null,
   primary key (value_id)
)
type = innodb;

create index idx_attribute_value on catalog_product_entity_varchar
(

);

create table catalog_product_link
(
   link_id              int(11) unsigned not null auto_increment,
   product_id           int(10) unsigned not null default 0,
   linked_product_id    int(10) unsigned not null default 0,
   link_type_id         tinyint(3) unsigned not null default 0,
   primary key (link_id)
)
type = innodb;

alter table catalog_product_link comment 'Related Products';

create table catalog_product_link_attribute
(
   product_link_attribute_id smallint(6) unsigned not null auto_increment,
   link_type_id         tinyint(3) unsigned not null default 0,
   product_link_attribute_code national varchar(32) not null,
   data_type            national varchar(32) not null,
   primary key (product_link_attribute_id)
)
type = innodb;

alter table catalog_product_link_attribute comment 'Attributes For Product Link';

create table catalog_product_link_attribute_decimal
(
   value_id             int(11) unsigned not null auto_increment,
   product_link_attribute_id smallint(6) unsigned,
   link_id              int(11) unsigned,
   value                decimal(12,4) not null default 0.0000,
   primary key (value_id)
)
type = innodb;

alter table catalog_product_link_attribute_decimal comment 'Decimal Attributes Values';

create table catalog_product_link_attribute_int
(
   value_id             int(11) unsigned not null auto_increment,
   product_link_attribute_id smallint(6) unsigned,
   link_id              int(11) unsigned,
   value                int(11) not null default 0,
   primary key (value_id)
)
type = innodb;

create table catalog_product_link_attribute_varchar
(
   value_id             int(11) unsigned not null auto_increment,
   product_link_attribute_id smallint(6) unsigned not null default 0,
   link_id              int(11) unsigned,
   value                national varchar(255) not null,
   primary key (value_id)
)
type = innodb;

alter table catalog_product_link_attribute_varchar comment 'Varchar Attributes Values';

create table catalog_product_link_type
(
   link_type_id         tinyint(3) unsigned not null auto_increment,
   code                 national varchar(32) not null,
   primary key (link_type_id)
)
type = innodb;

alter table catalog_product_link_type comment 'Types Of Product Link(Related, Superproduct, Bundles)';

create table catalog_product_option
(
   option_id            int(10) unsigned not null auto_increment,
   product_id           int(10) unsigned not null default 0,
   type                 national varchar(50) not null,
   is_require           tinyint(1) not null default 1,
   sku                  national varchar(64) not null,
   max_characters       int(10) unsigned,
   file_extension       national varchar(50),
   sort_order           int(10) unsigned not null default 0,
   primary key (option_id)
)
type = innodb type = innodb;

create index catalog_product_option_product on catalog_product_option
(
   product_id
);

create table catalog_product_option_price
(
   option_price_id      int(10) unsigned not null auto_increment,
   option_id            int(10) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   price                decimal(12,4) not null default 0.0000,
   price_type           national enum('fixed','percent') not null default fixed,
   primary key (option_price_id)
)
type = innodb type = innodb;

create index catalog_product_option_price_option on catalog_product_option_price
(
   option_id
);

create index catalog_product_option_title_store on catalog_product_option_price
(
   store_id
);

create table catalog_product_option_title
(
   option_title_id      int(10) unsigned not null auto_increment,
   option_id            int(10) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   title                national varchar(50) not null,
   primary key (option_title_id)
)
type = innodb type = innodb;

create index catalog_product_option_title_option on catalog_product_option_title
(
   option_id
);

create index catalog_product_option_title_store on catalog_product_option_title
(
   store_id
);

create table catalog_product_option_type_price
(
   option_type_price_id int(10) unsigned not null auto_increment,
   option_type_id       int(10) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   price                decimal(12,4) not null default 0.0000,
   price_type           national enum('fixed','percent') not null default fixed,
   primary key (option_type_price_id)
)
type = innodb type = innodb;

create index catalog_product_option_type_price_option_type on catalog_product_option_type_price
(
   option_type_id
);

create index catalog_product_option_type_price_store on catalog_product_option_type_price
(
   store_id
);

create table catalog_product_option_type_title
(
   option_type_title_id int(10) unsigned not null auto_increment,
   option_type_id       int(10) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   title                national varchar(50) not null,
   primary key (option_type_title_id)
)
type = innodb type = innodb;

create index catalog_product_option_type_title_option on catalog_product_option_type_title
(
   option_type_id
);

create index catalog_product_option_type_title_store on catalog_product_option_type_title
(
   store_id
);

create table catalog_product_option_type_value
(
   option_type_id       int(10) unsigned not null auto_increment,
   option_id            int(10) unsigned not null default 0,
   sku                  national varchar(64) not null,
   sort_order           int(10) unsigned not null default 0,
   primary key (option_type_id)
)
type = innodb type = innodb;

create index catalog_product_option_type_value_option on catalog_product_option_type_value
(
   option_id
);

create table catalog_product_super_attribute
(
   product_super_attribute_id int(10) unsigned not null auto_increment,
   product_id           int(10) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   position             smallint(5) unsigned not null default 0,
   primary key (product_super_attribute_id)
)
type = innodb;

create table catalog_product_super_attribute_label
(
   value_id             int(10) unsigned not null auto_increment,
   product_super_attribute_id int(10) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   value                national varchar(255) not null,
   primary key (value_id)
)
type = innodb;

create table catalog_product_super_attribute_pricing
(
   value_id             int(10) unsigned not null auto_increment,
   product_super_attribute_id int(10) unsigned not null default 0,
   value_index          national varchar(255) not null,
   is_percent           tinyint(1) unsigned default 0,
   pricing_value        decimal(10,4),
   primary key (value_id)
)
type = innodb;

create table catalog_product_super_link
(
   link_id              int(10) unsigned not null auto_increment,
   product_id           int(10) unsigned not null default 0,
   parent_id            int(10) unsigned not null default 0,
   primary key (link_id)
)
type = innodb;

create table catalog_product_website
(
   product_id           int(10) unsigned not null auto_increment,
   website_id           smallint(5) unsigned not null,
   primary key (product_id, website_id)
)
type = innodb;

create table catalogindex_eav
(
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   value                int(11) not null default 0,
   primary key (store_id, entity_id, attribute_id, value)
)
type = innodb;

create index idx_value on catalogindex_eav
(
   value
);

create table catalogindex_minimal_price
(
   index_id             int(10) unsigned not null auto_increment,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   customer_group_id    smallint(3) unsigned not null default 0,
   qty                  decimal(12,4) unsigned not null default 0.0000,
   value                decimal(12,4) not null default 0.0000,
   primary key (index_id)
)
type = innodb;

create index idx_qty on catalogindex_minimal_price
(
   qty
);

create index idx_value on catalogindex_minimal_price
(
   value
);

create table catalogindex_price
(
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   customer_group_id    smallint(3) unsigned not null default 0,
   qty                  decimal(12,4) unsigned not null default 0.0000,
   value                decimal(12,4) not null default 0.0000
)
type = innodb;

create index fk_catalogindex_price_customer_group on catalogindex_price
(
   customer_group_id
);

create index idx_qty on catalogindex_price
(
   qty
);

create index idx_range_value on catalogindex_price
(

);

create index idx_value on catalogindex_price
(
   value
);

create table cataloginventory_stock
(
   stock_id             smallint(4) unsigned not null auto_increment,
   stock_name           national varchar(255) not null,
   primary key (stock_id)
)
type = innodb;

alter table cataloginventory_stock comment 'Catalog Inventory Stocks List';

create table cataloginventory_stock_item
(
   item_id              int(10) unsigned not null auto_increment,
   product_id           int(10) unsigned not null default 0,
   stock_id             smallint(4) unsigned not null default 0,
   qty                  decimal(12,4) not null default 0.0000,
   min_qty              decimal(12,4) not null default 0.0000,
   use_config_min_qty   tinyint(1) unsigned not null default 1,
   is_qty_decimal       tinyint(1) unsigned not null default 0,
   backorders           tinyint(3) unsigned not null default 0,
   use_config_backorders tinyint(1) unsigned not null default 1,
   min_sale_qty         decimal(12,4) not null default 1.0000,
   use_config_min_sale_qty tinyint(1) unsigned not null default 1,
   max_sale_qty         decimal(12,4) not null default 0.0000,
   use_config_max_sale_qty tinyint(1) unsigned not null default 1,
   is_in_stock          tinyint(1) unsigned not null default 0,
   low_stock_date       datetime,
   notify_stock_qty     decimal(12,4),
   use_config_notify_stock_qty tinyint(1) unsigned not null default 1,
   manage_stock         tinyint(1) unsigned not null default 0,
   use_config_manage_stock tinyint(1) unsigned not null default 1,
   primary key (item_id)
)
type = innodb;

alter table cataloginventory_stock_item comment 'Inventory Stock Item Data';

alter table cataloginventory_stock_item
   add unique idx_stock_product (product_id, stock_id);

create table catalogrule
(
   rule_id              int(10) unsigned not null auto_increment,
   name                 national varchar(255) not null,
   description          national text not null,
   from_date            date,
   to_date              date,
   customer_group_ids   national varchar(255) not null,
   is_active            tinyint(1) not null default 0,
   conditions_serialized national text not null,
   actions_serialized   national text not null,
   stop_rules_processing tinyint(1) not null default 1,
   sort_order           int(10) unsigned not null default 0,
   simple_action        national varchar(32) not null,
   discount_amount      decimal(12,4) not null,
   website_ids          national text,
   primary key (rule_id)
)
type = innodb;

create index sort_order on catalogrule
(

);

create table catalogrule_product
(
   rule_product_id      int(10) unsigned not null auto_increment,
   rule_id              int(10) unsigned not null default 0,
   from_time            int(10) unsigned not null default 0,
   to_time              int(10) unsigned not null default 0,
   customer_group_id    smallint(5) unsigned not null default 0,
   product_id           int(10) unsigned not null default 0,
   action_operator      national enum('to_fixed','to_percent','by_fixed','by_percent') not null default to_fixed,
   action_amount        decimal(12,4) not null default 0.0000,
   action_stop          tinyint(1) not null default 0,
   sort_order           int(10) unsigned not null default 0,
   website_id           smallint(5) unsigned not null,
   primary key (rule_product_id)
)
type = innodb;

alter table catalogrule_product
   add unique sort_order (from_time, to_time, website_id, customer_group_id, product_id, sort_order);

create table catalogrule_product_price
(
   rule_product_price_id int(10) unsigned not null auto_increment,
   rule_date            date not null default 0000-00-00,
   customer_group_id    smallint(5) unsigned not null default 0,
   product_id           int(10) unsigned not null default 0,
   rule_price           decimal(12,4) not null default 0.0000,
   website_id           smallint(5) unsigned not null,
   latest_start_date    date,
   earliest_end_date    date,
   primary key (rule_product_price_id)
)
type = innodb;

alter table catalogrule_product_price
   add unique rule_date (rule_date, website_id, customer_group_id, product_id);

create table catalogsearch_query
(
   query_id             int(10) unsigned not null auto_increment,
   query_text           national varchar(255) not null,
   num_results          int(10) unsigned not null default 0,
   popularity           int(10) unsigned not null default 0,
   redirect             national varchar(255) not null,
   synonim_for          national varchar(255) not null,
   store_id             smallint(5) unsigned not null,
   display_in_terms     tinyint(1) not null default 1,
   updated_at           datetime not null,
   primary key (query_id)
)
type = innodb;

create index search_query on catalogsearch_query
(

);

create table checkout_agreement
(
   agreement_id         int(10) unsigned not null auto_increment,
   name                 national varchar(255) not null,
   content              national text not null,
   content_height       national varchar(25),
   checkbox_text        national text not null,
   is_active            tinyint(4) not null default 0,
   primary key (agreement_id)
)
type = innodb type = innodb;

create table checkout_agreement_store
(
   agreement_id         int(10) unsigned not null,
   store_id             smallint(5) unsigned not null
)
type = innodb type = innodb;

alter table checkout_agreement_store
   add unique agreement_id (agreement_id, store_id);

create table cms_block
(
   block_id             smallint(6) not null auto_increment,
   title                national varchar(255) not null,
   identifier           national varchar(255) not null,
   content              national text,
   creation_time        datetime,
   update_time          datetime,
   is_active            tinyint(1) not null default 1,
   primary key (block_id)
)
type = innodb;

alter table cms_block comment 'Cms Blocks';

create table cms_block_store
(
   block_id             smallint(6) not null,
   store_id             smallint(5) unsigned not null,
   primary key (block_id, store_id)
)
type = innodb;

alter table cms_block_store comment 'Cms Blocks To Stores';

create table cms_page
(
   page_id              smallint(6) not null auto_increment,
   title                national varchar(255) not null,
   root_template        national varchar(255) not null,
   meta_keywords        national text not null,
   meta_description     national text not null,
   identifier           national varchar(100) not null,
   content              national text,
   creation_time        datetime,
   update_time          datetime,
   is_active            tinyint(1) not null default 1,
   sort_order           tinyint(4) not null default 0,
   layout_update_xml    national text,
   custom_theme         national varchar(100),
   custom_theme_from    date,
   custom_theme_to      date,
   primary key (page_id)
)
type = innodb;

alter table cms_page comment 'Cms Pages';

create index identifier on cms_page
(
   identifier
);

create table cms_page_store
(
   page_id              smallint(6) not null,
   store_id             smallint(5) unsigned not null,
   primary key (page_id, store_id)
)
type = innodb;

alter table cms_page_store comment 'Cms Pages To Stores';

create table core_config_data
(
   config_id            int(10) unsigned not null auto_increment,
   scope                national enum('default','websites','stores','config') not null default default,
   scope_id             int(11) not null default 0,
   path                 national varchar(255) not null default general,
   value                national text not null,
   primary key (config_id)
)
type = innodb;

alter table core_config_data
   add unique config_scope (scope, scope_id, path);

create table core_email_template
(
   template_id          int(7) unsigned not null auto_increment,
   template_code        national varchar(150),
   template_text        national text,
   template_type        int(3) unsigned,
   template_subject     national varchar(200),
   template_sender_name national varchar(200),
   template_sender_email varchar(200),
   added_at             datetime,
   modified_at          datetime,
   primary key (template_id)
)
type = innodb;

alter table core_email_template comment 'Email Templates';

alter table core_email_template
   add unique template_code (template_code);

create index added_at on core_email_template
(
   added_at
);

create index modified_at on core_email_template
(
   modified_at
);

create table core_layout_link
(
   layout_link_id       int(10) unsigned not null auto_increment,
   store_id             smallint(5) unsigned not null default 0,
   package              national varchar(64) not null,
   theme                national varchar(64) not null,
   layout_update_id     int(10) unsigned not null default 0,
   primary key (layout_link_id)
)
type = innodb;

alter table core_layout_link
   add unique store_id (store_id, package, theme, layout_update_id);

create table core_layout_update
(
   layout_update_id     int(10) unsigned not null auto_increment,
   handle               national varchar(255),
   xml                  national text,
   primary key (layout_update_id)
)
type = innodb;

create index handle on core_layout_update
(
   handle
);

create table core_resource
(
   code                 national varchar(50) not null,
   version              national varchar(50) not null,
   primary key (code)
)
type = innodb;

alter table core_resource comment 'Resource Version Registry';

create table core_session
(
   session_id           national varchar(255) not null,
   website_id           smallint(5) unsigned,
   session_expires      int(10) unsigned not null default 0,
   session_data         national text not null,
   primary key (session_id)
)
type = innodb;

alter table core_session comment 'Session Data Store';

create table core_store
(
   store_id             smallint(5) unsigned not null auto_increment,
   code                 national varchar(32) not null,
   website_id           smallint(5) unsigned default 0,
   group_id             smallint(5) unsigned not null default 0,
   name                 national varchar(32) not null,
   sort_order           smallint(5) unsigned not null default 0,
   is_active            tinyint(1) unsigned not null default 0,
   primary key (store_id)
)
type = innodb;

alter table core_store comment 'Stores';

alter table core_store
   add unique code (code);

create index is_active on core_store
(

);

create table core_store_group
(
   group_id             smallint(5) unsigned not null auto_increment,
   website_id           smallint(5) unsigned not null default 0,
   name                 national varchar(32) not null,
   root_category_id     int(10) unsigned not null default 0,
   default_store_id     smallint(5) unsigned not null default 0,
   primary key (group_id)
)
type = innodb;

create index default_store_id on core_store_group
(
   default_store_id
);

create table core_translate
(
   key_id               int(10) unsigned not null auto_increment,
   string               national varchar(255) not null,
   store_id             smallint(5) unsigned not null default 0,
   translate            national varchar(255) not null,
   locale               national varchar(20) not null default en_us,
   primary key (key_id)
)
type = innodb;

alter table core_translate comment 'Translation Data';

alter table core_translate
   add unique idx_code (store_id, locale, string);

create table core_url_rewrite
(
   url_rewrite_id       int(10) unsigned not null auto_increment,
   store_id             smallint(5) unsigned not null default 0,
   category_id          int(10) unsigned,
   product_id           int(10) unsigned,
   id_path              national varchar(255) not null,
   request_path         national varchar(255) not null,
   target_path          national varchar(255) not null,
   is_system            tinyint(1) unsigned default 1,
   options              national varchar(255) not null,
   description          national varchar(255),
   primary key (url_rewrite_id)
)
type = innodb;

alter table core_url_rewrite
   add unique unq_path (store_id, id_path, is_system);

alter table core_url_rewrite
   add unique unq_request_path (store_id, request_path);

create index idx_category_rewrite on core_url_rewrite
(

);

create index idx_id_path on core_url_rewrite
(
   id_path
);

create index idx_target_path on core_url_rewrite
(

);

create table core_website
(
   website_id           smallint(5) unsigned not null auto_increment,
   code                 national varchar(32) not null,
   name                 national varchar(64) not null,
   sort_order           smallint(5) unsigned not null default 0,
   default_group_id     smallint(5) unsigned not null default 0,
   is_default           tinyint(1) unsigned default 0,
   primary key (website_id)
)
type = innodb;

alter table core_website comment 'Websites';

alter table core_website
   add unique code (code);

create index default_group_id on core_website
(
   default_group_id
);

create index sort_order on core_website
(
   sort_order
);

create table cron_schedule
(
   schedule_id          int(10) unsigned not null auto_increment,
   job_code             national varchar(255) not null default '0',
   status               national enum('pending','running','success','missed','error') not null default pending,
   messages             national text,
   created_at           datetime not null default 0000-00-00 00:00:00,
   scheduled_at         datetime not null default 0000-00-00 00:00:00,
   executed_at          datetime not null default 0000-00-00 00:00:00,
   finished_at          datetime not null default 0000-00-00 00:00:00,
   primary key (schedule_id)
)
type = innodb;

create index scheduled_at on cron_schedule
(

);

create index task_name on cron_schedule
(
   job_code
);

create table customer_address_entity
(
   entity_id            int(10) unsigned not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_set_id     smallint(5) unsigned not null default 0,
   increment_id         national varchar(50) not null,
   parent_id            int(10) unsigned,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   is_active            tinyint(1) unsigned not null default 1,
   primary key (entity_id)
)
type = innodb;

alter table customer_address_entity comment 'Customer Address Entities';

create table customer_address_entity_datetime
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                datetime not null default 0000-00-00 00:00:00,
   primary key (value_id)
)
type = innodb;

create table customer_address_entity_decimal
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                decimal(12,4) not null default 0.0000,
   primary key (value_id)
)
type = innodb;

create table customer_address_entity_int
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                int(11) not null default 0,
   primary key (value_id)
)
type = innodb;

create table customer_address_entity_text
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national text not null,
   primary key (value_id)
)
type = innodb;

create table customer_address_entity_varchar
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national varchar(255) not null,
   primary key (value_id)
)
type = innodb;

create table customer_entity
(
   entity_id            int(10) unsigned not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_set_id     smallint(5) unsigned not null default 0,
   website_id           smallint(5) unsigned,
   email                national varchar(255) not null,
   group_id             smallint(3) unsigned not null default 0,
   increment_id         national varchar(50) not null,
   store_id             smallint(5) unsigned default 0,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   is_active            tinyint(1) unsigned not null default 1,
   primary key (entity_id)
)
type = innodb;

alter table customer_entity comment 'Customer Entityies';

create index idx_auth on customer_entity
(

);

create index idx_entity_type on customer_entity
(
   entity_type_id
);

create table customer_entity_datetime
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                datetime not null default 0000-00-00 00:00:00,
   primary key (value_id)
)
type = innodb;

create table customer_entity_decimal
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                decimal(12,4) not null default 0.0000,
   primary key (value_id)
)
type = innodb;

create table customer_entity_int
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                int(11) not null default 0,
   primary key (value_id)
)
type = innodb;

create table customer_entity_text
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national text not null,
   primary key (value_id)
)
type = innodb;

create table customer_entity_varchar
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national varchar(255) not null,
   primary key (value_id)
)
type = innodb;

create table customer_group
(
   customer_group_id    smallint(3) unsigned not null auto_increment,
   customer_group_code  national varchar(32) not null,
   tax_class_id         int(10) unsigned not null default 0,
   primary key (customer_group_id)
)
type = innodb;

alter table customer_group comment 'Customer Groups';

create table dataflow_batch
(
   batch_id             int(10) unsigned not null auto_increment,
   profile_id           int(10) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   adapter              national varchar(128),
   params               national text,
   created_at           datetime,
   primary key (batch_id)
)
type = innodb;

create index idx_created_at on dataflow_batch
(
   created_at
);

create table dataflow_batch_export
(
   batch_export_id      bigint(20) unsigned not null auto_increment,
   batch_id             int(10) unsigned not null default 0,
   batch_data           national longtext,
   status               tinyint(3) unsigned not null default 0,
   primary key (batch_export_id)
)
type = innodb;

create table dataflow_batch_import
(
   batch_import_id      bigint(20) unsigned not null auto_increment,
   batch_id             int(10) unsigned not null default 0,
   batch_data           national longtext,
   status               tinyint(3) unsigned not null default 0,
   primary key (batch_import_id)
)
type = innodb;

create table dataflow_import_data
(
   import_id            int(11) not null auto_increment,
   session_id           int(11),
   serial_number        int(11) not null default 0,
   value                national text,
   status               int(1) not null default 0,
   primary key (import_id)
)
type = innodb;

create table dataflow_profile
(
   profile_id           int(10) unsigned not null auto_increment,
   name                 national varchar(255) not null,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   actions_xml          national text,
   gui_data             national text,
   direction            national enum('import','export'),
   entity_type          national varchar(64) not null,
   store_id             smallint(5) unsigned not null default 0,
   data_transfer        national enum('file','interactive'),
   primary key (profile_id)
)
type = innodb;

create table dataflow_profile_history
(
   history_id           int(10) unsigned not null auto_increment,
   profile_id           int(10) unsigned not null default 0,
   action_code          national varchar(64),
   user_id              int(10) unsigned not null default 0,
   performed_at         datetime,
   primary key (history_id)
)
type = innodb;

create table dataflow_session
(
   session_id           int(11) not null auto_increment,
   user_id              int(11) not null,
   created_date         datetime,
   file                 national varchar(255),
   type                 national varchar(32),
   direction            national varchar(32),
   comment              national varchar(255),
   primary key (session_id)
)
type = innodb;

create table design_change
(
   design_change_id     int(11) not null auto_increment,
   store_id             smallint(5) unsigned not null default 0,
   design               national varchar(255) not null,
   date_from            date,
   date_to              date,
   primary key (design_change_id)
)
type = innodb;

create table directory_country
(
   country_id           national varchar(2) not null,
   iso2_code            national varchar(2) not null,
   iso3_code            national varchar(3) not null,
   primary key (country_id)
)
type = innodb;

alter table directory_country comment 'Countries';

create table directory_country_format
(
   country_format_id    int(10) unsigned not null auto_increment,
   country_id           national varchar(2) not null,
   type                 national varchar(30) not null,
   format               national text not null,
   primary key (country_format_id)
)
type = innodb;

alter table directory_country_format comment 'Countries Format';

alter table directory_country_format
   add unique country_type (country_id, type);

create table directory_country_region
(
   region_id            mediumint(8) unsigned not null auto_increment,
   country_id           national varchar(4) not null default '0',
   code                 national varchar(32) not null,
   default_name         national varchar(255),
   primary key (region_id)
)
type = innodb;

alter table directory_country_region comment 'Country Regions';

create index fk_region_country on directory_country_region
(
   country_id
);

create table directory_country_region_name
(
   locale               national varchar(8) not null,
   region_id            mediumint(8) unsigned not null default 0,
   name                 national varchar(64) not null,
   primary key (locale, region_id)
)
type = innodb;

alter table directory_country_region_name comment 'Regions Names';

create table directory_currency_rate
(
   currency_from        national char(3) not null,
   currency_to          national char(3) not null,
   rate                 decimal(24,12) not null default 0.000000000000,
   primary key (currency_from, currency_to)
)
type = innodb;

create index fk_currency_rate_to on directory_currency_rate
(
   currency_to
);

create table eav_attribute
(
   attribute_id         smallint(5) unsigned not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_code       national varchar(255) not null,
   attribute_model      national varchar(255),
   backend_model        national varchar(255),
   backend_type         national enum('static','datetime','decimal','int','text','varchar') not null default static,
   backend_table        national varchar(255),
   frontend_model       national varchar(255),
   frontend_input       national varchar(50),
   frontend_label       national varchar(255),
   frontend_class       national varchar(255),
   source_model         national varchar(255),
   is_global            tinyint(1) unsigned not null default 1,
   is_visible           tinyint(1) unsigned not null default 1,
   is_required          tinyint(1) unsigned not null default 0,
   is_user_defined      tinyint(1) unsigned not null default 0,
   default_value        national text,
   is_searchable        tinyint(1) unsigned not null default 0,
   is_filterable        tinyint(1) unsigned not null default 0,
   is_comparable        tinyint(1) unsigned not null default 0,
   is_visible_on_front  tinyint(1) unsigned not null default 0,
   is_unique            tinyint(1) unsigned not null default 0,
   is_configurable      tinyint(1) unsigned not null default 1,
   apply_to             national varchar(255) not null,
   position             int(11) not null,
   note                 national varchar(255) not null,
   is_visible_in_advanced_search tinyint(1) unsigned not null default 0,
   is_used_for_price_rules tinyint(1) unsigned not null default 1,
   primary key (attribute_id)
)
type = innodb;

alter table eav_attribute
   add unique entity_type_id (entity_type_id, attribute_code);

create table eav_attribute_group
(
   attribute_group_id   smallint(5) unsigned not null auto_increment,
   attribute_set_id     smallint(5) unsigned not null default 0,
   attribute_group_name national varchar(255) not null,
   sort_order           smallint(6) not null default 0,
   default_id           smallint(5) unsigned default 0,
   primary key (attribute_group_id)
)
type = innodb;

alter table eav_attribute_group
   add unique attribute_set_id (attribute_set_id, attribute_group_name);

create index attribute_set_id_2 on eav_attribute_group
(

);

create table eav_attribute_option
(
   option_id            int(10) unsigned not null auto_increment,
   attribute_id         smallint(5) unsigned not null default 0,
   sort_order           smallint(5) unsigned not null default 0,
   primary key (option_id)
)
type = innodb;

alter table eav_attribute_option comment 'Attributes Option (For Source Model)';

create table eav_attribute_option_value
(
   value_id             int(10) unsigned not null auto_increment,
   option_id            int(10) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   value                national varchar(255) not null,
   primary key (value_id)
)
type = innodb;

alter table eav_attribute_option_value comment 'Attribute Option Values Per Store';

create table eav_attribute_set
(
   attribute_set_id     smallint(5) unsigned not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_set_name   national varchar(255) not null,
   sort_order           smallint(6) not null default 0,
   primary key (attribute_set_id)
)
type = innodb;

alter table eav_attribute_set
   add unique entity_type_id (entity_type_id, attribute_set_name);

create index entity_type_id_2 on eav_attribute_set
(

);

create table eav_entity
(
   entity_id            int(10) unsigned not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_set_id     smallint(5) unsigned not null default 0,
   increment_id         national varchar(50) not null,
   parent_id            int(11) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   is_active            tinyint(1) unsigned not null default 1,
   primary key (entity_id)
)
type = innodb;

alter table eav_entity comment 'Entityies';

create table eav_entity_attribute
(
   entity_attribute_id  int(10) unsigned not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_set_id     smallint(5) unsigned not null default 0,
   attribute_group_id   smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   sort_order           smallint(6) not null default 0,
   primary key (entity_attribute_id)
)
type = innodb;

alter table eav_entity_attribute
   add unique attribute_group_id (attribute_group_id, attribute_id);

alter table eav_entity_attribute
   add unique attribute_set_id_2 (attribute_set_id, attribute_id);

create index attribute_set_id_3 on eav_entity_attribute
(

);

create table eav_entity_datetime
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                datetime not null default 0000-00-00 00:00:00,
   primary key (value_id)
)
type = innodb;

alter table eav_entity_datetime comment 'Datetime Values Of Attributes';

create index fk_attribute_datetime_attribute on eav_entity_datetime
(
   attribute_id
);

create index value_by_attribute on eav_entity_datetime
(

);

create index value_by_entity_type on eav_entity_datetime
(

);

create table eav_entity_decimal
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                decimal(12,4) not null default 0.0000,
   primary key (value_id)
)
type = innodb;

alter table eav_entity_decimal comment 'Decimal Values Of Attributes';

create index fk_attribute_decimal_attribute on eav_entity_decimal
(
   attribute_id
);

create index value_by_attribute on eav_entity_decimal
(

);

create index value_by_entity_type on eav_entity_decimal
(

);

create table eav_entity_int
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                int(11) not null default 0,
   primary key (value_id)
)
type = innodb;

alter table eav_entity_int comment 'Integer Values Of Attributes';

create index fk_attribute_int_attribute on eav_entity_int
(
   attribute_id
);

create index value_by_attribute on eav_entity_int
(

);

create index value_by_entity_type on eav_entity_int
(

);

create table eav_entity_store
(
   entity_store_id      int(10) unsigned not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   increment_prefix     national varchar(20) not null,
   increment_last_id    national varchar(50) not null,
   primary key (entity_store_id)
)
type = innodb;

create table eav_entity_text
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national text not null,
   primary key (value_id)
)
type = innodb;

alter table eav_entity_text comment 'Text Values Of Attributes';

create index fk_attribute_text_attribute on eav_entity_text
(
   attribute_id
);

create table eav_entity_type
(
   entity_type_id       smallint(5) unsigned not null auto_increment,
   entity_type_code     national varchar(50) not null,
   entity_model         national varchar(255) not null,
   attribute_model      national varchar(255) not null,
   entity_table         national varchar(255) not null,
   value_table_prefix   national varchar(255) not null,
   entity_id_field      national varchar(255) not null,
   is_data_sharing      tinyint(4) unsigned not null default 1,
   data_sharing_key     national varchar(100) default default,
   default_attribute_set_id smallint(5) unsigned not null default 0,
   increment_model      national varchar(255) not null,
   increment_per_store  tinyint(1) unsigned not null default 0,
   increment_pad_length tinyint(8) unsigned not null default 8,
   increment_pad_char   national char(1) not null default '0',
   primary key (entity_type_id)
)
type = innodb;

create index entity_name on eav_entity_type
(
   entity_type_code
);

create table eav_entity_varchar
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national varchar(255) not null,
   primary key (value_id)
)
type = innodb;

alter table eav_entity_varchar comment 'Varchar Values Of Attributes';

create index fk_attribute_varchar_attribute on eav_entity_varchar
(
   attribute_id
);

create index value_by_attribute on eav_entity_varchar
(

);

create index value_by_entity_type on eav_entity_varchar
(

);

create table gift_message
(
   gift_message_id      int(7) unsigned not null auto_increment,
   customer_id          int(7) unsigned not null default 0,
   sender               national varchar(255) not null,
   recipient            national varchar(255) not null,
   message              national text not null,
   primary key (gift_message_id)
)
type = innodb;

create table googlecheckout_api_debug
(
   debug_id             int(10) unsigned not null auto_increment,
   dir                  national enum('in','out'),
   url                  national varchar(255),
   request_body         national text,
   response_body        national text,
   primary key (debug_id)
)
type = innodb;

create table log_customer
(
   log_id               int(10) unsigned not null auto_increment,
   visitor_id           bigint(20) unsigned,
   customer_id          int(11) not null default 0,
   login_at             datetime not null default 0000-00-00 00:00:00,
   logout_at            datetime,
   store_id             smallint(5) unsigned not null,
   primary key (log_id)
)
type = innodb;

alter table log_customer comment 'Customers log information';

create table log_quote
(
   quote_id             int(10) unsigned not null default 0,
   visitor_id           bigint(20) unsigned,
   created_at           datetime not null default 0000-00-00 00:00:00,
   deleted_at           datetime,
   primary key (quote_id)
)
type = innodb;

alter table log_quote comment 'Quote log data';

create table log_summary
(
   summary_id           bigint(20) unsigned not null auto_increment,
   store_id             smallint(5) unsigned not null,
   type_id              smallint(5) unsigned,
   visitor_count        int(11) not null default 0,
   customer_count       int(11) not null default 0,
   add_date             datetime not null default 0000-00-00 00:00:00,
   primary key (summary_id)
)
type = innodb;

alter table log_summary comment 'Summary log information';

create table log_summary_type
(
   type_id              smallint(5) unsigned not null auto_increment,
   type_code            national varchar(64) not null,
   period               smallint(5) unsigned not null default 0,
   period_type          national enum('minute','hour','day','week','month') not null default minute,
   primary key (type_id)
)
type = innodb;

alter table log_summary_type comment 'Type of summary information';

create table log_url
(
   url_id               bigint(20) unsigned not null default 0,
   visitor_id           bigint(20) unsigned,
   visit_time           datetime not null default 0000-00-00 00:00:00
)
type = innodb;

alter table log_url comment 'URL visiting history';

create table log_url_info
(
   url_id               bigint(20) unsigned not null auto_increment,
   url                  national varchar(255) not null,
   referer              national varchar(255),
   primary key (url_id)
)
type = innodb;

alter table log_url_info comment 'Detale information about url visit';

create table log_visitor
(
   visitor_id           bigint(20) unsigned not null auto_increment,
   session_id           national char(64) not null,
   first_visit_at       datetime,
   last_visit_at        datetime not null default 0000-00-00 00:00:00,
   last_url_id          bigint(20) unsigned not null default 0,
   store_id             smallint(5) unsigned not null,
   primary key (visitor_id)
)
type = innodb;

alter table log_visitor comment 'System visitors log';

create table log_visitor_info
(
   visitor_id           bigint(20) unsigned not null default 0,
   http_referer         national varchar(255),
   http_user_agent      national varchar(255),
   http_accept_charset  national varchar(255),
   http_accept_language national varchar(255),
   server_addr          bigint(20),
   remote_addr          bigint(20),
   primary key (visitor_id)
)
type = innodb;

alter table log_visitor_info comment 'Additional information by visitor';

create table newsletter_problem
(
   problem_id           int(7) unsigned not null auto_increment,
   subscriber_id        int(7) unsigned,
   queue_id             int(7) unsigned not null default 0,
   problem_error_code   int(3) unsigned default 0,
   problem_error_text   national varchar(200),
   primary key (problem_id)
)
type = innodb;

alter table newsletter_problem comment 'Newsletter Problems';

create table newsletter_queue
(
   queue_id             int(7) unsigned not null auto_increment,
   template_id          int(7) unsigned not null default 0,
   queue_status         int(3) unsigned not null default 0,
   queue_start_at       datetime,
   queue_finish_at      datetime,
   primary key (queue_id)
)
type = innodb;

alter table newsletter_queue comment 'Newsletter Queue';

create table newsletter_queue_link
(
   queue_link_id        int(9) unsigned not null auto_increment,
   queue_id             int(7) unsigned not null default 0,
   subscriber_id        int(7) unsigned not null default 0,
   letter_sent_at       datetime,
   primary key (queue_link_id)
)
type = innodb;

alter table newsletter_queue_link comment 'Newsletter Queue To Subscriber Link';

create table newsletter_queue_store_link
(
   queue_id             int(7) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   primary key (queue_id, store_id)
)
type = innodb;

create table newsletter_subscriber
(
   subscriber_id        int(7) unsigned not null auto_increment,
   store_id             smallint(5) unsigned default 0,
   change_status_at     datetime,
   customer_id          int(11) unsigned not null default 0,
   subscriber_email     varchar(150) not null,
   subscriber_status    int(3) not null default 0,
   subscriber_confirm_code national varchar(32) default null,
   primary key (subscriber_id)
)
type = innodb;

alter table newsletter_subscriber comment 'Newsletter Subscribers';

create index fk_subscriber_customer on newsletter_subscriber
(
   customer_id
);

create table newsletter_template
(
   template_id          int(7) unsigned not null auto_increment,
   template_code        national varchar(150),
   template_text        national text,
   template_text_preprocessed national text,
   template_type        int(3) unsigned,
   template_subject     national varchar(200),
   template_sender_name national varchar(200),
   template_sender_email varchar(200),
   template_actual      tinyint(1) unsigned default 1,
   added_at             datetime,
   modified_at          datetime,
   primary key (template_id)
)
type = innodb;

alter table newsletter_template comment 'Newsletter Templates';

create index added_at on newsletter_template
(
   added_at
);

create index modified_at on newsletter_template
(
   modified_at
);

create index template_actual on newsletter_template
(
   template_actual
);

create table paygate_authorizenet_debug
(
   debug_id             int(10) unsigned not null auto_increment,
   request_body         national text,
   response_body        national text,
   request_serialized   national text,
   result_serialized    national text,
   request_dump         national text,
   result_dump          national text,
   primary key (debug_id)
)
type = innodb;

create table paypal_api_debug
(
   debug_id             int(10) unsigned not null auto_increment,
   debug_at             timestamp not null default current_timestamp,
   request_body         national text,
   response_body        national text,
   primary key (debug_id)
);

create index debug_at on paypal_api_debug
(
   debug_at
);

create table paypaluk_api_debug
(
   debug_id             int(10) unsigned not null auto_increment,
   debug_at             timestamp not null default current_timestamp,
   request_body         national text,
   response_body        national text,
   primary key (debug_id)
)
type = innodb;

create index debug_at on paypaluk_api_debug
(
   debug_at
);

create table poll
(
   poll_id              int(10) unsigned not null auto_increment,
   poll_title           national varchar(255) not null,
   votes_count          int(10) unsigned not null default 0,
   store_id             smallint(5) unsigned default 0,
   date_posted          datetime not null default 0000-00-00 00:00:00,
   date_closed          datetime,
   active               smallint(6) not null default 1,
   closed               tinyint(1) not null default 0,
   answers_display      smallint(6),
   primary key (poll_id)
)
type = innodb;

create table poll_answer
(
   answer_id            int(10) unsigned not null auto_increment,
   poll_id              int(10) unsigned not null default 0,
   answer_title         national varchar(255) not null,
   votes_count          int(10) unsigned not null default 0,
   answer_order         smallint(6) not null default 0,
   primary key (answer_id)
)
type = innodb;

create table poll_store
(
   poll_id              int(10) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   primary key (poll_id, store_id)
)
type = innodb;

create table poll_vote
(
   vote_id              int(10) unsigned not null auto_increment,
   poll_id              int(10) unsigned not null default 0,
   poll_answer_id       int(10) unsigned not null default 0,
   ip_address           bigint(20),
   customer_id          int(11),
   vote_time            timestamp not null default current_timestamp,
   primary key (vote_id)
)
type = innodb;

create table product_alert_price
(
   alert_price_id       int(10) unsigned not null auto_increment,
   customer_id          int(10) unsigned not null default 0,
   product_id           int(10) unsigned not null default 0,
   price                decimal(12,4) not null default 0.0000,
   website_id           smallint(5) unsigned not null default 0,
   add_date             datetime not null default 0000-00-00 00:00:00,
   last_send_date       datetime,
   send_count           smallint(5) unsigned not null default 0,
   status               tinyint(3) unsigned not null default 0,
   primary key (alert_price_id)
)
type = innodb;

create table product_alert_stock
(
   alert_stock_id       int(10) unsigned not null auto_increment,
   customer_id          int(10) unsigned not null default 0,
   product_id           int(10) unsigned not null default 0,
   website_id           smallint(5) unsigned not null default 0,
   add_date             datetime not null default 0000-00-00 00:00:00,
   send_date            datetime,
   send_count           smallint(5) unsigned not null default 0,
   status               tinyint(3) unsigned not null default 0,
   primary key (alert_stock_id)
)
type = innodb;

create table rating
(
   rating_id            smallint(6) unsigned not null auto_increment,
   entity_id            smallint(6) unsigned not null default 0,
   rating_code          national varchar(64) not null,
   position             tinyint(3) unsigned not null default 0,
   primary key (rating_id)
)
type = innodb;

alter table rating comment 'Ratings';

alter table rating
   add unique idx_code (rating_code);

create table rating_entity
(
   entity_id            smallint(6) unsigned not null auto_increment,
   entity_code          national varchar(64) not null,
   primary key (entity_id)
)
type = innodb;

alter table rating_entity comment 'Rating Entities';

alter table rating_entity
   add unique idx_code (entity_code);

create table rating_option
(
   option_id            int(10) unsigned not null auto_increment,
   rating_id            smallint(6) unsigned not null default 0,
   code                 national varchar(32) not null,
   value                tinyint(3) unsigned not null default 0,
   position             tinyint(3) unsigned not null default 0,
   primary key (option_id)
)
type = innodb;

alter table rating_option comment 'Rating Options';

create table rating_option_vote
(
   vote_id              bigint(20) unsigned not null auto_increment,
   option_id            int(10) unsigned not null default 0,
   remote_ip            national varchar(16) not null,
   remote_ip_long       int(11) not null default 0,
   customer_id          int(11) unsigned default 0,
   entity_pk_value      bigint(20) unsigned not null default 0,
   rating_id            smallint(6) unsigned not null default 0,
   review_id            bigint(20) unsigned,
   percent              tinyint(3) not null default 0,
   value                tinyint(3) not null default 0,
   primary key (vote_id)
)
type = innodb;

alter table rating_option_vote comment 'Rating Option Values';

create table rating_option_vote_aggregated
(
   primary_id           int(11) not null auto_increment,
   rating_id            smallint(6) unsigned not null default 0,
   entity_pk_value      bigint(20) unsigned not null default 0,
   vote_count           int(10) unsigned not null default 0,
   vote_value_sum       int(10) unsigned not null default 0,
   percent              tinyint(3) not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   primary key (primary_id)
)
type = innodb;

create table rating_store
(
   rating_id            smallint(6) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   primary key (rating_id, store_id)
)
type = innodb;

create table rating_title
(
   rating_id            smallint(6) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   value                national varchar(255) not null,
   primary key (rating_id, store_id)
)
type = innodb;

create table report_event
(
   event_id             bigint(20) unsigned not null auto_increment,
   logged_at            datetime not null default 0000-00-00 00:00:00,
   event_type_id        smallint(6) unsigned not null default 0,
   object_id            int(10) unsigned not null default 0,
   subject_id           int(10) unsigned not null default 0,
   subtype              tinyint(3) unsigned not null default 0,
   store_id             smallint(5) unsigned not null,
   primary key (event_id)
)
type = innodb;

create index idx_object on report_event
(
   object_id
);

create index idx_subject on report_event
(
   subject_id
);

create index idx_subtype on report_event
(
   subtype
);

create table report_event_types
(
   event_type_id        smallint(6) unsigned not null auto_increment,
   event_name           national varchar(64) not null,
   customer_login       tinyint(3) unsigned not null default 0,
   primary key (event_type_id)
)
type = innodb;

create table review
(
   review_id            bigint(20) unsigned not null auto_increment,
   created_at           datetime not null default 0000-00-00 00:00:00,
   entity_id            smallint(5) unsigned not null default 0,
   entity_pk_value      int(10) unsigned not null default 0,
   status_id            tinyint(3) unsigned not null default 0,
   primary key (review_id)
)
type = innodb;

alter table review comment 'Review Base Information';

create table review_detail
(
   detail_id            bigint(20) unsigned not null auto_increment,
   review_id            bigint(20) unsigned not null default 0,
   store_id             smallint(5) unsigned default 0,
   title                national varchar(255) not null,
   detail               national text not null,
   nickname             national varchar(128) not null,
   customer_id          int(10) unsigned,
   primary key (detail_id)
)
type = innodb;

alter table review_detail comment 'Review Detail Information';

create table review_entity
(
   entity_id            smallint(5) unsigned not null auto_increment,
   entity_code          national varchar(32) not null,
   primary key (entity_id)
)
type = innodb;

alter table review_entity comment 'Review Entities';

create table review_entity_summary
(
   primary_id           bigint(20) not null auto_increment,
   entity_pk_value      bigint(20) not null default 0,
   entity_type          tinyint(4) not null default 0,
   reviews_count        smallint(6) not null default 0,
   rating_summary       tinyint(4) not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   primary key (primary_id)
)
type = innodb;

create table review_status
(
   status_id            tinyint(3) unsigned not null auto_increment,
   status_code          national varchar(32) not null,
   primary key (status_id)
)
type = innodb;

alter table review_status comment 'Review Statuses';

create table review_store
(
   review_id            bigint(20) unsigned not null,
   store_id             smallint(5) unsigned not null,
   primary key (review_id, store_id)
)
type = innodb;

create table sales_flat_order_item
(
   item_id              int(10) unsigned not null auto_increment,
   order_id             int(10) unsigned not null default 0,
   parent_item_id       int(10) unsigned,
   quote_item_id        int(10) unsigned,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   product_id           int(10) unsigned,
   product_type         national varchar(255),
   product_options      national text,
   weight               decimal(12,4) default 0.0000,
   is_virtual           tinyint(1) unsigned,
   sku                  national varchar(255) not null,
   name                 national varchar(255),
   description          national text,
   applied_rule_ids     national text,
   additional_data      national text,
   free_shipping        tinyint(1) unsigned not null default 0,
   is_qty_decimal       tinyint(1) unsigned,
   no_discount          tinyint(1) unsigned default 0,
   qty_backordered      decimal(12,4) default 0.0000,
   qty_canceled         decimal(12,4) default 0.0000,
   qty_invoiced         decimal(12,4) default 0.0000,
   qty_ordered          decimal(12,4) default 0.0000,
   qty_refunded         decimal(12,4) default 0.0000,
   qty_shipped          decimal(12,4) default 0.0000,
   cost                 decimal(12,4) default 0.0000,
   price                decimal(12,4) not null default 0.0000,
   base_price           decimal(12,4) not null default 0.0000,
   original_price       decimal(12,4),
   base_original_price  decimal(12,4),
   tax_percent          decimal(12,4) default 0.0000,
   tax_amount           decimal(12,4) default 0.0000,
   base_tax_amount      decimal(12,4) default 0.0000,
   tax_invoiced         decimal(12,4) default 0.0000,
   base_tax_invoiced    decimal(12,4) default 0.0000,
   discount_percent     decimal(12,4) default 0.0000,
   discount_amount      decimal(12,4) default 0.0000,
   base_discount_amount decimal(12,4) default 0.0000,
   discount_invoiced    decimal(12,4) default 0.0000,
   base_discount_invoiced decimal(12,4) default 0.0000,
   amount_refunded      decimal(12,4) default 0.0000,
   base_amount_refunded decimal(12,4) default 0.0000,
   row_total            decimal(12,4) not null default 0.0000,
   base_row_total       decimal(12,4) not null default 0.0000,
   row_invoiced         decimal(12,4) not null default 0.0000,
   base_row_invoiced    decimal(12,4) not null default 0.0000,
   row_weight           decimal(12,4) default 0.0000,
   gift_message_id      int(10),
   gift_message_available int(10),
   base_tax_before_discount national varchar(255),
   tax_before_discount  national varchar(255),
   primary key (item_id)
)
type = innodb type = innodb;

create index idx_order on sales_flat_order_item
(
   order_id
);

create table sales_flat_quote
(
   entity_id            int(10) unsigned not null auto_increment,
   store_id             smallint(5) unsigned not null default 0,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   converted_at         datetime not null default 0000-00-00 00:00:00,
   is_active            tinyint(1) unsigned default 1,
   is_virtual           tinyint(1) unsigned default 0,
   is_multi_shipping    tinyint(1) unsigned default 0,
   items_count          int(10) unsigned default 0,
   items_qty            decimal(12,4) default 0.0000,
   orig_order_id        int(10) unsigned default 0,
   store_to_base_rate   decimal(12,4) default 0.0000,
   store_to_quote_rate  decimal(12,4) default 0.0000,
   base_currency_code   national varchar(255),
   store_currency_code  national varchar(255),
   quote_currency_code  national varchar(255),
   grand_total          decimal(12,4) default 0.0000,
   base_grand_total     decimal(12,4) default 0.0000,
   checkout_method      national varchar(255),
   customer_id          int(10) unsigned default 0,
   customer_tax_class_id int(10) unsigned default 0,
   customer_group_id    int(10) unsigned default 0,
   customer_email       national varchar(255),
   customer_prefix      national varchar(40),
   customer_firstname   national varchar(255),
   customer_middlename  national varchar(40),
   customer_lastname    national varchar(255),
   customer_suffix      national varchar(40),
   customer_dob         datetime,
   customer_note        national varchar(255),
   customer_note_notify tinyint(1) unsigned default 1,
   customer_is_guest    tinyint(1) unsigned default 0,
   remote_ip            national varchar(32),
   applied_rule_ids     national varchar(255),
   reserved_order_id    national varchar(64),
   password_hash        national varchar(255),
   coupon_code          national varchar(255),
   quote_status_id      national varchar(255),
   billing_address_id   national varchar(255),
   custbalance_amount   national varchar(255),
   is_multi_payment     national varchar(255),
   customer_taxvat      national varchar(255),
   subtotal             national varchar(255),
   base_subtotal        national varchar(255),
   subtotal_with_discount national varchar(255),
   base_subtotal_with_discount national varchar(255),
   gift_message_id      national varchar(255),
   primary key (entity_id)
)
type = innodb type = innodb;

create index fk_sales_quote_store on sales_flat_quote
(
   store_id
);

create index idx_customer on sales_flat_quote
(

);

create table sales_flat_quote_address
(
   address_id           int(10) unsigned not null auto_increment,
   quote_id             int(10) unsigned not null default 0,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   customer_id          int(10) unsigned,
   save_in_address_book tinyint(1) default 0,
   customer_address_id  int(10) unsigned,
   address_type         national varchar(255),
   email                national varchar(255),
   prefix               national varchar(40),
   firstname            national varchar(255),
   middlename           national varchar(40),
   lastname             national varchar(255),
   suffix               national varchar(40),
   company              national varchar(255),
   street               national varchar(255),
   city                 national varchar(255),
   region               national varchar(255),
   region_id            int(10) unsigned,
   postcode             national varchar(255),
   country_id           national varchar(255),
   telephone            national varchar(255),
   fax                  national varchar(255),
   same_as_billing      tinyint(1) unsigned not null default 0,
   free_shipping        tinyint(1) unsigned not null default 0,
   collect_shipping_rates tinyint(1) unsigned not null default 0,
   shipping_method      national varchar(255) not null,
   shipping_description national varchar(255) not null,
   weight               decimal(12,4) not null default 0.0000,
   subtotal             decimal(12,4) not null default 0.0000,
   base_subtotal        decimal(12,4) not null default 0.0000,
   subtotal_with_discount decimal(12,4) not null default 0.0000,
   base_subtotal_with_discount decimal(12,4) not null default 0.0000,
   tax_amount           decimal(12,4) not null default 0.0000,
   base_tax_amount      decimal(12,4) not null default 0.0000,
   shipping_amount      decimal(12,4) not null default 0.0000,
   base_shipping_amount decimal(12,4) not null default 0.0000,
   shipping_tax_amount  decimal(12,4),
   base_shipping_tax_amount decimal(12,4),
   discount_amount      decimal(12,4) not null default 0.0000,
   base_discount_amount decimal(12,4) not null default 0.0000,
   grand_total          decimal(12,4) not null default 0.0000,
   base_grand_total     decimal(12,4) not null default 0.0000,
   customer_notes       national text,
   entity_id            national varchar(255),
   parent_id            national varchar(255),
   custbalance_amount   national varchar(255),
   base_custbalance_amount national varchar(255),
   applied_taxes        national text,
   gift_message_id      national varchar(255),
   primary key (address_id)
)
type = innodb type = innodb;

create table sales_flat_quote_address_item
(
   address_item_id      int(10) unsigned not null auto_increment,
   parent_item_id       int(10) unsigned,
   quote_address_id     int(10) unsigned not null default 0,
   quote_item_id        int(10) unsigned not null default 0,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   applied_rule_ids     national text,
   additional_data      national text,
   weight               decimal(12,4) default 0.0000,
   qty                  decimal(12,4) not null default 0.0000,
   discount_amount      decimal(12,4) default 0.0000,
   tax_amount           decimal(12,4) default 0.0000,
   row_total            decimal(12,4) not null default 0.0000,
   base_row_total       decimal(12,4) not null default 0.0000,
   row_total_with_discount decimal(12,4) default 0.0000,
   base_discount_amount decimal(12,4) default 0.0000,
   base_tax_amount      decimal(12,4) default 0.0000,
   row_weight           decimal(12,4) default 0.0000,
   parent_id            national varchar(255),
   product_id           national varchar(255),
   super_product_id     national varchar(255),
   parent_product_id    national varchar(255),
   sku                  national varchar(255),
   image                national varchar(255),
   name                 national varchar(255),
   description          national varchar(255),
   free_shipping        national varchar(255),
   is_qty_decimal       national varchar(255),
   price                national varchar(255),
   discount_percent     national varchar(255),
   no_discount          national varchar(255),
   tax_percent          national varchar(255),
   base_price           national varchar(255),
   gift_message_id      national varchar(255),
   primary key (address_item_id)
)
type = innodb type = innodb;

create table sales_flat_quote_item
(
   item_id              int(10) unsigned not null auto_increment,
   quote_id             int(10) unsigned not null default 0,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   product_id           int(10) unsigned,
   parent_item_id       int(10) unsigned,
   is_virtual           tinyint(1) unsigned,
   sku                  national varchar(255) not null,
   name                 national varchar(255),
   description          national text,
   applied_rule_ids     national text,
   additional_data      national text,
   free_shipping        tinyint(1) unsigned not null default 0,
   is_qty_decimal       tinyint(1) unsigned,
   no_discount          tinyint(1) unsigned default 0,
   weight               decimal(12,4) default 0.0000,
   qty                  decimal(12,4) not null default 0.0000,
   price                decimal(12,4) not null default 0.0000,
   base_price           decimal(12,4) not null default 0.0000,
   custom_price         decimal(12,4),
   discount_percent     decimal(12,4) default 0.0000,
   discount_amount      decimal(12,4) default 0.0000,
   base_discount_amount decimal(12,4) default 0.0000,
   tax_percent          decimal(12,4) default 0.0000,
   tax_amount           decimal(12,4) default 0.0000,
   base_tax_amount      decimal(12,4) default 0.0000,
   row_total            decimal(12,4) not null default 0.0000,
   base_row_total       decimal(12,4) not null default 0.0000,
   row_total_with_discount decimal(12,4) default 0.0000,
   row_weight           decimal(12,4) default 0.0000,
   parent_id            national varchar(255),
   product_type         national varchar(255),
   base_tax_before_discount national varchar(255),
   tax_before_discount  national varchar(255),
   original_custom_price national varchar(255),
   gift_message_id      national varchar(255),
   primary key (item_id)
)
type = innodb type = innodb;

create table sales_flat_quote_item_option
(
   option_id            int(10) unsigned not null auto_increment,
   item_id              int(10) unsigned not null,
   product_id           int(10) unsigned not null,
   code                 national varchar(255) not null,
   value                national text not null,
   primary key (option_id)
)
type = innodb type = innodb;

alter table sales_flat_quote_item_option comment 'Additional Options For Quote Item';

create table sales_flat_quote_payment
(
   payment_id           int(10) unsigned not null auto_increment,
   quote_id             int(10) unsigned not null default 0,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   method               national varchar(255),
   cc_type              national varchar(255),
   cc_number_enc        national varchar(255),
   cc_last4             national varchar(255),
   cc_cid_enc           national varchar(255),
   cc_owner             national varchar(255),
   cc_exp_month         tinyint(2) unsigned default 0,
   cc_exp_year          smallint(4) unsigned default 0,
   cc_ss_owner          national varchar(255),
   cc_ss_start_month    tinyint(2) unsigned default 0,
   cc_ss_start_year     smallint(4) unsigned default 0,
   cybersource_token    national varchar(255),
   paypal_correlation_id national varchar(255),
   paypal_payer_id      national varchar(255),
   paypal_payer_status  national varchar(255),
   po_number            national varchar(255),
   parent_id            national varchar(255),
   additional_data      national varchar(255),
   cc_ss_issue          national varchar(255),
   primary key (payment_id)
)
type = innodb type = innodb;

create table sales_flat_quote_shipping_rate
(
   rate_id              int(10) unsigned not null auto_increment,
   address_id           int(10) unsigned not null default 0,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   carrier              national varchar(255),
   carrier_title        national varchar(255),
   code                 national varchar(255),
   method               national varchar(255),
   method_description   national text,
   price                decimal(12,4) not null default 0.0000,
   parent_id            national varchar(255),
   error_message        national varchar(255),
   primary key (rate_id)
)
type = innodb type = innodb;

create table sales_order
(
   entity_id            int(10) unsigned not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_set_id     smallint(5) unsigned not null default 0,
   increment_id         national varchar(50) not null,
   parent_id            int(10) unsigned not null default 0,
   store_id             smallint(5) unsigned,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   is_active            tinyint(1) unsigned not null default 1,
   customer_id          int(11),
   tax_amount           decimal(12,4) not null default 0.0000,
   shipping_amount      decimal(12,4) not null default 0.0000,
   discount_amount      decimal(12,4) not null default 0.0000,
   subtotal             decimal(12,4) not null default 0.0000,
   grand_total          decimal(12,4) not null default 0.0000,
   total_paid           decimal(12,4) not null default 0.0000,
   total_refunded       decimal(12,4) not null default 0.0000,
   total_qty_ordered    decimal(12,4) not null default 0.0000,
   total_canceled       decimal(12,4) not null default 0.0000,
   total_invoiced       decimal(12,4) not null default 0.0000,
   total_online_refunded decimal(12,4) not null default 0.0000,
   total_offline_refunded decimal(12,4) not null default 0.0000,
   base_tax_amount      decimal(12,4) not null default 0.0000,
   base_shipping_amount decimal(12,4) not null default 0.0000,
   base_discount_amount decimal(12,4) not null default 0.0000,
   base_subtotal        decimal(12,4) not null default 0.0000,
   base_grand_total     decimal(12,4) not null default 0.0000,
   base_total_paid      decimal(12,4) not null default 0.0000,
   base_total_refunded  decimal(12,4) not null default 0.0000,
   base_total_qty_ordered decimal(12,4) not null default 0.0000,
   base_total_canceled  decimal(12,4) not null default 0.0000,
   base_total_invoiced  decimal(12,4) not null default 0.0000,
   base_total_online_refunded decimal(12,4) not null default 0.0000,
   base_total_offline_refunded decimal(12,4) not null default 0.0000,
   subtotal_refunded    decimal(12,4),
   subtotal_canceled    decimal(12,4),
   tax_refunded         decimal(12,4),
   tax_canceled         decimal(12,4),
   shipping_refunded    decimal(12,4),
   shipping_canceled    decimal(12,4),
   base_subtotal_refunded decimal(12,4),
   base_subtotal_canceled decimal(12,4),
   base_tax_refunded    decimal(12,4),
   base_tax_canceled    decimal(12,4),
   base_shipping_refunded decimal(12,4),
   base_shipping_canceled decimal(12,4),
   subtotal_invoiced    decimal(12,4),
   tax_invoiced         decimal(12,4),
   shipping_invoiced    decimal(12,4),
   base_subtotal_invoiced decimal(12,4),
   base_tax_invoiced    decimal(12,4),
   base_shipping_invoiced decimal(12,4),
   shipping_tax_amount  decimal(12,4),
   base_shipping_tax_amount decimal(12,4),
   primary key (entity_id)
)
type = innodb;

create index idx_customer on sales_order
(
   customer_id
);

create table sales_order_datetime
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                datetime not null default 0000-00-00 00:00:00,
   primary key (value_id)
)
type = innodb;

create table sales_order_decimal
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                decimal(12,4) not null default 0.0000,
   primary key (value_id)
)
type = innodb;

create table sales_order_entity
(
   entity_id            int(10) unsigned not null auto_increment,
   entity_type_id       smallint(8) unsigned not null default 0,
   attribute_set_id     smallint(5) unsigned not null default 0,
   increment_id         national varchar(50) not null,
   parent_id            int(10) unsigned not null default 0,
   store_id             smallint(5) unsigned,
   created_at           datetime not null default 0000-00-00 00:00:00,
   updated_at           datetime not null default 0000-00-00 00:00:00,
   is_active            tinyint(1) unsigned not null default 1,
   primary key (entity_id)
)
type = innodb;

create table sales_order_entity_datetime
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                datetime not null default 0000-00-00 00:00:00,
   primary key (value_id)
)
type = innodb;

create table sales_order_entity_decimal
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                decimal(12,4) not null default 0.0000,
   primary key (value_id)
)
type = innodb;

create table sales_order_entity_int
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                int(11) not null default 0,
   primary key (value_id)
)
type = innodb;

create table sales_order_entity_text
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national text not null,
   primary key (value_id)
)
type = innodb;

create table sales_order_entity_varchar
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national varchar(255) not null,
   primary key (value_id)
)
type = innodb;

create table sales_order_int
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                int(11) not null default 0,
   primary key (value_id)
)
type = innodb;

create table sales_order_tax
(
   tax_id               int(10) unsigned not null auto_increment,
   order_id             int(10) unsigned not null,
   code                 national varchar(255) not null,
   title                national varchar(255) not null,
   percent              decimal(12,4) not null,
   amount               decimal(12,4) not null,
   priority             int(11) not null,
   position             int(11) not null,
   base_amount          decimal(12,4) not null,
   process              smallint(6) not null,
   base_real_amount     decimal(12,4) not null,
   primary key (tax_id)
)
type = innodb type = innodb;

create index idx_order_tax on sales_order_tax
(

);

create table sales_order_text
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national text not null,
   primary key (value_id)
)
type = innodb;

create table sales_order_varchar
(
   value_id             int(11) not null auto_increment,
   entity_type_id       smallint(5) unsigned not null default 0,
   attribute_id         smallint(5) unsigned not null default 0,
   entity_id            int(10) unsigned not null default 0,
   value                national varchar(255) not null,
   primary key (value_id)
)
type = innodb;

create table salesrule
(
   rule_id              int(10) unsigned not null auto_increment,
   name                 national varchar(255) not null,
   description          national text not null,
   from_date            date default 0000-00-00,
   to_date              date default 0000-00-00,
   coupon_code          national varchar(255),
   uses_per_coupon      int(11) not null default 0,
   uses_per_customer    int(11) not null default 0,
   customer_group_ids   national varchar(255) not null,
   is_active            tinyint(1) not null default 0,
   conditions_serialized national text not null,
   actions_serialized   national text not null,
   stop_rules_processing tinyint(1) not null default 1,
   is_advanced          tinyint(3) unsigned not null default 1,
   product_ids          national text,
   sort_order           int(10) unsigned not null default 0,
   simple_action        national varchar(32) not null,
   discount_amount      decimal(12,4) not null default 0.0000,
   discount_qty         decimal(12,4) unsigned,
   discount_step        int(10) unsigned not null,
   simple_free_shipping tinyint(1) unsigned not null default 0,
   times_used           int(11) unsigned not null default 0,
   is_rss               tinyint(4) not null default 0,
   website_ids          national text,
   primary key (rule_id)
)
type = innodb;

create index sort_order on salesrule
(

);

create table salesrule_customer
(
   rule_customer_id     int(10) unsigned not null auto_increment,
   rule_id              int(10) unsigned not null default 0,
   customer_id          int(10) unsigned not null default 0,
   times_used           smallint(11) unsigned not null default 0,
   primary key (rule_customer_id)
)
type = innodb;

create index customer_id on salesrule_customer
(

);

create index rule_id on salesrule_customer
(

);

create table sendfriend_log
(
   log_id               int(11) not null auto_increment,
   ip                   int(11) not null default 0,
   time                 int(11) not null default 0,
   primary key (log_id)
)
type = innodb;

alter table sendfriend_log comment 'Send to friend function log storage table';

create index ip on sendfriend_log
(
   ip
);

create index time on sendfriend_log
(
   time
);

create table shipping_tablerate
(
   pk                   int(10) unsigned not null auto_increment,
   website_id           int(11) not null default 0,
   dest_country_id      national varchar(4) not null default '0',
   dest_region_id       int(10) not null default 0,
   dest_zip             national varchar(10) not null,
   condition_name       national varchar(20) not null,
   condition_value      decimal(12,4) not null default 0.0000,
   price                decimal(12,4) not null default 0.0000,
   cost                 decimal(12,4) not null default 0.0000,
   primary key (pk)
)
type = innodb;

alter table shipping_tablerate
   add unique dest_country (website_id, dest_country_id, dest_region_id, dest_zip, condition_name, condition_value);

create table sitemap
(
   sitemap_id           int(11) not null auto_increment,
   sitemap_type         national varchar(32),
   sitemap_filename     national varchar(32),
   sitemap_path         national tinytext,
   sitemap_time         timestamp,
   store_id             smallint(5) unsigned not null,
   primary key (sitemap_id)
)
type = innodb;

create table tag
(
   tag_id               int(11) unsigned not null auto_increment,
   name                 national varchar(255) not null,
   status               smallint(6) not null default 0,
   primary key (tag_id)
)
type = innodb;

create table tag_relation
(
   tag_relation_id      int(11) unsigned not null auto_increment,
   tag_id               int(11) unsigned not null default 0,
   customer_id          int(10) unsigned not null default 0,
   product_id           int(11) unsigned not null default 0,
   store_id             smallint(6) unsigned not null default 1,
   active               tinyint(1) unsigned not null default 1,
   created_at           datetime,
   primary key (tag_relation_id)
)
type = innodb;

create table tag_summary
(
   tag_id               int(11) unsigned not null default 0,
   store_id             smallint(5) unsigned not null default 0,
   customers            int(11) unsigned not null default 0,
   products             int(11) unsigned not null default 0,
   uses                 int(11) unsigned not null default 0,
   historical_uses      int(11) unsigned not null default 0,
   popularity           int(11) unsigned not null default 0,
   primary key (tag_id, store_id)
)
type = innodb;

create table tax_calculation
(
   tax_calculation_rate_id int(11) not null,
   tax_calculation_rule_id int(11) not null,
   customer_tax_class_id smallint(6) not null,
   product_tax_class_id smallint(6) not null
)
type = innodb type = innodb;

create index idx_tax_calculation on tax_calculation
(

);

create table tax_calculation_rate
(
   tax_calculation_rate_id int(11) not null auto_increment,
   tax_country_id       national char(2) not null,
   tax_region_id        mediumint(9) not null,
   tax_postcode         national varchar(12) not null,
   code                 national varchar(255) not null,
   rate                 decimal(12,4) not null,
   primary key (tax_calculation_rate_id)
)
type = innodb type = innodb;

create index idx_tax_calculation_rate on tax_calculation_rate
(

);

create index idx_tax_calculation_rate_code on tax_calculation_rate
(
   code
);

create table tax_calculation_rate_title
(
   tax_calculation_rate_title_id int(11) not null auto_increment,
   tax_calculation_rate_id int(11) not null,
   store_id             smallint(5) unsigned not null,
   value                national varchar(255) not null,
   primary key (tax_calculation_rate_title_id)
)
type = innodb type = innodb;

create index idx_tax_calculation_rate_title on tax_calculation_rate_title
(

);

create table tax_calculation_rule
(
   tax_calculation_rule_id int(11) not null auto_increment,
   code                 national varchar(255) not null,
   priority             mediumint(9) not null,
   position             mediumint(9) not null,
   primary key (tax_calculation_rule_id)
)
type = innodb type = innodb;

create index idx_tax_calculation_rule on tax_calculation_rule
(

);

create index idx_tax_calculation_rule_code on tax_calculation_rule
(
   code
);

create table tax_class
(
   class_id             smallint(6) not null auto_increment,
   class_name           national varchar(255) not null,
   class_type           national enum('customer','product') not null default customer,
   primary key (class_id)
)
type = innodb;

create table wishlist
(
   wishlist_id          int(10) unsigned not null auto_increment,
   customer_id          int(10) unsigned not null default 0,
   shared               tinyint(1) unsigned default 0,
   sharing_code         varchar(32) not null,
   primary key (wishlist_id)
)
type = innodb;

alter table wishlist comment 'Wishlist Main';

alter table wishlist
   add unique fk_customer (customer_id);

create table wishlist_item
(
   wishlist_item_id     int(10) unsigned not null auto_increment,
   wishlist_id          int(10) unsigned not null default 0,
   product_id           int(10) unsigned not null default 0,
   store_id             smallint(5) unsigned not null,
   added_at             datetime,
   description          national text,
   primary key (wishlist_item_id)
)
type = innodb;

alter table wishlist_item comment 'Wishlist Items';

alter table admin_rule add constraint fk_admin_rule foreign key (role_id)
      references admin_role (role_id);

alter table api_rule add constraint fk_api_rule foreign key (role_id)
      references api_role (role_id);

alter table catalog_category_entity_datetime add constraint fk_catalog_category_entity_datetime_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalog_category_entity_datetime add constraint fk_catalog_category_entity_datetime_entity foreign key (entity_id)
      references catalog_category_entity (entity_id);

alter table catalog_category_entity_datetime add constraint fk_catalog_category_entity_datetime_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_category_entity_decimal add constraint fk_catalog_category_entity_decimal_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalog_category_entity_decimal add constraint fk_catalog_category_entity_decimal_entity foreign key (entity_id)
      references catalog_category_entity (entity_id);

alter table catalog_category_entity_decimal add constraint fk_catalog_category_entity_decimal_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_category_entity_int add constraint fk_catalog_category_emtity_int_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalog_category_entity_int add constraint fk_catalog_category_emtity_int_entity foreign key (entity_id)
      references catalog_category_entity (entity_id);

alter table catalog_category_entity_int add constraint fk_catalog_category_emtity_int_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_category_entity_text add constraint fk_catalog_category_entity_text_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalog_category_entity_text add constraint fk_catalog_category_entity_text_entity foreign key (entity_id)
      references catalog_category_entity (entity_id);

alter table catalog_category_entity_text add constraint fk_catalog_category_entity_text_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_category_entity_varchar add constraint fk_catalog_category_entity_varchar_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalog_category_entity_varchar add constraint fk_catalog_category_entity_varchar_entity foreign key (entity_id)
      references catalog_category_entity (entity_id);

alter table catalog_category_entity_varchar add constraint fk_catalog_category_entity_varchar_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_category_product add constraint catalog_category_product_category foreign key (category_id)
      references catalog_category_entity (entity_id);

alter table catalog_category_product add constraint catalog_category_product_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table catalog_category_product_index add constraint fk_catalog_category_product_index_category_entity foreign key (category_id)
      references catalog_category_entity (entity_id);

alter table catalog_category_product_index add constraint fk_catalog_category_product_index_product_entity foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table catalog_compare_item add constraint fk_catalog_compare_item_customer foreign key (customer_id)
      references customer_entity (entity_id);

alter table catalog_compare_item add constraint fk_catalog_compare_item_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_bundle_option add constraint fk_catalog_product_bundle_option_parent foreign key (parent_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_bundle_option_value add constraint fk_catalog_product_bundle_option_value_option foreign key (option_id)
      references catalog_product_bundle_option (option_id);

alter table catalog_product_bundle_selection add constraint fk_catalog_product_bundle_selection_option foreign key (option_id)
      references catalog_product_bundle_option (option_id);

alter table catalog_product_bundle_selection add constraint fk_catalog_product_bundle_selection_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_enabled_index add constraint fk_catalog_product_enabled_index_product_entity foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_enabled_index add constraint fk_catalog_product_enabled_index_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_product_entity add constraint fk_catalog_product_entity_attribute_set_id foreign key (attribute_set_id)
      references eav_attribute_set (attribute_set_id);

alter table catalog_product_entity add constraint fk_catalog_product_entity_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table catalog_product_entity_datetime add constraint fk_catalog_product_entity_datetime_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalog_product_entity_datetime add constraint fk_catalog_product_entity_datetime_product_entity foreign key (entity_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_entity_datetime add constraint fk_catalog_product_entity_datetime_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_product_entity_decimal add constraint fk_catalog_product_entity_decimal_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalog_product_entity_decimal add constraint fk_catalog_product_entity_decimal_product_entity foreign key (entity_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_entity_decimal add constraint fk_catalog_product_entity_decimal_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_product_entity_gallery add constraint fk_catalog_product_entity_gallery_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalog_product_entity_gallery add constraint fk_catalog_product_entity_gallery_entity foreign key (entity_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_entity_gallery add constraint fk_catalog_product_entity_gallery_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_product_entity_int add constraint fk_catalog_product_entity_int_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalog_product_entity_int add constraint fk_catalog_product_entity_int_product_entity foreign key (entity_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_entity_int add constraint fk_catalog_product_entity_int_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_product_entity_media_gallery add constraint fk_catalog_product_media_gallery_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalog_product_entity_media_gallery add constraint fk_catalog_product_media_gallery_entity foreign key (entity_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_entity_media_gallery_value add constraint fk_catalog_product_media_gallery_value_gallery foreign key (value_id)
      references catalog_product_entity_media_gallery (value_id);

alter table catalog_product_entity_media_gallery_value add constraint fk_catalog_product_media_gallery_value_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_product_entity_text add constraint fk_catalog_product_entity_text_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalog_product_entity_text add constraint fk_catalog_product_entity_text_product_entity foreign key (entity_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_entity_text add constraint fk_catalog_product_entity_text_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_product_entity_tier_price add constraint fk_catalog_product_entity_tier_price_product_entity foreign key (entity_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_entity_tier_price add constraint fk_catalog_product_tier_website foreign key (website_id)
      references core_website (website_id);

alter table catalog_product_entity_tier_price add constraint fk_catalog_product_entity_tier_price_group foreign key (customer_group_id)
      references customer_group (customer_group_id);

alter table catalog_product_entity_varchar add constraint fk_catalog_product_entity_varchar_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalog_product_entity_varchar add constraint fk_catalog_product_entity_varchar_product_entity foreign key (entity_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_entity_varchar add constraint fk_catalog_product_entity_varchar_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_product_link add constraint fk_product_link_linked_product foreign key (linked_product_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_link add constraint fk_product_link_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_link add constraint fk_product_link_type foreign key (link_type_id)
      references catalog_product_link_type (link_type_id);

alter table catalog_product_link_attribute add constraint fk_attribute_product_link_type foreign key (link_type_id)
      references catalog_product_link_type (link_type_id);

alter table catalog_product_link_attribute_decimal add constraint fk_decimal_link foreign key (link_id)
      references catalog_product_link (link_id);

alter table catalog_product_link_attribute_decimal add constraint fk_decimal_product_link_attribute foreign key (product_link_attribute_id)
      references catalog_product_link_attribute (product_link_attribute_id);

alter table catalog_product_link_attribute_int add constraint fk_int_product_link foreign key (link_id)
      references catalog_product_link (link_id);

alter table catalog_product_link_attribute_int add constraint fk_int_product_link_attribute foreign key (product_link_attribute_id)
      references catalog_product_link_attribute (product_link_attribute_id);

alter table catalog_product_link_attribute_varchar add constraint fk_varchar_link foreign key (link_id)
      references catalog_product_link (link_id);

alter table catalog_product_link_attribute_varchar add constraint fk_varchar_product_link_attribute foreign key (product_link_attribute_id)
      references catalog_product_link_attribute (product_link_attribute_id);

alter table catalog_product_option add constraint fk_catalog_product_option_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_option_price add constraint fk_catalog_product_option_price_option foreign key (option_id)
      references catalog_product_option (option_id);

alter table catalog_product_option_price add constraint fk_catalog_product_option_price_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_product_option_title add constraint fk_catalog_product_option_title_option foreign key (option_id)
      references catalog_product_option (option_id);

alter table catalog_product_option_title add constraint fk_catalog_product_option_title_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_product_option_type_price add constraint fk_catalog_product_option_type_price_option foreign key (option_type_id)
      references catalog_product_option_type_value (option_type_id);

alter table catalog_product_option_type_price add constraint fk_catalog_product_option_type_price_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_product_option_type_title add constraint fk_catalog_product_option_type_title_option foreign key (option_type_id)
      references catalog_product_option_type_value (option_type_id);

alter table catalog_product_option_type_title add constraint fk_catalog_product_option_type_title_store foreign key (store_id)
      references core_store (store_id);

alter table catalog_product_option_type_value add constraint fk_catalog_product_option_type_value_option foreign key (option_id)
      references catalog_product_option (option_id);

alter table catalog_product_super_attribute add constraint fk_super_product_attribute_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_super_attribute_label add constraint fk_super_product_attribute_label foreign key (product_super_attribute_id)
      references catalog_product_super_attribute (product_super_attribute_id);

alter table catalog_product_super_attribute_pricing add constraint fk_super_product_attribute_pricing foreign key (product_super_attribute_id)
      references catalog_product_super_attribute (product_super_attribute_id);

alter table catalog_product_super_link add constraint fk_super_product_link_entity foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_super_link add constraint fk_super_product_link_parent foreign key (parent_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_website add constraint fk_catalog_product_website_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table catalog_product_website add constraint fk_cataolog_product_website_website foreign key (website_id)
      references core_website (website_id);

alter table catalogindex_eav add constraint fk_catalogindex_eav_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalogindex_eav add constraint fk_catalogindex_eav_entity foreign key (entity_id)
      references catalog_product_entity (entity_id);

alter table catalogindex_eav add constraint fk_catalogindex_eav_store foreign key (store_id)
      references core_store (store_id);

alter table catalogindex_minimal_price add constraint fk_catalogindex_minimal_price_customer_group foreign key (customer_group_id)
      references customer_group (customer_group_id);

alter table catalogindex_minimal_price add constraint fk_catalogindex_minimal_price_entity foreign key (entity_id)
      references catalog_product_entity (entity_id);

alter table catalogindex_minimal_price add constraint fk_catalogindex_minimal_price_store foreign key (store_id)
      references core_store (store_id);

alter table catalogindex_price add constraint fk_catalogindex_price_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table catalogindex_price add constraint fk_catalogindex_price_entity foreign key (entity_id)
      references catalog_product_entity (entity_id);

alter table catalogindex_price add constraint fk_catalogindex_price_store foreign key (store_id)
      references core_store (store_id);

alter table cataloginventory_stock_item add constraint fk_cataloginventory_stock_item_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table cataloginventory_stock_item add constraint fk_cataloginventory_stock_item_stock foreign key (stock_id)
      references cataloginventory_stock (stock_id);

alter table catalogrule_product add constraint fk_catalogrule_product_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table catalogrule_product add constraint fk_catalogrule_product_customergroup foreign key (customer_group_id)
      references customer_group (customer_group_id);

alter table catalogrule_product add constraint fk_catalogrule_product_rule foreign key (rule_id)
      references catalogrule (rule_id);

alter table catalogrule_product add constraint fk_catalogrule_product_website foreign key (website_id)
      references core_website (website_id);

alter table catalogrule_product_price add constraint fk_catalogrule_product_price_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table catalogrule_product_price add constraint fk_catalogrule_product_price_customergroup foreign key (customer_group_id)
      references customer_group (customer_group_id);

alter table catalogrule_product_price add constraint fk_catalogrule_product_price_website foreign key (website_id)
      references core_website (website_id);

alter table catalogsearch_query add constraint fk_catalogsearch_query foreign key (store_id)
      references core_store (store_id);

alter table checkout_agreement_store add constraint fk_checkout_agreement foreign key (agreement_id)
      references checkout_agreement (agreement_id);

alter table checkout_agreement_store add constraint fk_checkout_agreement_store foreign key (store_id)
      references core_store (store_id);

alter table cms_block_store add constraint fk_cms_block_store_block foreign key (block_id)
      references cms_block (block_id);

alter table cms_block_store add constraint fk_cms_block_store_store foreign key (store_id)
      references core_store (store_id);

alter table cms_page_store add constraint fk_cms_page_store_page foreign key (page_id)
      references cms_page (page_id);

alter table cms_page_store add constraint fk_cms_page_store_store foreign key (store_id)
      references core_store (store_id);

alter table core_layout_link add constraint fk_core_layout_link_store foreign key (store_id)
      references core_store (store_id);

alter table core_layout_link add constraint fk_core_layout_link_update foreign key (layout_update_id)
      references core_layout_update (layout_update_id);

alter table core_session add constraint fk_session_website foreign key (website_id)
      references core_website (website_id);

alter table core_store add constraint fk_store_group_store foreign key (group_id)
      references core_store_group (group_id);

alter table core_store add constraint fk_store_website foreign key (website_id)
      references core_website (website_id);

alter table core_store_group add constraint fk_store_group_website foreign key (website_id)
      references core_website (website_id);

alter table core_translate add constraint fk_core_translate_store foreign key (store_id)
      references core_store (store_id);

alter table core_url_rewrite add constraint fk_core_url_rewrite_category foreign key (category_id)
      references catalog_category_entity (entity_id);

alter table core_url_rewrite add constraint fk_core_url_rewrite_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table core_url_rewrite add constraint fk_core_url_rewrite_store foreign key (store_id)
      references core_store (store_id);

alter table customer_address_entity add constraint fk_customer_address_customer_id foreign key (parent_id)
      references customer_entity (entity_id);

alter table customer_address_entity_datetime add constraint fk_customer_address_datetime_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table customer_address_entity_datetime add constraint fk_customer_address_datetime_entity foreign key (entity_id)
      references customer_address_entity (entity_id);

alter table customer_address_entity_datetime add constraint fk_customer_address_datetime_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table customer_address_entity_decimal add constraint fk_customer_address_decimal_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table customer_address_entity_decimal add constraint fk_customer_address_decimal_entity foreign key (entity_id)
      references customer_address_entity (entity_id);

alter table customer_address_entity_decimal add constraint fk_customer_address_decimal_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table customer_address_entity_int add constraint fk_customer_address_int_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table customer_address_entity_int add constraint fk_customer_address_int_entity foreign key (entity_id)
      references customer_address_entity (entity_id);

alter table customer_address_entity_int add constraint fk_customer_address_int_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table customer_address_entity_text add constraint fk_customer_address_text_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table customer_address_entity_text add constraint fk_customer_address_text_entity foreign key (entity_id)
      references customer_address_entity (entity_id);

alter table customer_address_entity_text add constraint fk_customer_address_text_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table customer_address_entity_varchar add constraint fk_customer_address_varchar_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table customer_address_entity_varchar add constraint fk_customer_address_varchar_entity foreign key (entity_id)
      references customer_address_entity (entity_id);

alter table customer_address_entity_varchar add constraint fk_customer_address_varchar_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table customer_entity add constraint fk_customer_entity_store foreign key (store_id)
      references core_store (store_id);

alter table customer_entity add constraint fk_customer_website foreign key (website_id)
      references core_website (website_id);

alter table customer_entity_datetime add constraint fk_customer_datetime_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table customer_entity_datetime add constraint fk_customer_datetime_entity foreign key (entity_id)
      references customer_entity (entity_id);

alter table customer_entity_datetime add constraint fk_customer_datetime_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table customer_entity_decimal add constraint fk_customer_decimal_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table customer_entity_decimal add constraint fk_customer_decimal_entity foreign key (entity_id)
      references customer_entity (entity_id);

alter table customer_entity_decimal add constraint fk_customer_decimal_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table customer_entity_int add constraint fk_customer_int_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table customer_entity_int add constraint fk_customer_int_entity foreign key (entity_id)
      references customer_entity (entity_id);

alter table customer_entity_int add constraint fk_customer_int_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table customer_entity_text add constraint fk_customer_text_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table customer_entity_text add constraint fk_customer_text_entity foreign key (entity_id)
      references customer_entity (entity_id);

alter table customer_entity_text add constraint fk_customer_text_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table customer_entity_varchar add constraint fk_customer_varchar_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table customer_entity_varchar add constraint fk_customer_varchar_entity foreign key (entity_id)
      references customer_entity (entity_id);

alter table customer_entity_varchar add constraint fk_customer_varchar_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table dataflow_batch add constraint fk_dataflow_batch_profile foreign key (profile_id)
      references dataflow_profile (profile_id);

alter table dataflow_batch add constraint fk_dataflow_batch_store foreign key (store_id)
      references core_store (store_id);

alter table dataflow_batch_export add constraint fk_dataflow_batch_export_batch foreign key (batch_id)
      references dataflow_batch (batch_id);

alter table dataflow_batch_import add constraint fk_dataflow_batch_import_batch foreign key (batch_id)
      references dataflow_batch (batch_id);

alter table dataflow_import_data add constraint fk_dataflow_import_data foreign key (session_id)
      references dataflow_session (session_id);

alter table dataflow_profile_history add constraint fk_dataflow_profile_history foreign key (profile_id)
      references dataflow_profile (profile_id);

alter table design_change add constraint fk_design_change_store foreign key (store_id)
      references core_store (store_id);

alter table directory_country_region_name add constraint fk_directory_region_name_region foreign key (region_id)
      references directory_country_region (region_id);

alter table eav_attribute add constraint fk_eav_attribute foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table eav_attribute_group add constraint fk_eav_attribute_group foreign key (attribute_set_id)
      references eav_attribute_set (attribute_set_id);

alter table eav_attribute_option add constraint fk_attribute_option_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table eav_attribute_option_value add constraint fk_attribute_option_value_option foreign key (option_id)
      references eav_attribute_option (option_id);

alter table eav_attribute_option_value add constraint fk_attribute_option_value_store foreign key (store_id)
      references core_store (store_id);

alter table eav_attribute_set add constraint fk_eav_attribute_set foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table eav_entity add constraint fk_eav_entity foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table eav_entity add constraint fk_eav_entity_store foreign key (store_id)
      references core_store (store_id);

alter table eav_entity_attribute add constraint fk_eav_entity_attrivute_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table eav_entity_attribute add constraint fk_eav_entity_attrivute_group foreign key (attribute_group_id)
      references eav_attribute_group (attribute_group_id);

alter table eav_entity_attribute add constraint fk_eav_entity_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table eav_entity_attribute add constraint fk_eav_entity_attribute_group foreign key (attribute_group_id)
      references eav_attribute_group (attribute_group_id);

alter table eav_entity_datetime add constraint fk_eav_entity_datetime_entity foreign key (entity_id)
      references eav_entity (entity_id);

alter table eav_entity_datetime add constraint fk_eav_entity_datetime_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table eav_entity_datetime add constraint fk_eav_entity_datetime_store foreign key (store_id)
      references core_store (store_id);

alter table eav_entity_decimal add constraint fk_eav_entity_decimal_entity foreign key (entity_id)
      references eav_entity (entity_id);

alter table eav_entity_decimal add constraint fk_eav_entity_decimal_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table eav_entity_decimal add constraint fk_eav_entity_decimal_store foreign key (store_id)
      references core_store (store_id);

alter table eav_entity_int add constraint fk_eav_entity_int_entity foreign key (entity_id)
      references eav_entity (entity_id);

alter table eav_entity_int add constraint fk_eav_entity_int_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table eav_entity_int add constraint fk_eav_entity_int_store foreign key (store_id)
      references core_store (store_id);

alter table eav_entity_store add constraint fk_eav_entity_store_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table eav_entity_store add constraint fk_eav_entity_store_store foreign key (store_id)
      references core_store (store_id);

alter table eav_entity_text add constraint fk_eav_entity_text_entity foreign key (entity_id)
      references eav_entity (entity_id);

alter table eav_entity_text add constraint fk_eav_entity_text_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table eav_entity_text add constraint fk_eav_entity_text_store foreign key (store_id)
      references core_store (store_id);

alter table eav_entity_varchar add constraint fk_eav_entity_varchar_entity foreign key (entity_id)
      references eav_entity (entity_id);

alter table eav_entity_varchar add constraint fk_eav_entity_varchar_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table eav_entity_varchar add constraint fk_eav_entity_varchar_store foreign key (store_id)
      references core_store (store_id);

alter table newsletter_problem add constraint fk_problem_queue foreign key (queue_id)
      references newsletter_queue (queue_id);

alter table newsletter_problem add constraint fk_problem_subscriber foreign key (subscriber_id)
      references newsletter_subscriber (subscriber_id);

alter table newsletter_queue add constraint fk_queue_template foreign key (template_id)
      references newsletter_template (template_id);

alter table newsletter_queue_link add constraint fk_queue_link_queue foreign key (queue_id)
      references newsletter_queue (queue_id);

alter table newsletter_queue_link add constraint fk_queue_link_subscriber foreign key (subscriber_id)
      references newsletter_subscriber (subscriber_id);

alter table newsletter_queue_store_link add constraint fk_link_queue foreign key (queue_id)
      references newsletter_queue (queue_id);

alter table newsletter_queue_store_link add constraint fk_newsletter_queue_store_link_store foreign key (store_id)
      references core_store (store_id);

alter table newsletter_subscriber add constraint fk_newsletter_subscriber_store foreign key (store_id)
      references core_store (store_id);

alter table poll add constraint fk_poll_store foreign key (store_id)
      references core_store (store_id);

alter table poll_answer add constraint fk_poll_parent foreign key (poll_id)
      references poll (poll_id);

alter table poll_store add constraint fk_poll_store_poll foreign key (poll_id)
      references poll (poll_id);

alter table poll_store add constraint fk_poll_store_store foreign key (store_id)
      references core_store (store_id);

alter table poll_vote add constraint fk_poll_answer foreign key (poll_answer_id)
      references poll_answer (answer_id);

alter table product_alert_price add constraint fk_product_alert_price_customer foreign key (customer_id)
      references customer_entity (entity_id);

alter table product_alert_price add constraint fk_product_alert_price_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table product_alert_price add constraint fk_product_alert_price_website foreign key (website_id)
      references core_website (website_id);

alter table product_alert_stock add constraint fk_product_alert_stock_customer foreign key (customer_id)
      references customer_entity (entity_id);

alter table product_alert_stock add constraint fk_product_alert_stock_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table product_alert_stock add constraint fk_product_alert_stock_website foreign key (website_id)
      references core_website (website_id);

alter table rating add constraint fk_rating_entity_key foreign key (entity_id)
      references rating_entity (entity_id);

alter table rating_option add constraint fk_rating_option_rating foreign key (rating_id)
      references rating (rating_id);

alter table rating_option_vote add constraint fk_rating_option_value_option foreign key (option_id)
      references rating_option (option_id);

alter table rating_option_vote_aggregated add constraint fk_rating_option_value_aggregate foreign key (rating_id)
      references rating (rating_id);

alter table rating_option_vote_aggregated add constraint fk_rating_option_vote_aggregated_store foreign key (store_id)
      references core_store (store_id);

alter table rating_store add constraint fk_rating_store_rating foreign key (rating_id)
      references rating (rating_id);

alter table rating_store add constraint fk_rating_store_store foreign key (store_id)
      references core_store (store_id);

alter table rating_title add constraint fk_rating_title foreign key (rating_id)
      references rating (rating_id);

alter table rating_title add constraint fk_rating_title_store foreign key (store_id)
      references core_store (store_id);

alter table report_event add constraint fk_report_event_store foreign key (store_id)
      references core_store (store_id);

alter table report_event add constraint fk_report_event_type foreign key (event_type_id)
      references report_event_types (event_type_id);

alter table review add constraint fk_review_entity foreign key (entity_id)
      references review_entity (entity_id);

alter table review add constraint fk_review_parent_product foreign key (entity_pk_value)
      references catalog_product_entity (entity_id);

alter table review add constraint fk_review_status foreign key (status_id)
      references review_status (status_id);

alter table review_detail add constraint fk_review_detail_review foreign key (review_id)
      references review (review_id);

alter table review_detail add constraint fk_review_detail_store foreign key (store_id)
      references core_store (store_id);

alter table review_entity_summary add constraint fk_review_entity_summary_store foreign key (store_id)
      references core_store (store_id);

alter table review_store add constraint fk_review_store_review foreign key (review_id)
      references review (review_id);

alter table review_store add constraint fk_review_store_store foreign key (store_id)
      references core_store (store_id);

alter table sales_flat_quote_address add constraint fk_sales_quote_address_sales_quote foreign key (quote_id)
      references sales_flat_quote (entity_id);

alter table sales_flat_quote_address_item add constraint fk_quote_address_item_quote_address foreign key (quote_address_id)
      references sales_flat_quote_address (address_id);

alter table sales_flat_quote_address_item add constraint fk_sales_flat_quote_address_item_parent foreign key (parent_item_id)
      references sales_flat_quote_address_item (address_item_id);

alter table sales_flat_quote_address_item add constraint fk_sales_quote_address_item_quote_item foreign key (quote_item_id)
      references sales_flat_quote_item (item_id);

alter table sales_flat_quote_item add constraint fk_sales_flat_quote_item_parent_item foreign key (parent_item_id)
      references sales_flat_quote_item (item_id);

alter table sales_flat_quote_item add constraint fk_sales_quote_item_catalog_product_entity foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table sales_flat_quote_item add constraint fk_sales_quote_item_sales_quote foreign key (quote_id)
      references sales_flat_quote (entity_id);

alter table sales_flat_quote_item_option add constraint fk_sales_quote_item_option_item_id foreign key (item_id)
      references sales_flat_quote_item (item_id);

alter table sales_flat_quote_payment add constraint fk_sales_quote_payment_sales_quote foreign key (quote_id)
      references sales_flat_quote (entity_id);

alter table sales_flat_quote_shipping_rate add constraint fk_sales_quote_shipping_rate_address foreign key (address_id)
      references sales_flat_quote_address (address_id);

alter table sales_order add constraint fk_sale_order_store foreign key (store_id)
      references core_store (store_id);

alter table sales_order add constraint fk_sale_order_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table sales_order_datetime add constraint fk_sales_order_datetime foreign key (entity_id)
      references sales_order (entity_id);

alter table sales_order_datetime add constraint fk_sales_order_datetime_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table sales_order_datetime add constraint fk_sales_order_datetime_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table sales_order_decimal add constraint fk_sales_order_decimal foreign key (entity_id)
      references sales_order (entity_id);

alter table sales_order_decimal add constraint fk_sales_order_decimal_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table sales_order_decimal add constraint fk_sales_order_decimal_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table sales_order_entity add constraint fk_sale_order_entity_store foreign key (store_id)
      references core_store (store_id);

alter table sales_order_entity add constraint fk_sales_order_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table sales_order_entity_datetime add constraint fk_sales_order_entity_datetime foreign key (entity_id)
      references sales_order_entity (entity_id);

alter table sales_order_entity_datetime add constraint fk_sales_order_entity_datetime_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table sales_order_entity_datetime add constraint fk_sales_order_entity_datetime_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table sales_order_entity_decimal add constraint fk_sales_order_entity_decimal foreign key (entity_id)
      references sales_order_entity (entity_id);

alter table sales_order_entity_decimal add constraint fk_sales_order_entity_decimal_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table sales_order_entity_decimal add constraint fk_sales_order_entity_decimal_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table sales_order_entity_int add constraint fk_sales_order_entity_int foreign key (entity_id)
      references sales_order_entity (entity_id);

alter table sales_order_entity_int add constraint fk_sales_order_entity_int_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table sales_order_entity_int add constraint fk_sales_order_entity_int_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table sales_order_entity_text add constraint fk_sales_order_entity_text foreign key (entity_id)
      references sales_order_entity (entity_id);

alter table sales_order_entity_text add constraint fk_sales_order_entity_text_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table sales_order_entity_text add constraint fk_sales_order_entity_text_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table sales_order_entity_varchar add constraint fk_sales_order_entity_varchar foreign key (entity_id)
      references sales_order_entity (entity_id);

alter table sales_order_entity_varchar add constraint fk_sales_order_entity_varchar_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table sales_order_entity_varchar add constraint fk_sales_order_entity_varchar_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table sales_order_int add constraint fk_sales_order_int foreign key (entity_id)
      references sales_order (entity_id);

alter table sales_order_int add constraint fk_sales_order_int_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table sales_order_int add constraint fk_sales_order_int_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table sales_order_text add constraint fk_sales_order_text foreign key (entity_id)
      references sales_order (entity_id);

alter table sales_order_text add constraint fk_sales_order_text_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table sales_order_text add constraint fk_sales_order_text_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table sales_order_varchar add constraint fk_sales_order_varchar foreign key (entity_id)
      references sales_order (entity_id);

alter table sales_order_varchar add constraint fk_sales_order_varchar_attribute foreign key (attribute_id)
      references eav_attribute (attribute_id);

alter table sales_order_varchar add constraint fk_sales_order_varchar_entity_type foreign key (entity_type_id)
      references eav_entity_type (entity_type_id);

alter table salesrule_customer add constraint fk_salesrule_customer_id foreign key (customer_id)
      references customer_entity (entity_id);

alter table salesrule_customer add constraint fk_salesrule_customer_rule foreign key (rule_id)
      references salesrule (rule_id);

alter table sitemap add constraint fk_sitemap_store foreign key (store_id)
      references core_store (store_id);

alter table tag_relation add constraint fk_tag_relation_product foreign key (product_id)
      references catalog_product_entity (entity_id);

alter table tag_relation add constraint tag_relation_ibfk_1 foreign key (tag_id)
      references tag (tag_id);

alter table tag_relation add constraint tag_relation_ibfk_2 foreign key (customer_id)
      references customer_entity (entity_id);

alter table tag_relation add constraint tag_relation_ibfk_4 foreign key (store_id)
      references core_store (store_id);

alter table tag_summary add constraint fk_tag_summary_store foreign key (store_id)
      references core_store (store_id);

alter table tag_summary add constraint tag_summary_tag foreign key (tag_id)
      references tag (tag_id);

alter table tax_calculation add constraint fk_tax_calculation_ctc foreign key (customer_tax_class_id)
      references tax_class (class_id);

alter table tax_calculation add constraint fk_tax_calculation_ptc foreign key (product_tax_class_id)
      references tax_class (class_id);

alter table tax_calculation add constraint fk_tax_calculation_rate foreign key (tax_calculation_rate_id)
      references tax_calculation_rate (tax_calculation_rate_id);

alter table tax_calculation add constraint fk_tax_calculation_rule foreign key (tax_calculation_rule_id)
      references tax_calculation_rule (tax_calculation_rule_id);

alter table tax_calculation_rate_title add constraint fk_tax_calculation_rate_title_rate foreign key (tax_calculation_rate_id)
      references tax_calculation_rate (tax_calculation_rate_id);

alter table tax_calculation_rate_title add constraint fk_tax_calculation_rate_title_store foreign key (store_id)
      references core_store (store_id);

alter table wishlist_item add constraint fk_item_wishlist foreign key (wishlist_id)
      references wishlist (wishlist_id);

alter table wishlist_item add constraint fk_wishlist_item_store foreign key (store_id)
      references core_store (store_id);

alter table wishlist_item add constraint fk_wishlist_product foreign key (product_id)
      references catalog_product_entity (entity_id);
