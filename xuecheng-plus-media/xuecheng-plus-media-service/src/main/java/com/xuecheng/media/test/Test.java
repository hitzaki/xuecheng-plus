package com.xuecheng.media.test;

import io.minio.*;
import io.minio.errors.MinioException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Test {
    static MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();
    //上传文件
    public static void upload()throws IOException,
            NoSuchAlgorithmException, InvalidKeyException {
        try {
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket("testbucket").build());
                            //检查 testbucket 桶是否创建，没有创建自动创建
            if (!found) {

                minioClient.makeBucket(MakeBucketArgs.builder().bucket("testbucket")
                        .build());
            } else {
                System.out.println("Bucket 'testbucket' already exists.");
            }
            //上传 1.avi,上传到 avi 子目录
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket("testbucket")
                            .object("avi/1.jpg")
                            .filename("C:\\Users\\CHEN\\Desktop\\home.jpg")
                            .build());
            System.out.println("上传成功");
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
            System.out.println("HTTP trace: " + e.httpTrace());
        }
    }

    //删除文件
    public static void delete(String bucket,String filepath)throws
            IOException, NoSuchAlgorithmException, InvalidKeyException {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(filepath).build());
            System.out.println("删除成功");
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
            System.out.println("HTTP trace: " + e.httpTrace());
        }
    }

    //下载文件
    public static void getFile(String bucket,String filepath,String outFile)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        try {
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(filepath)
                            .build());
                 FileOutputStream fileOutputStream = new FileOutputStream(new
                         File(outFile));
            ) {
                // Read data from stream
                IOUtils.copy(stream,fileOutputStream);
                System.out.println("下载成功");
            }
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
            System.out.println("HTTP trace: " + e.httpTrace());
        }
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        upload();
    }

    public void testChunk() throws IOException {
        File sourceFile = new File("d:/develop/bigfile_test/nacos.avi");
        String chunkPath = "d:/develop/bigfile_test/chunk/";
        File chunkFolder = new File(chunkPath);
        if (!chunkFolder.exists()) {
            chunkFolder.mkdirs();
        }
        //分块大小
        long chunkSize = 1024 * 1024 * 1;
        //分块数量
        long chunkNum = (long) Math.ceil(sourceFile.length() * 1.0 / chunkSize);
        System.out.println("分块总数："+chunkNum);
        //缓冲区大小
        byte[] b = new byte[1024];
        //使用 RandomAccessFile 访问文件
        RandomAccessFile raf_read = new RandomAccessFile(sourceFile, "r");
        //分块
        for (int i = 0; i < chunkNum; i++) {
            //创建分块文件
            File file = new File(chunkPath + i);
            if(file.exists()){
                file.delete();
            }
            boolean newFile = file.createNewFile();
            if (newFile) {
                //向分块文件中写数据
                RandomAccessFile raf_write = new RandomAccessFile(file, "rw");
                int len = -1;
                while ((len = raf_read.read(b)) != -1) {
                    raf_write.write(b, 0, len);
                    if (file.length() >= chunkSize) {
                        break;
                    }
                }
                raf_write.close();
                System.out.println("完成分块"+i);
            }
        }
        raf_read.close();
    }

    public void testMerge() throws IOException {
        //块文件目录
        File chunkFolder = new File("d:/develop/bigfile_test/chunk/");
        //原始文件
        File originalFile = new File("d:/develop/bigfile_test/nacos.avi");
        //合并文件
        File mergeFile = new File("d:/develop/bigfile_test/nacos01.avi");
        if (mergeFile.exists()) {
            mergeFile.delete();
        }
        //创建新的合并文件
        mergeFile.createNewFile();
        //用于写文件
        RandomAccessFile raf_write = new RandomAccessFile(mergeFile, "rw");
        //指针指向文件顶端
        raf_write.seek(0);
        //缓冲区
        byte[] b = new byte[1024];
        //分块列表
        File[] fileArray = chunkFolder.listFiles();
        // 转成集合，便于排序
        List<File> fileList = Arrays.asList(fileArray);
        // 从小到大排序
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName()) -
                        Integer.parseInt(o2.getName());
            }
        });
        //合并文件
        for (File chunkFile : fileList) {
            RandomAccessFile raf_read = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len = raf_read.read(b)) != -1) {
                raf_write.write(b, 0, len);
            }
            raf_read.close();
        }
        raf_write.close();
        //校验文件
        try (
                FileInputStream fileInputStream = new FileInputStream(originalFile);
                FileInputStream mergeFileStream = new FileInputStream(mergeFile);
        ) {
            //取出原始文件的 md5
            String originalMd5 = DigestUtils.md5Hex(fileInputStream);
            //取出合并文件的 md5 进行比较
            String mergeFileMd5 = DigestUtils.md5Hex(mergeFileStream);
            if (originalMd5.equals(mergeFileMd5)) {
                System.out.println("合并文件成功");
            } else {
                System.out.println("合并文件失败");
            }
        }
    }




}
