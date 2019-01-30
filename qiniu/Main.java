package qiniu;

import java.io.File;

/**
 * 测试用
 * @author Yupi Li
 * @date 19/01/14
 */
public class Main {

    public static void main(String[] args) {
        // 上传文件
        String key = QiNiuClientUtils.uploadFile(new File("xxx"), "/picture/user/yupi.jpg");
        // 获取单个文件信息
        QiNiuClientUtils.getFileInfo("/picture/user/yupi.jpg");
        // 获取文件列表
        QiNiuClientUtils.getFileInfoList("/picture", null);
        // 删除文件
        QiNiuClientUtils.delFile(key);
    }

}
