package com.okayji.file.service;

import com.okayji.file.dto.request.MultipartUploadCompleteRequest;
import com.okayji.file.dto.request.PresignedUrlRequest;
import com.okayji.file.dto.response.MultipartUploadInitResponse;
import com.okayji.file.dto.response.PresignedUrlResponse;

import java.nio.file.Path;

public interface S3Service {
    PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request);
    MultipartUploadInitResponse initMultipartUpload(PresignedUrlRequest request);
    String completeMultipartUpload(MultipartUploadCompleteRequest request);
    void abortMultipartUpload(String fileKey, String uploadId);
    Path downloadToTempFile(String mediaUrl);
    String uploadTempFrame(Path file, String contentType, String prefix);
    void deleteObject(String fileUrl);
    String getContentTypeFromS3Url(String s3Url);
}
