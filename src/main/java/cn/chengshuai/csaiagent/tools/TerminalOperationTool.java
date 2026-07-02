package cn.chengshuai.csaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

/**
 * 终端操作工具（收敛为视觉数据整理用途）。
 *
 * <p>安全边界：仅放行白名单只读/整理类命令，拦截危险命令与管道/重定向/命令链等组合符号，
 * 避免直通 shell 带来的系统级风险。命中安全策略时返回提示而不执行。</p>
 */
public class TerminalOperationTool {

    /** 允许执行的命令白名单（仅限目录结构、查看、分场景归档、批量重命名等视觉数据整理用途）。 */
    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "ls", "pwd", "cat", "head", "tail", "mkdir", "mv", "cp", "find", "tree", "echo", "wc", "stat");

    /** 危险关键词，命中即拒绝。 */
    private static final List<String> DANGER_PATTERNS = List.of(
            "rm ", "rmdir", "mkfs", "dd ", "shutdown", "reboot", "kill", "chmod 777",
            "sudo", "> /dev", ":(){", "curl", "wget", "ssh", "scp", "chown");

    /** 命令链/管道/重定向等组合符号，默认拒绝（只放行白名单单命令）。 */
    private static final List<String> SHELL_OPERATORS = List.of("|", "&", ";", "`", "$(", ">", "<", "&&", "||");

    @Tool(description = "Execute a safe, whitelisted terminal command for organizing vision data (e.g. ls, find, mkdir, mv, cp). Dangerous or compound commands are rejected.")
    public String executeTerminalCommand(@ToolParam(description = "Command to execute in the terminal") String command) {
        String denyReason = checkSecurity(command);
        if (denyReason != null) {
            return "命令被安全策略拦截：" + denyReason;
        }

        StringBuilder output = new StringBuilder();
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            ProcessBuilder builder = isWindows
                    ? new ProcessBuilder("cmd.exe", "/c", command)
                    : new ProcessBuilder("/bin/sh", "-c", command);
            Process process = builder.redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("Command execution failed with exit code: ").append(exitCode);
            }
        } catch (IOException e) {
            output.append("Error executing command: ").append(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            output.append("Command execution interrupted: ").append(e.getMessage());
        }
        return output.toString();
    }

    /**
     * 安全校验，返回拒绝原因；通过则返回 null。
     */
    private String checkSecurity(String command) {
        if (command == null || command.isBlank()) {
            return "命令为空";
        }
        String trimmed = command.trim();

        for (String op : SHELL_OPERATORS) {
            if (trimmed.contains(op)) {
                return "不允许使用管道/重定向/命令链符号（" + op + "）";
            }
        }
        String lower = trimmed.toLowerCase();
        for (String danger : DANGER_PATTERNS) {
            if (lower.contains(danger)) {
                return "包含危险命令（" + danger.trim() + "）";
            }
        }
        if (trimmed.contains("..")) {
            return "不允许使用上级目录路径（..）";
        }
        String first = trimmed.split("\\s+")[0];
        if (!ALLOWED_COMMANDS.contains(first)) {
            return "命令 " + first + " 不在白名单内（仅允许 " + String.join("/", ALLOWED_COMMANDS) + "）";
        }
        return null;
    }
}
