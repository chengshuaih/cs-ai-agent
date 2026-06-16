package cn.chengshuai.csaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FileOperationToolTest {

    @Test
    void readFile() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        String fileName = "恋爱智能体.txt";
        String readFile = fileOperationTool.readFile(fileName);
        Assertions.assertNotNull(readFile);

    }

    @Test
    void writeFile() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        String fileName = "恋爱智能体.txt";
        String content = "这是一个恋爱智能体！！！！！！！！！！！";
        String writeFile = fileOperationTool.writeFile(fileName, content);
        Assertions.assertNotNull(writeFile);
    }
}