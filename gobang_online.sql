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

 Date: 26/06/2025 18:26:04
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
  INDEX `game_id`(`game_id`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 449 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Fixed;

-- 为落子并发安全添加唯一索引，防止同一局同一格被重复落子
ALTER TABLE `game_moves` ADD UNIQUE INDEX uniq_game_xy (`game_id`, `x`, `y`);

-- ----------------------------
-- Records of game_moves
-- ----------------------------
INSERT INTO `game_moves` VALUES (448, 141, 1, 2, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (447, 139, 9, 6, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (446, 139, 8, 5, 8, 2, NULL);
INSERT INTO `game_moves` VALUES (445, 139, 7, 5, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (444, 139, 6, 4, 9, 2, NULL);
INSERT INTO `game_moves` VALUES (443, 139, 5, 4, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (442, 139, 4, 3, 9, 2, NULL);
INSERT INTO `game_moves` VALUES (441, 139, 3, 3, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (440, 139, 2, 2, 9, 2, NULL);
INSERT INTO `game_moves` VALUES (439, 139, 1, 2, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (438, 138, 12, 5, 11, 2, NULL);
INSERT INTO `game_moves` VALUES (437, 138, 11, 4, 12, 1, NULL);
INSERT INTO `game_moves` VALUES (436, 138, 10, 4, 10, 2, NULL);
INSERT INTO `game_moves` VALUES (435, 138, 9, 3, 11, 1, NULL);
INSERT INTO `game_moves` VALUES (434, 138, 8, 1, 7, 2, NULL);
INSERT INTO `game_moves` VALUES (433, 138, 7, 1, 8, 1, NULL);
INSERT INTO `game_moves` VALUES (432, 138, 6, 2, 8, 2, NULL);
INSERT INTO `game_moves` VALUES (431, 138, 5, 2, 9, 1, NULL);
INSERT INTO `game_moves` VALUES (430, 138, 4, 3, 9, 2, NULL);
INSERT INTO `game_moves` VALUES (429, 138, 3, 2, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (428, 138, 2, 1, 10, 2, NULL);
INSERT INTO `game_moves` VALUES (427, 138, 1, 1, 11, 1, NULL);
INSERT INTO `game_moves` VALUES (426, 137, 10, 6, 8, 2, NULL);
INSERT INTO `game_moves` VALUES (425, 137, 9, 3, 14, 1, NULL);
INSERT INTO `game_moves` VALUES (424, 137, 8, 5, 9, 2, NULL);
INSERT INTO `game_moves` VALUES (423, 137, 7, 2, 14, 1, NULL);
INSERT INTO `game_moves` VALUES (422, 137, 6, 4, 10, 2, NULL);
INSERT INTO `game_moves` VALUES (421, 137, 5, 1, 14, 1, NULL);
INSERT INTO `game_moves` VALUES (420, 137, 4, 3, 11, 2, NULL);
INSERT INTO `game_moves` VALUES (419, 137, 3, 3, 12, 1, NULL);
INSERT INTO `game_moves` VALUES (418, 137, 2, 2, 12, 2, NULL);
INSERT INTO `game_moves` VALUES (417, 137, 1, 2, 13, 1, NULL);
INSERT INTO `game_moves` VALUES (416, 136, 9, 5, 11, 1, NULL);
INSERT INTO `game_moves` VALUES (415, 136, 8, 4, 10, 2, NULL);
INSERT INTO `game_moves` VALUES (414, 136, 7, 4, 11, 1, NULL);
INSERT INTO `game_moves` VALUES (413, 136, 6, 3, 10, 2, NULL);
INSERT INTO `game_moves` VALUES (412, 136, 5, 3, 11, 1, NULL);
INSERT INTO `game_moves` VALUES (411, 136, 4, 2, 10, 2, NULL);
INSERT INTO `game_moves` VALUES (410, 136, 3, 2, 11, 1, NULL);
INSERT INTO `game_moves` VALUES (409, 136, 2, 1, 10, 2, NULL);
INSERT INTO `game_moves` VALUES (408, 136, 1, 1, 11, 1, NULL);
INSERT INTO `game_moves` VALUES (407, 135, 9, 6, 6, 1, NULL);
INSERT INTO `game_moves` VALUES (406, 135, 8, 5, 7, 2, NULL);
INSERT INTO `game_moves` VALUES (405, 135, 7, 5, 6, 1, NULL);
INSERT INTO `game_moves` VALUES (404, 135, 6, 4, 7, 2, NULL);
INSERT INTO `game_moves` VALUES (403, 135, 5, 4, 6, 1, NULL);
INSERT INTO `game_moves` VALUES (402, 135, 4, 3, 7, 2, NULL);
INSERT INTO `game_moves` VALUES (401, 135, 3, 3, 6, 1, NULL);
INSERT INTO `game_moves` VALUES (400, 135, 2, 2, 7, 2, NULL);
INSERT INTO `game_moves` VALUES (399, 135, 1, 2, 6, 1, NULL);
INSERT INTO `game_moves` VALUES (398, 134, 2, 1, 3, 2, NULL);
INSERT INTO `game_moves` VALUES (397, 134, 1, 1, 1, 1, NULL);
INSERT INTO `game_moves` VALUES (396, 132, 1, 4, 13, 1, NULL);
INSERT INTO `game_moves` VALUES (395, 131, 1, 1, 9, 1, NULL);
INSERT INTO `game_moves` VALUES (394, 130, 1, 2, 4, 1, NULL);
INSERT INTO `game_moves` VALUES (393, 128, 1, 1, 12, 1, NULL);
INSERT INTO `game_moves` VALUES (392, 126, 1, 1, 12, 1, NULL);
INSERT INTO `game_moves` VALUES (391, 125, 1, 1, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (390, 124, 1, 2, 3, 1, NULL);
INSERT INTO `game_moves` VALUES (389, 120, 1, 3, 5, 1, NULL);
INSERT INTO `game_moves` VALUES (388, 117, 3, 3, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (387, 117, 2, 2, 7, 2, NULL);
INSERT INTO `game_moves` VALUES (386, 117, 1, 3, 7, 1, NULL);
INSERT INTO `game_moves` VALUES (385, 114, 1, 1, 5, 1, NULL);
INSERT INTO `game_moves` VALUES (384, 112, 6, 2, 10, 2, NULL);
INSERT INTO `game_moves` VALUES (383, 112, 5, 1, 10, 1, NULL);
INSERT INTO `game_moves` VALUES (382, 112, 4, 2, 11, 2, NULL);
INSERT INTO `game_moves` VALUES (381, 112, 3, 2, 7, 1, NULL);
INSERT INTO `game_moves` VALUES (380, 112, 2, 1, 3, 2, NULL);
INSERT INTO `game_moves` VALUES (379, 112, 1, 1, 2, 1, NULL);
INSERT INTO `game_moves` VALUES (378, 109, 1, 1, 13, 1, NULL);
INSERT INTO `game_moves` VALUES (377, 108, 2, 2, 7, 2, NULL);
INSERT INTO `game_moves` VALUES (376, 108, 1, 2, 6, 1, NULL);

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
) ENGINE = MyISAM AUTO_INCREMENT = 142 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Fixed;

-- ----------------------------
-- Records of game_record
-- ----------------------------
INSERT INTO `game_record` VALUES (138, 154, 1, 2, 2, '2025-06-26 17:21:31', '2025-06-26 17:21:43');
INSERT INTO `game_record` VALUES (137, 153, 1, 2, 2, '2025-06-26 17:19:17', '2025-06-26 17:19:29');
INSERT INTO `game_record` VALUES (135, 149, 2, 1, 2, '2025-06-26 17:17:37', '2025-06-26 17:17:51');
INSERT INTO `game_record` VALUES (134, 149, 2, 1, 2, '2025-06-26 17:17:25', '2025-06-26 17:17:30');
INSERT INTO `game_record` VALUES (131, 147, 2, 1, 1, '2025-06-26 17:11:11', '2025-06-26 17:11:14');
INSERT INTO `game_record` VALUES (130, 146, 2, 1, 1, '2025-06-26 17:07:01', '2025-06-26 17:07:04');
INSERT INTO `game_record` VALUES (126, 143, 1, 2, NULL, '2025-06-26 17:00:34', NULL);
INSERT INTO `game_record` VALUES (125, 142, 1, 2, 1, '2025-06-26 17:00:00', '2025-06-26 17:00:02');
INSERT INTO `game_record` VALUES (124, 141, 2, 1, 2, '2025-06-26 16:58:05', '2025-06-26 16:58:08');
INSERT INTO `game_record` VALUES (123, 140, 2, 1, 1, '2025-06-26 16:56:00', '2025-06-26 16:56:02');
INSERT INTO `game_record` VALUES (122, 139, 1, 2, 2, '2025-06-26 16:50:28', '2025-06-26 16:50:29');
INSERT INTO `game_record` VALUES (121, 139, 1, 2, 2, '2025-06-26 16:50:21', '2025-06-26 16:50:22');
INSERT INTO `game_record` VALUES (120, 138, 2, 1, 2, '2025-06-26 16:47:00', '2025-06-26 16:47:03');
INSERT INTO `game_record` VALUES (119, 138, 2, 1, 2, '2025-06-26 16:46:42', '2025-06-26 16:46:43');
INSERT INTO `game_record` VALUES (118, 138, 2, 1, 2, '2025-06-26 16:46:37', '2025-06-26 16:46:38');
INSERT INTO `game_record` VALUES (117, 137, 1, 2, 2, '2025-06-26 16:45:59', '2025-06-26 16:46:06');
INSERT INTO `game_record` VALUES (116, 136, 2, 1, 2, '2025-06-26 16:41:20', '2025-06-26 16:41:22');
INSERT INTO `game_record` VALUES (115, 135, 1, 2, 2, '2025-06-26 16:37:16', '2025-06-26 16:37:17');
INSERT INTO `game_record` VALUES (114, 134, 2, 1, 2, '2025-06-26 16:34:46', '2025-06-26 16:34:50');
INSERT INTO `game_record` VALUES (113, 133, 2, 1, NULL, '2025-06-26 16:32:55', NULL);
INSERT INTO `game_record` VALUES (112, 132, 2, 1, NULL, '2025-06-26 16:32:22', NULL);
INSERT INTO `game_record` VALUES (111, 131, 1, 2, NULL, '2025-06-26 16:29:50', '2025-06-26 16:29:51');
INSERT INTO `game_record` VALUES (110, 131, 1, 2, NULL, '2025-06-26 16:29:47', '2025-06-26 16:29:47');
INSERT INTO `game_record` VALUES (109, 130, 1, 2, NULL, '2025-06-26 16:23:57', '2025-06-26 16:24:00');
INSERT INTO `game_record` VALUES (108, 129, 2, 1, NULL, '2025-06-26 16:23:45', '2025-06-26 16:23:49');
INSERT INTO `game_record` VALUES (107, 126, 2, 1, NULL, '2025-06-26 16:14:43', '2025-06-26 16:14:44');
INSERT INTO `game_record` VALUES (106, 125, 1, 2, NULL, '2025-06-26 16:14:30', '2025-06-26 16:14:34');

-- ----------------------------
-- Table structure for room
-- ----------------------------
DROP TABLE IF EXISTS `room`;
CREATE TABLE `room`  (
  `room_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `owner_id` bigint(20) NOT NULL,
  `status` tinyint(4) NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`room_id`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 156 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Fixed;

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
  `join_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 441 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Fixed;

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
) ENGINE = MyISAM AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, '123456', '123456', '$2a$10$iV1J1PMGathqMETtQR0MgecoW6e6.nlWgBwjDligMO1t9.THdeFiW', 'liuchuanhua2003@outlook.com', '2025-06-23 18:52:46');
INSERT INTO `user` VALUES (2, '12345678', '12345678', '$2a$10$4Qyn8tSzhcmWjZz8QB5aLOa/v5QKSEIVJnHf.a4kNgpJQ6TUp3fMi', '3236870353@qq.com', '2025-06-25 16:13:28');

SET FOREIGN_KEY_CHECKS = 1;
