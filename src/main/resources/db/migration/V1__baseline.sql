-- Civitas baseline schema: the state of the production and local dev databases when Flyway was
-- introduced (2026-10-03), taken with `mysqldump --no-data` and includes the manually applied
-- ADMIN/VIEWER role change (docs/migrations/2026-09-add-viewer-role.sql).
--
-- Existing databases are baselined at version 1, so this script never runs on them; it only builds
-- an empty database from scratch. Never edit it — add a V2__... migration instead.
-- Tables are ordered so every foreign key references an already-created table.

CREATE TABLE `unions` (
  `id` binary(16) NOT NULL,
  `address` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `status` tinyint DEFAULT NULL,
  `stripe_customer_id` varchar(255) DEFAULT NULL,
  `vat_code` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnufmovq1gakny0gnpfo9u0vui` (`vat_code`),
  CONSTRAINT `unions_chk_1` CHECK ((`status` between 0 and 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','VIEWER') NOT NULL,
  `username` varchar(255) NOT NULL,
  `union_id` binary(16) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
  KEY `FKl9if8gaau95iju5xmkjwo65qs` (`union_id`),
  CONSTRAINT `FKl9if8gaau95iju5xmkjwo65qs` FOREIGN KEY (`union_id`) REFERENCES `unions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `date_of_birth` date DEFAULT NULL,
  `date_of_last_payment` date DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `language` enum('EN','NL','TR') NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `member_status` enum('ACTIVE','INACTIVE') NOT NULL,
  `next_billing_date` date DEFAULT NULL,
  `phone_number` varchar(255) NOT NULL,
  `subscription_amount` decimal(38,2) DEFAULT NULL,
  `subscription_frequency` enum('MONTHLY','NONE','YEARLY') DEFAULT NULL,
  `subscription_status` enum('ACTIVE','PAUSED') DEFAULT NULL,
  `union_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKp3kli6lvjnqexy7eag0p48c6c` (`email`,`union_id`),
  KEY `FKb153ru1mc6idaqwwdymniro2q` (`union_id`),
  CONSTRAINT `FKb153ru1mc6idaqwwdymniro2q` FOREIGN KEY (`union_id`) REFERENCES `unions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(500) DEFAULT NULL,
  `end` datetime(6) NOT NULL,
  `event_type` enum('GENERAL','MEETING','SOCIAL','WORKSHOP') DEFAULT NULL,
  `location` varchar(100) DEFAULT NULL,
  `start` datetime(6) NOT NULL,
  `title` varchar(100) NOT NULL,
  `union_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqcd3eh37u2t89fae1fof37mbc` (`union_id`),
  CONSTRAINT `FKqcd3eh37u2t89fae1fof37mbc` FOREIGN KEY (`union_id`) REFERENCES `unions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `event_members` (
  `event_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  PRIMARY KEY (`event_id`,`member_id`),
  KEY `FKl3lo9ylixd3li85iycei0xd3u` (`member_id`),
  CONSTRAINT `FKa16fvd3ddidr02a2w3scu9wqy` FOREIGN KEY (`event_id`) REFERENCES `event` (`id`),
  CONSTRAINT `FKl3lo9ylixd3li85iycei0xd3u` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` double NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `currency` varchar(255) NOT NULL,
  `last_reminder_sent_at` datetime(6) DEFAULT NULL,
  `note` varchar(255) DEFAULT NULL,
  `payment_id` varchar(255) DEFAULT NULL,
  `status` enum('EXPIRED','FAILED','PAID_MANUALLY','PENDING','SUCCEEDED') DEFAULT NULL,
  `type` enum('DONATION','EVENT_PAYMENT','MEMBERSHIP_FEE','OTHER') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `member_id` bigint NOT NULL,
  `union_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8g210cmyp5dyikvxf9ek6ub30` (`member_id`),
  KEY `FK9txy7m3wf0o820iqb52ffrdos` (`union_id`),
  CONSTRAINT `FK8g210cmyp5dyikvxf9ek6ub30` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`),
  CONSTRAINT `FK9txy7m3wf0o820iqb52ffrdos` FOREIGN KEY (`union_id`) REFERENCES `unions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `accounting_invoices` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(255) DEFAULT NULL,
  `counterparty` varchar(255) DEFAULT NULL,
  `file_url` varchar(255) DEFAULT NULL,
  `filename` varchar(255) DEFAULT NULL,
  `invoice_date` date DEFAULT NULL,
  `invoice_number` varchar(255) DEFAULT NULL,
  `status` enum('APPROVED','ARCHIVED','PENDING_REVIEW') NOT NULL,
  `total_amount` decimal(19,2) DEFAULT NULL,
  `type` enum('EXPENSE','INCOME') NOT NULL,
  `union_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK5l5a2o8djsmjt0cs9n9e2olj9` (`union_id`),
  CONSTRAINT `FK5l5a2o8djsmjt0cs9n9e2olj9` FOREIGN KEY (`union_id`) REFERENCES `unions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `message_args` varchar(255) DEFAULT NULL,
  `message_key` varchar(255) DEFAULT NULL,
  `status` enum('READ','UNREAD') DEFAULT NULL,
  `title_key` varchar(255) DEFAULT NULL,
  `type` enum('EVENT','MEMBER','OTHER','SYSTEM','TRANSACTION') DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnk4ftb5am9ubmkv1661h15ds9` (`user_id`),
  CONSTRAINT `FKnk4ftb5am9ubmkv1661h15ds9` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_dashboard_tile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) NOT NULL,
  `position` int NOT NULL,
  `settings_json` text,
  `widget_key` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKis304xb5ff35vcsykqvnbttv3` (`user_id`),
  CONSTRAINT `FKis304xb5ff35vcsykqvnbttv3` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
