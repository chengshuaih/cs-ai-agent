package cn.chengshuai.csaiagent.auth;

/**
 * 用户信息（本地 JSON 持久化）。
 */
public record UserInfo(String id, String username, String passwordHash, String createdAt) {
}
