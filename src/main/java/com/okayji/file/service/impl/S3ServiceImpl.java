package com.okayji.file.service.impl;

import com.okayji.exception.AppError;
import com.okayji.exception.AppException;
import com.okayji.file.dto.request.MultipartUploadCompleteRequest;
import com.okayji.file.dto.request.PresignedUrlRequest;
import com.okayji.file.dto.response.MultipartUploadInitResponse;
import com.okayji.file.dto.response.PartPresignedUrl;
import com.okayji.file.dto.response.PresignedUrlResponse;
import com.okayji.file.service.S3MediaTypes;
import com.okayji.file.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Uri;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "S3-SERVICE")
public class S3ServiceImpl implements S3Service {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Value("${presigned-url.expiration-minutes}")
    private int expirationMinutes;

    @Value("${multipart-url.expiration-minutes}")
    private int multipartExpirationMinutes;

    @Value("${file.max-size.image}")
    private int imageMaxSize;

    @Value("${file.max-size.video}")
    private int videoMaxSize;

    @Value("${file.max-size.other}")
    private int otherMaxSize;

    @Value("${file.max-size.multipart}")
    private int multipartUploadMaxSize;

    private final int PART_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    public PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request) {
        // Validate file size
        long maxSize = getMaxFileSizeForType(request.getFileType());
        if (request.getFileSize() > maxSize)
            throw new AppException(AppError.MAX_FILE_SIZE);

        // Validate file type
        if (!S3MediaTypes.isAllowedFileType(request.getFileType()))
            throw new AppException(AppError.FILE_NOT_ALLOW);

        String fileExtension = getFileExtension(request.getFileName());
        String category = determineCategoryFromType(request.getFileType());
        // By category: images/, videos/, files/
        String uniqueKey = String.format("%s/%s/%s.%s",
                category,
                LocalDate.now(),
                UUID.randomUUID(),
                fileExtension);

        // Generate presigned URL
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .putObjectRequest(builder -> builder
                        .bucket(bucketName)
                        .key(uniqueKey)
                        .contentType(request.getFileType())
                        .contentLength(request.getFileSize())
                        .build())
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        String presignedUrl = presignedRequest.url().toString();
        String publicUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, uniqueKey);

        log.info("Generated presigned URL for key: {}", uniqueKey);

        return PresignedUrlResponse.builder()
                .presignedUrl(presignedUrl)
                .fileKey(uniqueKey)
                .publicUrl(publicUrl)
                .expiresIn(expirationMinutes * 60)
                .build();
    }

    @Override
    public MultipartUploadInitResponse initMultipartUpload(PresignedUrlRequest request) {
        // Validate file size
        if (request.getFileSize() > (long) multipartUploadMaxSize * 1024 * 1024)
            throw new AppException(AppError.MAX_FILE_SIZE);

        // Validate file type
        if (!S3MediaTypes.isAllowedFileType(request.getFileType()))
            throw new AppException(AppError.FILE_NOT_ALLOW);

        int numParts = (int) Math.ceil((double) request.getFileSize() / PART_SIZE);

        String fileExtension = getFileExtension(request.getFileName());
        String category = determineCategoryFromType(request.getFileType());
        String uniqueKey = String.format("%s/%s/%s.%s",
                category,
                LocalDate.now(),
                UUID.randomUUID(),
                fileExtension);

        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(uniqueKey)
                .contentType(request.getFileType())
                .build();

        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();
        log.info("Initiated multipart upload. UploadId: {}, Key: {}", uploadId, uniqueKey);

        List<PartPresignedUrl> partUrls = new ArrayList<>();
        for (int i = 1; i <= numParts; i++) {
            int finalI = i;
            UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(multipartExpirationMinutes))
                    .uploadPartRequest(builder -> builder
                            .bucket(bucketName)
                            .key(uniqueKey)
                            .uploadId(uploadId)
                            .partNumber(finalI)
                            .build())
                    .build();

            PresignedUploadPartRequest presignedRequest = s3Presigner.presignUploadPart(presignRequest);

            partUrls.add(PartPresignedUrl.builder()
                    .partNumber(i)
                    .presignedUrl(presignedRequest.url().toString())
                    .build());
        }

        String publicUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, uniqueKey);

        return MultipartUploadInitResponse.builder()
                .uploadId(uploadId)
                .fileKey(uniqueKey)
                .parts(partUrls)
                .partSize(PART_SIZE)
                .publicUrl(publicUrl)
                .build();
    }

    @Override
    public String completeMultipartUpload(MultipartUploadCompleteRequest request) {
        log.info("Completing multipart upload. UploadId: {}, Parts: {}",
                request.getUploadId(), request.getParts().size());

        List<CompletedPart> completedParts = request.getParts().stream()
                .map(part -> CompletedPart.builder()
                        .partNumber(part.getPartNumber())
                        .eTag(part.getEtag())
                        .build())
                .toList();

        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(request.getFileKey())
                .uploadId(request.getUploadId())
                .multipartUpload(builder -> builder
                        .parts(completedParts)
                        .build())
                .build();

        try {
            CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(completeRequest);
            log.info("Multipart upload completed successfully. Location: {}",
                    response.location());
            return response.location();
        } catch (S3Exception e) {
            log.error("Failed to complete multipart upload: {}", e.getMessage());
            throw new AppException(AppError.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public void abortMultipartUpload(String fileKey, String uploadId) {
        AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .uploadId(uploadId)
                .build();
        try {
            s3Client.abortMultipartUpload(abortRequest);
            log.info("Aborted multipart upload. UploadId: {}", uploadId);
        } catch (S3Exception e) {
            log.error("Failed to abort multipart upload: {}", e.getMessage());
        }
    }

    @Override
    public Path downloadToTempFile(String mediaUrl) {
        String fileKey = mediaUrl;
        try {
            fileKey = extractFileKeyFromUrl(mediaUrl);
            String ext = fileKey.contains(".") ? fileKey.substring(fileKey.lastIndexOf('.')) : ".bin";

            Path tempDir = Files.createTempDirectory("video-download-");
            Path tempFile = tempDir.resolve(UUID.randomUUID() + ext); // path chưa tồn tại

            log.info("Downloading to temp: fileKey={}, tempFile={}", fileKey, tempFile);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.getObject(request, ResponseTransformer.toFile(tempFile));
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("Cannot download file from S3: " + fileKey, e);
        }
    }

    @Override
    public String uploadTempFrame(Path file, String contentType, String prefix) {
        try {
            String key = String.format("%s/%s/%s.jpg",
                    prefix,
                    LocalDate.now(),
                    UUID.randomUUID());

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, file);
            return String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, region, key);
        } catch (Exception e) {
            throw new RuntimeException("Cannot upload temp frame to S3", e);
        }
    }

    @Override
    public void deleteObject(String fileUrl) {
        try {
            String fileKey = extractFileKeyFromUrl(fileUrl);
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(request);
        } catch (RuntimeException e) {
            log.warn("Cannot delete S3 object {}", fileUrl, e);
        }
    }

    @Override
    public String getContentTypeFromS3Url(String s3Url) {
        try {
            S3Uri s3Uri = s3Client.utilities().parseUri(URI.create(s3Url));

            String bucket = s3Uri.bucket()
                    .orElseThrow(() -> new IllegalArgumentException("Missing bucket"));
            if (!bucket.equals(bucketName))
                throw new IllegalArgumentException("Invalid bucket");
            String key = s3Uri.key()
                    .orElseThrow(() -> new IllegalArgumentException("Missing key"));

            HeadObjectResponse head = s3Client.headObject(b -> b.bucket(bucket).key(key));
            return head.contentType();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) return "";
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    private String determineCategoryFromType(String contentType) {
        if (contentType.startsWith("image/"))
            return "images";
        if (contentType.startsWith("video/"))
            return "videos";
        return "files";
    }

    private long getMaxFileSizeForType(String contentType) {
        if (contentType.startsWith("image/"))
            return (long) imageMaxSize * 1024 * 1024;
        if (contentType.startsWith("video/"))
            return (long) videoMaxSize * 1024 * 1024;
        return (long) otherMaxSize * 1024 * 1024;
    }

    public String extractFileKeyFromUrl(String mediaUrl) {
        try {
            URI uri = URI.create(mediaUrl);
            String path = uri.getPath();
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (RuntimeException e) {
            return null;
        }
    }

}
