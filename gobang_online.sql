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

 Date: 29/06/2025 00:10:59
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

SET FOREIGN_KEY_CHECKS = 1;
