/*
 Navicat Premium Dump SQL

 Source Server         : mysql80
 Source Server Type    : MySQL
 Source Server Version : 80012 (8.0.12)
 Source Host           : localhost:3307
 Source Schema         : gobang_online

 Target Server Type    : MySQL
 Target Server Version : 80012 (8.0.12)
 File Encoding         : 65001

 Date: 26/06/2025 23:35:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for game_moves
-- ----------------------------
DROP TABLE IF EXISTS `game_moves`;
CREATE TABLE `game_moves`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `game_id` bigint(20) NOT NULL,
  `move_index` int(11) NOT NULL,
  `x` int(11) NOT NULL,
  `y` int(11) NOT NULL,
  `player` tinyint(4) NOT NULL,
  `move_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_game_xy`(`game_id`, `x`, `y`) USING BTREE,
  INDEX `game_id`(`game_id`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 514 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = FIXED;

-- ----------------------------
-- Records of game_moves
-- ----------------------------
INSERT INTO `game_moves` VALUES (449, 144, 1, 4, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (450, 144, 2, 2, 3, 2, NULL);
INSERT INTO `game_moves` VALUES (451, 145, 1, 7, 6, 1, NULL);
INSERT INTO `game_moves` VALUES (452, 145, 2, 3, 8, 2, NULL);
INSERT INTO `game_moves` VALUES (453, 145, 3, 4, 11, 1, NULL);
INSERT INTO `game_moves` VALUES (454, 145, 4, 4, 7, 2, NULL);
INSERT INTO `game_moves` VALUES (455, 146, 1, 5, 6, 1, NULL);
INSERT INTO `game_moves` VALUES (456, 146, 2, 6, 5, 2, NULL);
INSERT INTO `game_moves` VALUES (457, 149, 1, 5, 8, 1, NULL);
INSERT INTO `game_moves` VALUES (458, 149, 2, 3, 3, 2, NULL);
INSERT INTO `game_moves` VALUES (459, 149, 3, 7, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (460, 149, 4, 6, 6, 2, NULL);
INSERT INTO `game_moves` VALUES (461, 149, 5, 4, 14, 1, NULL);
INSERT INTO `game_moves` VALUES (462, 149, 6, 2, 8, 2, NULL);
INSERT INTO `game_moves` VALUES (463, 149, 7, 2, 12, 1, NULL);
INSERT INTO `game_moves` VALUES (464, 149, 8, 2, 5, 2, NULL);
INSERT INTO `game_moves` VALUES (465, 149, 9, 4, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (466, 149, 10, 1, 4, 2, NULL);
INSERT INTO `game_moves` VALUES (467, 149, 11, 8, 13, 1, NULL);
INSERT INTO `game_moves` VALUES (468, 149, 12, 5, 7, 2, NULL);
INSERT INTO `game_moves` VALUES (469, 149, 13, 10, 8, 1, NULL);
INSERT INTO `game_moves` VALUES (470, 149, 14, 5, 2, 2, NULL);
INSERT INTO `game_moves` VALUES (471, 149, 15, 6, 11, 1, NULL);
INSERT INTO `game_moves` VALUES (472, 149, 16, 4, 6, 2, NULL);
INSERT INTO `game_moves` VALUES (473, 149, 17, 10, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (474, 149, 18, 4, 5, 2, NULL);
INSERT INTO `game_moves` VALUES (475, 150, 1, 11, 11, 1, NULL);
INSERT INTO `game_moves` VALUES (476, 150, 2, 7, 6, 2, NULL);
INSERT INTO `game_moves` VALUES (477, 150, 3, 5, 12, 1, NULL);
INSERT INTO `game_moves` VALUES (478, 150, 4, 4, 8, 2, NULL);
INSERT INTO `game_moves` VALUES (479, 150, 5, 1, 11, 1, NULL);
INSERT INTO `game_moves` VALUES (480, 150, 6, 2, 6, 2, NULL);
INSERT INTO `game_moves` VALUES (481, 150, 7, 8, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (482, 150, 8, 5, 4, 2, NULL);
INSERT INTO `game_moves` VALUES (483, 156, 1, 2, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (484, 156, 2, 3, 7, 2, NULL);
INSERT INTO `game_moves` VALUES (485, 157, 1, 5, 6, 1, NULL);
INSERT INTO `game_moves` VALUES (486, 157, 2, 4, 9, 2, NULL);
INSERT INTO `game_moves` VALUES (487, 157, 3, 8, 7, 1, NULL);
INSERT INTO `game_moves` VALUES (488, 157, 4, 6, 10, 2, NULL);
INSERT INTO `game_moves` VALUES (489, 158, 1, 3, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (490, 158, 2, 3, 8, 2, NULL);
INSERT INTO `game_moves` VALUES (491, 158, 3, 5, 13, 1, NULL);
INSERT INTO `game_moves` VALUES (492, 158, 4, 2, 6, 2, NULL);
INSERT INTO `game_moves` VALUES (493, 158, 5, 6, 11, 1, NULL);
INSERT INTO `game_moves` VALUES (494, 158, 6, 1, 4, 2, NULL);
INSERT INTO `game_moves` VALUES (495, 158, 7, 3, 12, 1, NULL);
INSERT INTO `game_moves` VALUES (496, 158, 8, 3, 5, 2, NULL);
INSERT INTO `game_moves` VALUES (497, 158, 9, 8, 4, 1, NULL);
INSERT INTO `game_moves` VALUES (498, 158, 10, 4, 7, 2, NULL);
INSERT INTO `game_moves` VALUES (499, 158, 11, 7, 8, 1, NULL);
INSERT INTO `game_moves` VALUES (500, 158, 12, 1, 13, 2, NULL);
INSERT INTO `game_moves` VALUES (501, 159, 1, 5, 8, 1, NULL);
INSERT INTO `game_moves` VALUES (502, 160, 1, 5, 7, 1, NULL);
INSERT INTO `game_moves` VALUES (503, 160, 2, 4, 7, 2, NULL);
INSERT INTO `game_moves` VALUES (504, 160, 3, 5, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (505, 163, 1, 3, 7, 1, NULL);
INSERT INTO `game_moves` VALUES (506, 163, 2, 4, 7, 2, NULL);
INSERT INTO `game_moves` VALUES (507, 163, 3, 3, 8, 1, NULL);
INSERT INTO `game_moves` VALUES (508, 163, 4, 4, 8, 2, NULL);
INSERT INTO `game_moves` VALUES (509, 163, 5, 3, 9, 1, NULL);
INSERT INTO `game_moves` VALUES (510, 163, 6, 4, 9, 2, NULL);
INSERT INTO `game_moves` VALUES (511, 163, 7, 3, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (512, 163, 8, 4, 10, 2, NULL);
INSERT INTO `game_moves` VALUES (513, 163, 9, 3, 11, 1, NULL);

-- ----------------------------
-- Table structure for game_record
-- ----------------------------
DROP TABLE IF EXISTS `game_record`;
CREATE TABLE `game_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `room_id` bigint(20) NOT NULL,
  `black_id` bigint(20) NOT NULL,
  `white_id` bigint(20) NOT NULL,
  `winner` bigint(20) NULL DEFAULT NULL,
  `start_time` datetime NULL DEFAULT NULL,
  `end_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `room_id`(`room_id`) USING BTREE,
  INDEX `black_id`(`black_id`) USING BTREE,
  INDEX `white_id`(`white_id`) USING BTREE,
  INDEX `winner`(`winner`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 165 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = FIXED;

-- ----------------------------
-- Records of game_record
-- ----------------------------
INSERT INTO `game_record` VALUES (142, 156, 1, 2, 2, '2025-06-26 19:56:12', '2025-06-26 19:56:28');
INSERT INTO `game_record` VALUES (143, 156, 1, 2, 2, '2025-06-26 19:56:34', '2025-06-26 19:56:36');
INSERT INTO `game_record` VALUES (144, 158, 1, 2, 2, '2025-06-26 20:01:28', '2025-06-26 20:02:25');
INSERT INTO `game_record` VALUES (145, 163, 1, 2, 2, '2025-06-26 20:16:41', '2025-06-26 20:20:00');
INSERT INTO `game_record` VALUES (146, 164, 1, 2, NULL, '2025-06-26 20:20:17', NULL);
INSERT INTO `game_record` VALUES (147, 165, 1, 2, 1, '2025-06-26 20:29:24', '2025-06-26 20:29:34');
INSERT INTO `game_record` VALUES (148, 165, 1, 3, 1, '2025-06-26 20:29:39', '2025-06-26 20:31:54');
INSERT INTO `game_record` VALUES (149, 166, 1, 2, 1, '2025-06-26 20:33:05', '2025-06-26 20:33:52');
INSERT INTO `game_record` VALUES (150, 167, 1, 2, 1, '2025-06-26 20:34:10', '2025-06-26 20:35:19');
INSERT INTO `game_record` VALUES (151, 168, 1, 2, 1, '2025-06-26 20:40:44', '2025-06-26 20:41:15');
INSERT INTO `game_record` VALUES (152, 169, 1, 2, 1, '2025-06-26 20:43:05', '2025-06-26 20:43:24');
INSERT INTO `game_record` VALUES (154, 171, 1, 2, 1, '2025-06-26 20:49:16', '2025-06-26 20:49:48');
INSERT INTO `game_record` VALUES (155, 172, 1, 2, 1, '2025-06-26 20:58:20', '2025-06-26 20:58:35');
INSERT INTO `game_record` VALUES (157, 174, 3, 2, 3, '2025-06-26 21:10:11', '2025-06-26 21:10:47');

-- ----------------------------
-- Table structure for room
-- ----------------------------
DROP TABLE IF EXISTS `room`;
CREATE TABLE `room`  (
  `room_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `owner_id` bigint(20) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`room_id`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 180 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = FIXED;

-- ----------------------------
-- Records of room
-- ----------------------------

-- ----------------------------
-- Table structure for room_user
-- ----------------------------
DROP TABLE IF EXISTS `room_user`;
CREATE TABLE `room_user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `room_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `role` tinyint(4) UNSIGNED ZEROFILL NOT NULL DEFAULT 'player',
  `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`, `role`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 512 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = FIXED;

-- ----------------------------
-- Records of room_user
-- ----------------------------

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `user_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE,
  UNIQUE INDEX `email`(`email`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, '123456', '123456', '$2a$10$iV1J1PMGathqMETtQR0MgecoW6e6.nlWgBwjDligMO1t9.THdeFiW', 'liuchuanhua2003@outlook.com', '2025-06-23 18:52:46');
INSERT INTO `user` VALUES (2, '12345678', '12345678', '$2a$10$4Qyn8tSzhcmWjZz8QB5aLOa/v5QKSEIVJnHf.a4kNgpJQ6TUp3fMi', '3236870353@qq.com', '2025-06-25 16:13:28');
INSERT INTO `user` VALUES (3, '123456789', '123456789', '$2a$10$OWY9VxsFF4dPZi.6F4rqYusssuGNAy.u5EslG/ByKxLpS3ZFm27kC', 'qwery@gmail.com', '2025-06-26 20:17:54');

SET FOREIGN_KEY_CHECKS = 1;
