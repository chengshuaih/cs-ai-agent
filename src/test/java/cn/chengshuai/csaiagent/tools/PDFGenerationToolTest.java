package cn.chengshuai.csaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PDFGenerationToolTest {

    @Test
    public void testGeneratePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "机器视觉智能体项目.pdf";
        String content = "机器视觉智能体项目 https://www.bilibili.com";
        String result = tool.generatePDF(fileName, content);
        assertNotNull(result);
    }
}
