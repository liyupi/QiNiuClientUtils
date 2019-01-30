package qiniu;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.BatchStatus;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * 七牛云存储客户端工具类
 * @author Yupi Li
 * @date 19-01-30
 */
public class QiNiuClientUtils {

    /**
     * 可以自己创建七牛常量类
     */
    private final static String ASSESS_KEY = QiNiuConstant.ASSESS_KEY;
    private final static String SECRET_KEY = QiNiuConstant.SECRET_KEY;
    private final static String BUCKET = QiNiuConstant.BUCKET;

    /**
     * token过期时间（s）
     */
    private static Long expires = 7200L;

    /**
     * token
     */
    private static String upToken;

    /**
     * 上传管理
     */
    private static UploadManager uploadManager;

    /**
     * 桶管理
     */
    private static BucketManager bucketManager;


    // 初始化静态对象
    static {
        // 根据机房执行不同的Zone方法
        Configuration cfg = new Configuration(Zone.zone2());
        uploadManager = new UploadManager(cfg);
        // 生成上传凭证，然后准备上传
        Auth auth = Auth.create(ASSESS_KEY, SECRET_KEY);
        upToken = auth.uploadToken(BUCKET, null, expires, null);
        bucketManager = new BucketManager(auth, cfg);
        // 定时重新获取token
        Thread refreshTokenThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(expires * 1000 - 10);
                    upToken = auth.uploadToken(BUCKET, null, expires, null);
                    System.out.println("get token succeed");
                } catch (Exception e) {
                    // 应该换成日志记录
                    System.out.println("get token failed");
                }
            }
        }, "refreshToken");
        refreshTokenThread.setDaemon(true);
        refreshTokenThread.start();
    }

    /**
     * 文件上传（支持多种方式：InputStream/File）
     * 文件名默认为空
     * @param file 文件
     * @return 文件外链
     */
    public static String uploadFile(Object file) {
        return uploadFile(file, null);
    }

    /**
     * 文件上传（指定文件名）
     * @param file     文件
     * @param filename 文件名
     * @return 文件外链
     */
    public static String uploadFile(Object file, String filename) {
        try {
            Response response = null;
            if (file instanceof File) {
                response = uploadManager.put((File) file, filename, upToken);
            } else if (file instanceof InputStream) {
                response = uploadManager.put((InputStream) file, filename, upToken, null, null);
            }
            // 解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(Objects.requireNonNull(response).bodyString(), DefaultPutRet.class);
            System.out.println(putRet.key + ": upload success");
            return putRet.key;
        } catch (QiniuException ex) {
            Response r = ex.response;
            System.err.println(r.toString());
            try {
                System.err.println(r.bodyString());
            } catch (QiniuException ex2) {
                // ignore
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 删除文件（支持多个）
     * @param filenames 文件名列表
     * @return 是否删除成功
     */
    public static boolean delFile(String... filenames) {
        try {
            BucketManager.BatchOperations batchOperations = new BucketManager.BatchOperations();
            batchOperations.addDeleteOp(BUCKET, filenames);
            Response response = bucketManager.batch(batchOperations);
            BatchStatus[] batchStatusList = response.jsonToObject(BatchStatus[].class);
            for (int i = 0; i < filenames.length; i++) {
                BatchStatus status = batchStatusList[i];
                // 打印些提示信息
                String key = filenames[i];
                System.out.print(key + ": ");
                if (status.code == 200) {
                    System.out.println("delete success");
                } else {
                    System.out.println(status.data.error);
                }
            }
            return true;
        } catch (QiniuException ex) {
            System.err.println(ex.response.toString());
        }
        return false;
    }

    /**
     * 获取指定文件信息
     * @param filename 文件名
     */
    public static void getFileInfo(String filename) {
        try {
            FileInfo fileInfo = bucketManager.stat(BUCKET, filename);
            System.out.println(fileInfo.hash);
            System.out.println(fileInfo.fsize);
            System.out.println(fileInfo.mimeType);
            System.out.println(fileInfo.putTime);
        } catch (QiniuException ex) {
            System.err.println(ex.response.toString());
        }
    }

    /**
     * 获取指定目录信息
     * 描述：七牛云存储中没有目录的概念，通过分割key来模拟目录，按前缀查找
     * @param dirname 查找目录（完整目录路径）
     * @param limit 最大结果数
     */
    public static void getFileInfoList(String dirname, Integer limit) {
        // limit默认为1000
        limit = Optional.ofNullable(limit).orElse(1000);
        // 指定目录分隔符，列出所有公共前缀（模拟列出目录效果）
        String delimiter = "";
        // 列举空间文件列表
        BucketManager.FileListIterator fileListIterator = bucketManager.createFileListIterator(BUCKET, dirname, limit, delimiter);
        while (fileListIterator.hasNext()) {
            // 可自定义对fileInfo的处理
            FileInfo[] items = fileListIterator.next();
            for (FileInfo item : items) {
                System.out.println(item.key);
                System.out.println(item.hash);
                System.out.println(item.fsize);
                System.out.println(item.mimeType);
                System.out.println(item.putTime);
                System.out.println(item.endUser);
            }
        }
    }
}
