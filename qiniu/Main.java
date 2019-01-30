package qiniu;

import java.io.File;

/**
 * 测试用
 * @author Yupi Li
 * @date 19/01/14
 */
public class Main {

    public static void main(String[] args) {
        String key = QiNiuClientUtils.uploadFile(new File("xxx"));
        QiNiuClientUtils.delFile(key);
    }

}
