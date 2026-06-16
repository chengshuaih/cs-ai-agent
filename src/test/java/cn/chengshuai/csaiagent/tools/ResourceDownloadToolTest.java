package cn.chengshuai.csaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ResourceDownloadToolTest {

    @Test
    public void testDownloadResource() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String url = "https://www.whut.edu.cn/images/whutlogo.png";
        String fileName = "whutlogo.png";
        String result = tool.downloadResource(url, fileName);
        assertNotNull(result);
    }
}
