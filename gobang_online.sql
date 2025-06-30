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

 Date: 30/06/2025 17:29:45
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for game_records
-- ----------------------------
DROP TABLE IF EXISTS `game_records`;
CREATE TABLE `game_records`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
  `room_id` bigint(20) NOT NULL COMMENT '房间ID',
  `game_id` bigint(20) NOT NULL COMMENT '游戏ID',
  `black_player_id` bigint(20) NOT NULL COMMENT '黑棋玩家ID',
  `white_player_id` bigint(20) NOT NULL COMMENT '白棋玩家ID',
  `winner_id` bigint(20) NULL DEFAULT NULL COMMENT '获胜者ID，NULL表示平局或未结束',
  `start_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '游戏开始时间',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '游戏结束时间',
  `moves` json NOT NULL COMMENT '落子序列',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_room_id`(`room_id`) USING BTREE,
  INDEX `idx_game_id`(`game_id`) USING BTREE,
  INDEX `idx_players`(`black_player_id`, `white_player_id`) USING BTREE,
  INDEX `idx_winner`(`winner_id`) USING BTREE,
  INDEX `idx_start_time`(`start_time`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 73 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '游戏记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of game_records
-- ----------------------------
INSERT INTO `game_records` VALUES (1, 1, 1, 1, 2, 1, '2025-06-28 16:01:50', '2025-06-28 16:02:05', '[[3, 9], [3, 8], [4, 9], [4, 8], [5, 9], [5, 8], [6, 9], [6, 8], [7, 9]]');
INSERT INTO `game_records` VALUES (2, 3, 2, 1, 2, 1, '2025-06-28 16:14:56', '2025-06-28 16:15:10', '[[1, 11], [1, 10], [2, 11], [2, 10], [3, 11], [3, 10], [4, 11], [4, 10], [5, 11]]');
INSERT INTO `game_records` VALUES (3, 4, 3, 1, 2, 1, '2025-06-28 16:16:46', '2025-06-28 16:16:56', '[[3, 11], [3, 10], [4, 11], [4, 10], [5, 11], [5, 10], [6, 11], [6, 10], [7, 11]]');
INSERT INTO `game_records` VALUES (4, 4, 4, 2, 1, 2, '2025-06-28 16:16:58', '2025-06-28 16:17:07', '[[3, 7], [3, 8], [4, 7], [4, 8], [5, 7], [5, 8], [6, 7], [6, 8], [7, 7]]');
INSERT INTO `game_records` VALUES (5, 4, 5, 1, 2, 2, '2025-06-28 16:17:09', '2025-06-28 16:22:05', '[[5, 11], [5, 10]]');
INSERT INTO `game_records` VALUES (6, 6, 6, 1, 2, 1, '2025-06-28 16:37:04', '2025-06-28 16:37:18', '[[1, 10], [1, 7], [2, 10], [2, 7], [3, 10], [3, 7], [4, 10], [4, 7], [5, 10]]');
INSERT INTO `game_records` VALUES (7, 7, 7, 1, 2, 2, '2025-06-28 16:45:20', '2025-06-28 16:45:30', '[[2, 12], [2, 11], [3, 11], [6, 4]]');
INSERT INTO `game_records` VALUES (8, 8, 8, 2, 3, 2, '2025-06-28 16:45:45', '2025-06-28 16:46:07', '[]');
INSERT INTO `game_records` VALUES (9, 9, 9, 1, 2, 2, '2025-06-28 16:48:54', '2025-06-28 16:49:00', '[[7, 13], [4, 9]]');
INSERT INTO `game_records` VALUES (10, 10, 10, 1, 2, 1, '2025-06-28 16:52:29', '2025-06-28 16:52:41', '[[2, 9], [2, 8], [3, 9], [3, 8], [4, 9], [4, 8], [5, 9], [5, 8], [6, 9]]');
INSERT INTO `game_records` VALUES (11, 10, 11, 2, 1, 2, '2025-06-28 16:52:45', '2025-06-28 16:53:25', '[[5, 6], [5, 8], [6, 7], [5, 7], [6, 6], [6, 8], [7, 6], [7, 7], [7, 8], [8, 8], [8, 7], [8, 6], [9, 6], [9, 7], [9, 8], [10, 8], [10, 7], [10, 6], [11, 8], [11, 7], [11, 6], [12, 8], [12, 7], [12, 6], [13, 8], [13, 7], [13, 6], [14, 8], [14, 7], [14, 6], [10, 9], [11, 9], [9, 10]]');
INSERT INTO `game_records` VALUES (12, 11, 12, 1, 2, 1, '2025-06-28 17:01:59', '2025-06-28 17:02:14', '[[2, 11], [2, 10], [3, 11], [3, 10], [4, 10]]');
INSERT INTO `game_records` VALUES (13, 11, 13, 1, 3, 1, '2025-06-28 17:02:19', '2025-06-28 17:02:30', '[[2, 13]]');
INSERT INTO `game_records` VALUES (14, 12, 14, 1, 2, 2, '2025-06-28 17:09:54', '2025-06-28 17:10:02', '[]');
INSERT INTO `game_records` VALUES (15, 13, 15, 1, 2, 2, '2025-06-28 17:11:42', '2025-06-28 17:17:48', '[]');
INSERT INTO `game_records` VALUES (16, 14, 16, 1, 2, 2, '2025-06-28 17:19:31', '2025-06-28 17:19:33', '[]');
INSERT INTO `game_records` VALUES (17, 15, 17, 1, 2, 1, '2025-06-28 17:19:58', '2025-06-28 17:24:26', '[]');
INSERT INTO `game_records` VALUES (18, 16, 18, 1, 2, 1, '2025-06-28 17:26:19', '2025-06-28 17:26:33', '[[8, 11], [8, 10], [9, 12], [8, 9], [7, 10], [8, 8], [6, 9], [8, 7], [5, 8]]');
INSERT INTO `game_records` VALUES (19, 16, 19, 2, 1, 2, '2025-06-28 17:26:38', '2025-06-28 17:26:46', '[[6, 3], [6, 4], [6, 2], [7, 4]]');
INSERT INTO `game_records` VALUES (20, 17, 20, 1, 2, 1, '2025-06-28 17:35:16', '2025-06-28 17:35:21', '[]');
INSERT INTO `game_records` VALUES (21, 17, 21, 1, 2, 1, '2025-06-28 17:35:27', '2025-06-28 17:35:50', '[]');
INSERT INTO `game_records` VALUES (22, 17, 22, 1, 2, 1, '2025-06-28 17:36:00', '2025-06-28 17:36:02', '[]');
INSERT INTO `game_records` VALUES (23, 17, 23, 1, 2, 1, '2025-06-28 17:37:46', '2025-06-28 17:37:48', '[]');
INSERT INTO `game_records` VALUES (24, 17, 24, 1, 2, 1, '2025-06-28 17:37:50', '2025-06-28 17:37:52', '[]');
INSERT INTO `game_records` VALUES (25, 1, 1, 1, 2, 1, '2025-06-28 17:42:09', '2025-06-28 17:42:15', '[]');
INSERT INTO `game_records` VALUES (26, 1, 2, 1, 3, 1, '2025-06-28 17:42:25', '2025-06-28 17:42:31', '[]');
INSERT INTO `game_records` VALUES (27, 2, 3, 1, 3, 1, '2025-06-28 17:45:26', '2025-06-28 17:45:34', '[]');
INSERT INTO `game_records` VALUES (28, 3, 4, 1, 3, 3, '2025-06-28 17:45:45', '2025-06-28 17:45:53', '[]');
INSERT INTO `game_records` VALUES (29, 4, 5, 1, 2, 2, '2025-06-28 18:39:05', '2025-06-28 18:39:16', '[[2, 11], [2, 10], [3, 11], [3, 10]]');
INSERT INTO `game_records` VALUES (30, 5, 6, 1, 2, 1, '2025-06-28 18:41:41', '2025-06-28 18:41:46', '[[0, 14], [1, 13]]');
INSERT INTO `game_records` VALUES (31, 5, 7, 1, 2, 1, '2025-06-28 18:41:50', '2025-06-28 18:41:52', '[]');
INSERT INTO `game_records` VALUES (32, 5, 8, 1, 2, 1, '2025-06-28 18:41:58', '2025-06-28 18:42:00', '[]');
INSERT INTO `game_records` VALUES (33, 5, 9, 1, 2, 1, '2025-06-28 18:44:03', '2025-06-28 18:44:05', '[]');
INSERT INTO `game_records` VALUES (34, 5, 10, 1, 2, 1, '2025-06-28 18:44:06', '2025-06-28 18:44:08', '[]');
INSERT INTO `game_records` VALUES (35, 6, 11, 1, 2, 2, '2025-06-28 18:48:22', '2025-06-28 18:48:35', '[[0, 9], [0, 8], [1, 9], [1, 8], [2, 8], [2, 9], [3, 8], [3, 10], [4, 8], [4, 11], [5, 8], [5, 12]]');
INSERT INTO `game_records` VALUES (36, 7, 12, 1, 2, 1, '2025-06-28 18:50:05', '2025-06-28 18:50:19', '[[4, 10], [4, 11], [5, 10], [5, 11], [6, 10], [6, 11], [7, 10], [7, 11], [8, 10]]');
INSERT INTO `game_records` VALUES (37, 8, 13, 1, 2, 1, '2025-06-28 18:54:24', '2025-06-28 18:54:33', '[[2, 13], [2, 12], [3, 13], [3, 12], [4, 13], [4, 12], [5, 13], [5, 12], [6, 13]]');
INSERT INTO `game_records` VALUES (38, 9, 14, 1, 2, 1, '2025-06-28 18:56:42', '2025-06-28 18:56:45', '[]');
INSERT INTO `game_records` VALUES (39, 9, 15, 1, 2, 1, '2025-06-28 18:56:46', '2025-06-28 18:56:50', '[]');
INSERT INTO `game_records` VALUES (40, 9, 16, 1, 2, 1, '2025-06-28 18:56:52', '2025-06-28 18:57:02', '[[2, 11], [2, 10], [3, 11], [3, 10], [4, 11], [4, 10], [5, 11], [5, 10], [6, 11]]');
INSERT INTO `game_records` VALUES (41, 9, 17, 2, 1, 1, '2025-06-28 18:57:05', '2025-06-28 18:57:09', '[]');
INSERT INTO `game_records` VALUES (42, 9, 18, 1, 2, 1, '2025-06-28 18:57:10', '2025-06-28 18:57:12', '[]');
INSERT INTO `game_records` VALUES (43, 11, 19, 1, 2, 2, '2025-06-28 18:59:01', '2025-06-28 18:59:03', '[]');
INSERT INTO `game_records` VALUES (44, 1, 1, 2, 3, 3, '2025-06-28 20:02:26', '2025-06-28 20:02:30', '[[2, 5]]');
INSERT INTO `game_records` VALUES (45, 2, 2, 2, 3, 2, '2025-06-28 20:03:23', '2025-06-28 20:03:25', '[]');
INSERT INTO `game_records` VALUES (46, 4, 4, 1, 3, 1, '2025-06-28 20:04:07', '2025-06-28 20:06:37', '[]');
INSERT INTO `game_records` VALUES (47, 2, 3, 2, 3, 3, '2025-06-28 20:03:30', '2025-06-28 20:06:39', '[[1, 11]]');
INSERT INTO `game_records` VALUES (48, 2, 2, 1, 3, 3, '2025-06-28 20:07:29', '2025-06-28 20:07:40', '[]');
INSERT INTO `game_records` VALUES (49, 1, 1, 2, 3, 2, '2025-06-28 20:07:11', '2025-06-28 20:07:58', '[[4, 9], [2, 12]]');
INSERT INTO `game_records` VALUES (50, 3, 3, 2, 3, 2, '2025-06-28 20:09:20', '2025-06-28 20:09:21', '[]');
INSERT INTO `game_records` VALUES (51, 4, 4, 2, 3, 2, '2025-06-28 20:52:43', '2025-06-28 20:52:51', '[]');
INSERT INTO `game_records` VALUES (52, 6, 5, 1, 2, 1, '2025-06-28 21:17:29', '2025-06-28 21:17:42', '[[2, 9], [2, 3], [3, 13], [3, 5], [3, 11], [3, 4], [3, 10], [3, 3], [3, 12], [2, 6], [3, 9]]');
INSERT INTO `game_records` VALUES (53, 7, 6, 1, 2, 2, '2025-06-28 21:39:08', '2025-06-28 21:39:14', '[]');
INSERT INTO `game_records` VALUES (54, 8, 7, 1, 2, 1, '2025-06-28 21:46:32', '2025-06-28 21:46:35', '[]');
INSERT INTO `game_records` VALUES (55, 9, 8, 1, 2, 2, '2025-06-28 21:46:43', '2025-06-28 21:48:06', '[]');
INSERT INTO `game_records` VALUES (56, 10, 9, 1, 2, 1, '2025-06-28 21:53:20', '2025-06-28 21:53:23', '[[1, 10], [1, 12]]');
INSERT INTO `game_records` VALUES (57, 12, 10, 1, 2, 1, '2025-06-28 22:14:09', '2025-06-28 22:14:11', '[]');
INSERT INTO `game_records` VALUES (58, 12, 11, 1, 2, 1, '2025-06-28 22:14:13', '2025-06-28 22:14:16', '[]');
INSERT INTO `game_records` VALUES (59, 12, 12, 1, 2, 1, '2025-06-28 22:14:21', '2025-06-28 22:14:22', '[]');
INSERT INTO `game_records` VALUES (60, 12, 13, 1, 2, 1, '2025-06-28 22:14:27', '2025-06-28 22:14:29', '[]');
INSERT INTO `game_records` VALUES (61, 12, 14, 1, 2, 1, '2025-06-28 22:14:30', '2025-06-28 22:14:33', '[]');
INSERT INTO `game_records` VALUES (62, 12, 15, 1, 2, 1, '2025-06-28 22:14:47', '2025-06-28 22:14:49', '[]');
INSERT INTO `game_records` VALUES (63, 15, 16, 1, 2, 1, '2025-06-28 22:33:46', '2025-06-28 22:33:56', '[[2, 13], [2, 12], [3, 13], [3, 12], [4, 13], [4, 12], [5, 13], [5, 12], [6, 13]]');
INSERT INTO `game_records` VALUES (64, 15, 17, 2, 1, 2, '2025-06-28 22:34:00', '2025-06-28 22:34:10', '[[4, 8], [4, 9], [5, 8], [5, 9], [6, 8], [6, 9], [7, 8], [7, 9], [8, 8]]');
INSERT INTO `game_records` VALUES (65, 15, 18, 1, 2, 1, '2025-06-28 22:36:45', '2025-06-28 22:36:54', '[[5, 9], [5, 8], [6, 9], [6, 8], [7, 9], [7, 8], [8, 9], [8, 8], [9, 9]]');
INSERT INTO `game_records` VALUES (66, 16, 19, 1, 2, 1, '2025-06-28 22:42:03', '2025-06-28 22:42:12', '[[1, 13], [1, 12], [2, 13], [2, 12], [3, 13], [3, 12], [4, 13], [4, 12], [5, 13]]');
INSERT INTO `game_records` VALUES (67, 18, 20, 1, 2, 1, '2025-06-28 23:06:41', '2025-06-28 23:06:58', '[[1, 13], [1, 12], [2, 13], [2, 12], [3, 12], [3, 13], [4, 12], [4, 13], [5, 12], [5, 13], [6, 12], [6, 13], [7, 12]]');
INSERT INTO `game_records` VALUES (68, 19, 21, 1, 2, 2, '2025-06-28 23:10:38', '2025-06-28 23:10:40', '[]');
INSERT INTO `game_records` VALUES (69, 27, 22, 1, 2, 1, '2025-06-28 23:26:27', '2025-06-28 23:26:29', '[]');
INSERT INTO `game_records` VALUES (70, 28, 23, 1, 2, 2, '2025-06-28 23:32:17', '2025-06-28 23:32:22', '[[0, 14], [0, 13]]');
INSERT INTO `game_records` VALUES (71, 4, 1, 1, 2, 2, '2025-06-29 00:00:08', '2025-06-29 00:01:12', '[[3, 11], [3, 4]]');
INSERT INTO `game_records` VALUES (72, 3, 2, 1, 2, 2, '2025-06-29 00:07:39', '2025-06-29 00:07:43', '[]');

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
INSERT INTO `user` VALUES (3, '1234567', '1234567', '$2a$10$4Qyn8tSzhcmWjZz8QB5aLOa/v5QKSEIVJnHf.a4kNgpJQ6TUp3fMi', '1111', '2025-06-27 00:22:21');

SET FOREIGN_KEY_CHECKS = 1;
