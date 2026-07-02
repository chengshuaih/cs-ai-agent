package cn.chengshuai.csaiagent.auth;

import cn.chengshuai.csaiagent.constant.FileConstant;
import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户持久化（tmp/vision/users.json）+ 内存 token 缓存。
 * Token 重启失效，软著展示场景足够用。
 */
@Component
public class UserStore {

    private static final String USERS_FILE = FileConstant.FILE_SAVE_DIR + "/vision/users.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** token → userId */
    private final ConcurrentHashMap<String, String> tokenCache = new ConcurrentHashMap<>();

    private final Object lock = new Object();

    @PostConstruct
    public void init() {
        FileUtil.mkParentDirs(USERS_FILE);
    }

    public void save(UserInfo user) {
        synchronized (lock) {
            List<UserInfo> list = listAll();
            list.removeIf(u -> user.id().equals(u.id()));
            list.add(user);
            write(list);
        }
    }

    public Optional<UserInfo> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return listAll().stream().filter(u -> username.equals(u.username())).findFirst();
    }

    public Optional<UserInfo> findByToken(String token) {
        if (token == null) return Optional.empty();
        String userId = tokenCache.get(token);
        if (userId == null) return Optional.empty();
        return listAll().stream().filter(u -> userId.equals(u.id())).findFirst();
    }

    public void putToken(String token, String userId) {
        tokenCache.put(token, userId);
    }

    public void removeToken(String token) {
        tokenCache.remove(token);
    }

    private List<UserInfo> listAll() {
        File file = new File(USERS_FILE);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        try {
            var root = MAPPER.readTree(file);
            if (!root.isArray()) return new ArrayList<>();
            List<UserInfo> result = new ArrayList<>();
            for (var item : root) {
                try { result.add(MAPPER.treeToValue(item, UserInfo.class)); } catch (Exception ignored) {}
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void write(List<UserInfo> list) {
        try {
            FileUtil.writeUtf8String(MAPPER.writeValueAsString(list), USERS_FILE);
        } catch (Exception e) {
            throw new IllegalStateException("写入用户数据失败", e);
        }
    }
}
