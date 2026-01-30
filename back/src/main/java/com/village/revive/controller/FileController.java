package com.village.revive.controller;

import com.village.revive.utils.AliOssUtil;
import com.village.revive.utils.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/file")
@CrossOrigin
@AllArgsConstructor
public class FileController {

    private final AliOssUtil aliOssUtil;

    /**
     * 单文件上传
     */
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.fail("文件为空");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        String namrPart = originalFilename.substring(0, originalFilename.lastIndexOf("."));
        String newFileName = namrPart+"-"+UUID.randomUUID() + ext;
        log.info("上传文件: " + newFileName);
        // OSS 对象名（保持你原来的目录结构）
        String objectName = "uploads/" + newFileName;

        aliOssUtil.upload(file.getBytes(), objectName);

        // 返回你的下载接口路径
        return Result.success("/api/file/download/" + newFileName);
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{fileName}")
    public void downloadFile(@PathVariable String fileName, HttpServletResponse response) throws IOException {

        String objectName = "uploads/" + fileName;

        InputStream inputStream = aliOssUtil.getInputStream(objectName);

        if (inputStream == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = determineContentType(fileName);
        response.setContentType(contentType);

        if (contentType.startsWith("image/")) {
            response.setHeader("Content-Disposition",
                    "inline;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        } else {
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        }

        OutputStream os = response.getOutputStream();
        byte[] buffer = new byte[4096];
        int len;

        while ((len = inputStream.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }

        os.flush();
    }

    /**
     * 批量上传
     */
    @PostMapping("/upload/batch")
    public Result<String[]> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.fail("未选择任何文件");
        }

        String[] urls = new String[files.length];

        for (int i = 0; i < files.length; i++) {
            try {
                Result<String> result = uploadFile(files[i]);
                if (result.getCode() == 200) {
                    urls[i] = result.getData();
                } else {
                    return Result.fail("批量上传失败：" + result.getMessage());
                }
            } catch (Exception e) {
                log.error("批量上传失败", e);
                return Result.fail("批量上传失败");
            }
        }

        return Result.success(urls);
    }

    private String determineContentType(String fileName) {
        fileName = fileName.toLowerCase();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".bmp")) return "image/bmp";
        if (fileName.endsWith(".webp")) return "image/webp";
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }
}