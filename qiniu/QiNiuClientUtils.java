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
import com.qiniu.util.Auth;

import java.io.File;

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
     * 上传管理
     */
    private static UploadManager uploadManager;

    /**
     * token
     */
    private static String upToken;

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
        upToken = auth.uploadToken(BUCKET);
        bucketManager = new BucketManager(auth, cfg);
    }

    /**
     * 文件上传
     * 文件名默认为空
     * @param file 文件
     * @return 文件外链
     */
    public static String uploadFile(File file) {
        return uploadFile(file, null);
    }

    /**
     * 文件上传（指定文件名）
     * @param file     文件
     * @param filename 文件名
     * @return 文件外链
     */
    public static String uploadFile(File file, String filename) {
        try {
            Response response = uploadManager.put(file, filename, upToken);
            // 解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
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
}
