package cn.chengshuai.csaiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class   CsManusTest {

    @Resource
    private CsManus csManus;

    @Test
    void run() {
        String userPrompt = """
                我想在上海静安区做一次目标检测数据采集，请帮我推荐 5 公里内合适的采集点位，
                并结合一些网络样例图片，制定一份详细的数据采集计划，
                并以 PDF 格式输出""";
        String answer = csManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}
