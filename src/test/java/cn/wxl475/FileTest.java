package cn.wxl475;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

@Slf4j
public class FileTest {

    @Test
    public void fileCutting() throws IOException {
        RandomAccessFile input = new RandomAccessFile("D:/obs录像/2024-01-03 22-56-52.mkv", "r");
        RandomAccessFile output1 = new RandomAccessFile("D:/桌面/1", "rw");
        RandomAccessFile output2 = new RandomAccessFile("D:/桌面/2", "rw");
        long length1 = input.length()/2;
        long length2 = input.length() - length1;
        byte[] bytes = new byte[(int) input.length()];
        input.read(bytes);
        input.close();
        byte[] bytes1 = new byte[(int) length1];
        byte[] bytes2 = new byte[(int) length2];
        System.arraycopy(bytes, 0, bytes1, 0, (int) length1);
        System.arraycopy(bytes, (int) length1, bytes2, 0, (int) length2);
        output1.write(bytes1);
        output2.write(bytes2);
        output1.close();
        output2.close();
    }
    @Test
    public void imageConvert(){
        ImgUtil.convert(FileUtil.file("D:/学期资料/图集/新建文件夹/1 (2).jpg"), FileUtil.file("D:/学期资料/图集/新建文件夹/1 (2).png"));
    }

    @Test
    public void scale(){
        File file = FileUtil.file("D:/学期资料/图集/新建文件夹/1 (2).jpg");
        File file1 = FileUtil.file("D:/学期资料/图集/新建文件夹/1.jpg");
        ImgUtil.scale(file,file1, 0.1F);
    }
}
