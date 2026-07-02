package cn.chengshuai.csaiagent.controller;

import cn.chengshuai.csaiagent.auth.UserInfo;
import cn.chengshuai.csaiagent.auth.UserStore;
import cn.chengshuai.csaiagent.common.ApiResponse;
import cn.chengshuai.csaiagent.common.ResultCode;
import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 用户认证接口：注册、登录、获取当前用户信息。
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private static final DateTimeFormatter ID_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final UserStore userStore;

    public AuthController(UserStore userStore) {
        this.userStore = userStore;
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, String>> register(@RequestBody AuthRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            return ApiResponse.error(ResultCode.PARAM_INVALID, "用户名和密码不能为空");
        }
        if (userStore.findByUsername(request.username()).isPresent()) {
            return ApiResponse.error(ResultCode.PARAM_INVALID, "用户名已存在");
        }
        String id = "user-" + LocalDateTime.now().format(ID_FMT);
        String hash = BCrypt.hashpw(request.password());
        UserInfo user = new UserInfo(id, request.username(), hash, LocalDateTime.now().toString());
        userStore.save(user);
        log.info("用户注册成功: {}", request.username());
        return ApiResponse.success(Map.of("id", id, "username", request.username()));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody AuthRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            return ApiResponse.error(ResultCode.PARAM_INVALID, "用户名和密码不能为空");
        }
        var userOpt = userStore.findByUsername(request.username());
        if (userOpt.isEmpty() || !BCrypt.checkpw(request.password(), userOpt.get().passwordHash())) {
            return ApiResponse.error(ResultCode.PARAM_INVALID, "用户名或密码错误");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        userStore.putToken(token, userOpt.get().id());
        log.info("用户登录成功: {}", request.username());
        return ApiResponse.success(Map.of("token", token, "username", request.username()));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, String>> me(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        if (token == null) {
            return ApiResponse.error(ResultCode.PARAM_INVALID, "未登录");
        }
        var userOpt = userStore.findByToken(token);
        if (userOpt.isEmpty()) {
            return ApiResponse.error(ResultCode.PARAM_INVALID, "token 无效或已过期");
        }
        UserInfo user = userOpt.get();
        return ApiResponse.success(Map.of("id", user.id(), "username", user.username()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        if (token != null) {
            userStore.removeToken(token);
        }
        return ApiResponse.success(null);
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public record AuthRequest(String username, String password) {}
}
