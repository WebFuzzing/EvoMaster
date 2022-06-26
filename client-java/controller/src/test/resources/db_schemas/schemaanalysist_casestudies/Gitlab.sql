CREATE TABLE `events` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `target_type` varchar(255) DEFAULT NULL,
  `target_id` int(11) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `data` text,
  `project_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `action` int(11) DEFAULT NULL,
  `author_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_events_on_action` (`action`),
  KEY `index_events_on_author_id` (`author_id`),
  KEY `index_events_on_created_at` (`created_at`),
  KEY `index_events_on_project_id` (`project_id`),
  KEY `index_events_on_target_id` (`target_id`),
  KEY `index_events_on_target_type` (`target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `issues` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) DEFAULT NULL,
  `assignee_id` int(11) DEFAULT NULL,
  `author_id` int(11) DEFAULT NULL,
  `project_id` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `position` int(11) DEFAULT '0',
  `branch_name` varchar(255) DEFAULT NULL,
  `description` text,
  `milestone_id` int(11) DEFAULT NULL,
  `state` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_issues_on_assignee_id` (`assignee_id`),
  KEY `index_issues_on_author_id` (`author_id`),
  KEY `index_issues_on_created_at` (`created_at`),
  KEY `index_issues_on_milestone_id` (`milestone_id`),
  KEY `index_issues_on_project_id` (`project_id`),
  KEY `index_issues_on_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `keys` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `key` text,
  `title` varchar(255) DEFAULT NULL,
  `identifier` varchar(255) DEFAULT NULL,
  `project_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_keys_on_identifier` (`identifier`),
  KEY `index_keys_on_project_id` (`project_id`),
  KEY `index_keys_on_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `merge_requests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `target_branch` varchar(255) NOT NULL,
  `source_branch` varchar(255) NOT NULL,
  `project_id` int(11) NOT NULL,
  `author_id` int(11) DEFAULT NULL,
  `assignee_id` int(11) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `st_commits` text,
  `st_diffs` text,
  `milestone_id` int(11) DEFAULT NULL,
  `state` varchar(255) DEFAULT NULL,
  `merge_status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_merge_requests_on_assignee_id` (`assignee_id`),
  KEY `index_merge_requests_on_author_id` (`author_id`),
  KEY `index_merge_requests_on_created_at` (`created_at`),
  KEY `index_merge_requests_on_milestone_id` (`milestone_id`),
  KEY `index_merge_requests_on_project_id` (`project_id`),
  KEY `index_merge_requests_on_source_branch` (`source_branch`),
  KEY `index_merge_requests_on_target_branch` (`target_branch`),
  KEY `index_merge_requests_on_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `milestones` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `project_id` int(11) NOT NULL,
  `description` text,
  `due_date` date DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `state` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_milestones_on_due_date` (`due_date`),
  KEY `index_milestones_on_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `namespaces` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `path` varchar(255) NOT NULL,
  `owner_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `type` varchar(255) DEFAULT NULL,
  `description` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `index_namespaces_on_name` (`name`),
  KEY `index_namespaces_on_owner_id` (`owner_id`),
  KEY `index_namespaces_on_path` (`path`),
  KEY `index_namespaces_on_type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;

CREATE TABLE `notes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `note` text,
  `noteable_type` varchar(255) DEFAULT NULL,
  `author_id` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `project_id` int(11) DEFAULT NULL,
  `attachment` varchar(255) DEFAULT NULL,
  `line_code` varchar(255) DEFAULT NULL,
  `commit_id` varchar(255) DEFAULT NULL,
  `noteable_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_notes_on_commit_id` (`commit_id`),
  KEY `index_notes_on_created_at` (`created_at`),
  KEY `index_notes_on_noteable_type` (`noteable_type`),
  KEY `index_notes_on_project_id_and_noteable_type` (`project_id`,`noteable_type`),
  KEY `index_notes_on_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `projects` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `path` varchar(255) DEFAULT NULL,
  `description` text,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `creator_id` int(11) DEFAULT NULL,
  `default_branch` varchar(255) DEFAULT NULL,
  `issues_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `wall_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `merge_requests_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `wiki_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `namespace_id` int(11) DEFAULT NULL,
  `public` tinyint(1) NOT NULL DEFAULT '0',
  `issues_tracker` varchar(255) NOT NULL DEFAULT 'gitlab',
  `issues_tracker_id` varchar(255) DEFAULT NULL,
  `snippets_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `last_activity_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_projects_on_owner_id` (`creator_id`),
  KEY `index_projects_on_last_activity_at` (`last_activity_at`),
  KEY `index_projects_on_namespace_id` (`namespace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `protected_branches` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `schema_migrations` (
  `version` varchar(255) NOT NULL,
  UNIQUE KEY `unique_schema_migrations` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `services` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `token` varchar(255) DEFAULT NULL,
  `project_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '0',
  `project_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_services_on_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `snippets` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) DEFAULT NULL,
  `content` text,
  `author_id` int(11) NOT NULL,
  `project_id` int(11) NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `expires_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_snippets_on_created_at` (`created_at`),
  KEY `index_snippets_on_expires_at` (`expires_at`),
  KEY `index_snippets_on_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `taggings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tag_id` int(11) DEFAULT NULL,
  `taggable_id` int(11) DEFAULT NULL,
  `taggable_type` varchar(255) DEFAULT NULL,
  `tagger_id` int(11) DEFAULT NULL,
  `tagger_type` varchar(255) DEFAULT NULL,
  `context` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `tags` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `user_team_project_relationships` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) DEFAULT NULL,
  `user_team_id` int(11) DEFAULT NULL,
  `greatest_access` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `user_team_user_relationships` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `user_team_id` int(11) DEFAULT NULL,
  `group_admin` tinyint(1) DEFAULT NULL,
  `permission` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `user_teams` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `path` varchar(255) DEFAULT NULL,
  `owner_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `description` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL DEFAULT '',
  `encrypted_password` varchar(128) NOT NULL DEFAULT '',
  `reset_password_token` varchar(255) DEFAULT NULL,
  `reset_password_sent_at` datetime DEFAULT NULL,
  `remember_created_at` datetime DEFAULT NULL,
  `sign_in_count` int(11) DEFAULT '0',
  `current_sign_in_at` datetime DEFAULT NULL,
  `last_sign_in_at` datetime DEFAULT NULL,
  `current_sign_in_ip` varchar(255) DEFAULT NULL,
  `last_sign_in_ip` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `admin` tinyint(1) NOT NULL DEFAULT '0',
  `projects_limit` int(11) DEFAULT '10',
  `skype` varchar(255) NOT NULL DEFAULT '',
  `linkedin` varchar(255) NOT NULL DEFAULT '',
  `twitter` varchar(255) NOT NULL DEFAULT '',
  `authentication_token` varchar(255) DEFAULT NULL,
  `theme_id` int(11) NOT NULL DEFAULT '1',
  `bio` varchar(255) DEFAULT NULL,
  `failed_attempts` int(11) DEFAULT '0',
  `locked_at` datetime DEFAULT NULL,
  `extern_uid` varchar(255) DEFAULT NULL,
  `provider` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `can_create_group` tinyint(1) NOT NULL DEFAULT '1',
  `can_create_team` tinyint(1) NOT NULL DEFAULT '1',
  `state` varchar(255) DEFAULT NULL,
  `color_scheme_id` int(11) NOT NULL DEFAULT '1',
  `notification_level` int(11) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_users_on_email` (`email`),
  UNIQUE KEY `index_users_on_reset_password_token` (`reset_password_token`),
  KEY `index_users_on_admin` (`admin`),
  KEY `index_users_on_name` (`name`),
  KEY `index_users_on_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;

CREATE TABLE `users_projects` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `project_id` int(11) NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `project_access` int(11) NOT NULL DEFAULT '0',
  `notification_level` int(11) NOT NULL DEFAULT '3',
  PRIMARY KEY (`id`),
  KEY `index_users_projects_on_project_access` (`project_access`),
  KEY `index_users_projects_on_project_id` (`project_id`),
  KEY `index_users_projects_on_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `web_hooks` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `url` varchar(255) DEFAULT NULL,
  `project_id` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `type` varchar(255) DEFAULT 'ProjectHook',
  `service_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO schema_migrations (version) VALUES ('20110913200833');

INSERT INTO schema_migrations (version) VALUES ('20110913204141');

INSERT INTO schema_migrations (version) VALUES ('20110914221600');

INSERT INTO schema_migrations (version) VALUES ('20110915205627');

INSERT INTO schema_migrations (version) VALUES ('20110915213352');

INSERT INTO schema_migrations (version) VALUES ('20110916123731');

INSERT INTO schema_migrations (version) VALUES ('20110916162511');

INSERT INTO schema_migrations (version) VALUES ('20110917212932');

INSERT INTO schema_migrations (version) VALUES ('20110921192501');

INSERT INTO schema_migrations (version) VALUES ('20110922110156');

INSERT INTO schema_migrations (version) VALUES ('20110923211333');

INSERT INTO schema_migrations (version) VALUES ('20110924214549');

INSERT INTO schema_migrations (version) VALUES ('20110924215658');

INSERT INTO schema_migrations (version) VALUES ('20110926082616');

INSERT INTO schema_migrations (version) VALUES ('20110927130352');

INSERT INTO schema_migrations (version) VALUES ('20110928140106');

INSERT INTO schema_migrations (version) VALUES ('20110928142747');

INSERT INTO schema_migrations (version) VALUES ('20110928161328');

INSERT INTO schema_migrations (version) VALUES ('20111005193700');

INSERT INTO schema_migrations (version) VALUES ('20111009101738');

INSERT INTO schema_migrations (version) VALUES ('20111009110913');

INSERT INTO schema_migrations (version) VALUES ('20111009111204');

INSERT INTO schema_migrations (version) VALUES ('20111015154310');

INSERT INTO schema_migrations (version) VALUES ('20111016183422');

INSERT INTO schema_migrations (version) VALUES ('20111016193417');

INSERT INTO schema_migrations (version) VALUES ('20111016195506');

INSERT INTO schema_migrations (version) VALUES ('20111019212429');

INSERT INTO schema_migrations (version) VALUES ('20111021101550');

INSERT INTO schema_migrations (version) VALUES ('20111025134235');

INSERT INTO schema_migrations (version) VALUES ('20111027051828');

INSERT INTO schema_migrations (version) VALUES ('20111027142641');

INSERT INTO schema_migrations (version) VALUES ('20111027152724');

INSERT INTO schema_migrations (version) VALUES ('20111101222453');

INSERT INTO schema_migrations (version) VALUES ('20111111093150');

INSERT INTO schema_migrations (version) VALUES ('20111115063954');

INSERT INTO schema_migrations (version) VALUES ('20111124115339');

INSERT INTO schema_migrations (version) VALUES ('20111127155345');

INSERT INTO schema_migrations (version) VALUES ('20111206213842');

INSERT INTO schema_migrations (version) VALUES ('20111206222316');

INSERT INTO schema_migrations (version) VALUES ('20111207211728');

INSERT INTO schema_migrations (version) VALUES ('20111214091851');

INSERT INTO schema_migrations (version) VALUES ('20111220190817');

INSERT INTO schema_migrations (version) VALUES ('20111231111825');

INSERT INTO schema_migrations (version) VALUES ('20120110180749');

INSERT INTO schema_migrations (version) VALUES ('20120119202326');

INSERT INTO schema_migrations (version) VALUES ('20120121122616');

INSERT INTO schema_migrations (version) VALUES ('20120206170141');

INSERT INTO schema_migrations (version) VALUES ('20120215182305');

INSERT INTO schema_migrations (version) VALUES ('20120216085842');

INSERT INTO schema_migrations (version) VALUES ('20120216215008');

INSERT INTO schema_migrations (version) VALUES ('20120219130957');

INSERT INTO schema_migrations (version) VALUES ('20120219140810');

INSERT INTO schema_migrations (version) VALUES ('20120219193300');

INSERT INTO schema_migrations (version) VALUES ('20120228130210');

INSERT INTO schema_migrations (version) VALUES ('20120228134252');

INSERT INTO schema_migrations (version) VALUES ('20120301185805');

INSERT INTO schema_migrations (version) VALUES ('20120307095918');

INSERT INTO schema_migrations (version) VALUES ('20120315111711');

INSERT INTO schema_migrations (version) VALUES ('20120315132931');

INSERT INTO schema_migrations (version) VALUES ('20120317095543');

INSERT INTO schema_migrations (version) VALUES ('20120323221339');

INSERT INTO schema_migrations (version) VALUES ('20120329170745');

INSERT INTO schema_migrations (version) VALUES ('20120405211750');

INSERT INTO schema_migrations (version) VALUES ('20120408180246');

INSERT INTO schema_migrations (version) VALUES ('20120408181910');

INSERT INTO schema_migrations (version) VALUES ('20120413135904');

INSERT INTO schema_migrations (version) VALUES ('20120627145613');

INSERT INTO schema_migrations (version) VALUES ('20120706065612');

INSERT INTO schema_migrations (version) VALUES ('20120712080407');

INSERT INTO schema_migrations (version) VALUES ('20120729131232');

INSERT INTO schema_migrations (version) VALUES ('20120905043334');

INSERT INTO schema_migrations (version) VALUES ('20121002150926');

INSERT INTO schema_migrations (version) VALUES ('20121002151033');

INSERT INTO schema_migrations (version) VALUES ('20121009205010');

INSERT INTO schema_migrations (version) VALUES ('20121026114600');

INSERT INTO schema_migrations (version) VALUES ('20121119170638');

INSERT INTO schema_migrations (version) VALUES ('20121120051432');

INSERT INTO schema_migrations (version) VALUES ('20121120103700');

INSERT INTO schema_migrations (version) VALUES ('20121120113838');

INSERT INTO schema_migrations (version) VALUES ('20121122145155');

INSERT INTO schema_migrations (version) VALUES ('20121122150932');

INSERT INTO schema_migrations (version) VALUES ('20121123104937');

INSERT INTO schema_migrations (version) VALUES ('20121123164910');

INSERT INTO schema_migrations (version) VALUES ('20121203154450');

INSERT INTO schema_migrations (version) VALUES ('20121203160507');

INSERT INTO schema_migrations (version) VALUES ('20121205201726');

INSERT INTO schema_migrations (version) VALUES ('20121218164840');

INSERT INTO schema_migrations (version) VALUES ('20121219095402');

INSERT INTO schema_migrations (version) VALUES ('20121219183753');

INSERT INTO schema_migrations (version) VALUES ('20121220064104');

INSERT INTO schema_migrations (version) VALUES ('20121220064453');

INSERT INTO schema_migrations (version) VALUES ('20130102143055');

INSERT INTO schema_migrations (version) VALUES ('20130110172407');

INSERT INTO schema_migrations (version) VALUES ('20130123114545');

INSERT INTO schema_migrations (version) VALUES ('20130125090214');

INSERT INTO schema_migrations (version) VALUES ('20130131070232');

INSERT INTO schema_migrations (version) VALUES ('20130206084024');

INSERT INTO schema_migrations (version) VALUES ('20130207104426');

INSERT INTO schema_migrations (version) VALUES ('20130211085435');

INSERT INTO schema_migrations (version) VALUES ('20130214154045');

INSERT INTO schema_migrations (version) VALUES ('20130218140952');

INSERT INTO schema_migrations (version) VALUES ('20130218141038');

INSERT INTO schema_migrations (version) VALUES ('20130218141117');

INSERT INTO schema_migrations (version) VALUES ('20130218141258');

INSERT INTO schema_migrations (version) VALUES ('20130218141327');

INSERT INTO schema_migrations (version) VALUES ('20130218141344');

INSERT INTO schema_migrations (version) VALUES ('20130218141444');

INSERT INTO schema_migrations (version) VALUES ('20130218141507');

INSERT INTO schema_migrations (version) VALUES ('20130218141536');

INSERT INTO schema_migrations (version) VALUES ('20130218141554');

INSERT INTO schema_migrations (version) VALUES ('20130220124204');

INSERT INTO schema_migrations (version) VALUES ('20130220125544');

INSERT INTO schema_migrations (version) VALUES ('20130220125545');

INSERT INTO schema_migrations (version) VALUES ('20130220133245');

INSERT INTO schema_migrations (version) VALUES ('20130304104623');

INSERT INTO schema_migrations (version) VALUES ('20130304104740');

INSERT INTO schema_migrations (version) VALUES ('20130304105317');

INSERT INTO schema_migrations (version) VALUES ('20130315124931');

INSERT INTO schema_migrations (version) VALUES ('20130318212250');

INSERT INTO schema_migrations (version) VALUES ('20130325173941');

INSERT INTO schema_migrations (version) VALUES ('20130403003950');

INSERT INTO schema_migrations (version) VALUES ('20130404164628');

INSERT INTO schema_migrations (version) VALUES ('20130410175022');