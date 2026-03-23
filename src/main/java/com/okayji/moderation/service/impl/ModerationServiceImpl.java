package com.okayji.moderation.service.impl;

import com.okayji.file.service.S3Service;
import com.okayji.moderation.dto.ModerationVerdict;
import com.okayji.moderation.dto.request.OpenAiModerationRequest;
import com.okayji.moderation.dto.response.OpenAiModerationResponse;
import com.okayji.moderation.entity.*;
import com.okayji.moderation.service.ModerationService;
import org.springframework.ai.moderation.Categories;
import org.springframework.ai.moderation.CategoryScores;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.ai.moderation.ModerationResponse;
import org.springframework.ai.openai.OpenAiModerationModel;
import org.springframework.ai.openai.OpenAiModerationOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Service
public class ModerationServiceImpl implements ModerationService {
    // docs: https://developers.openai.com/api/docs/guides/moderation
    // docs: https://docs.spring.io/spring-ai/reference/api/moderation/openai-moderation.html
    // access: 05/03/2026

    private final String model;
    private final OpenAiModerationModel openAiModerationModel;
    private final OpenAiModerationOptions moderationOptions;
    private final RestClient openAiRestClient;
    private final S3Service s3Service;
    private final String ffmpegPath;

    public ModerationServiceImpl(
            OpenAiModerationModel openAiModerationModel,
            @Value("${spring.ai.openai.moderation.options.model}") String model,
            @Value("${spring.ai.openai.api-key}") String openaiApiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            S3Service s3Service,
            @Value("${app.ffmpeg.path}") String ffmpegPath) {
        this.openAiModerationModel = openAiModerationModel;
        this.model = model;

        this.moderationOptions = OpenAiModerationOptions.builder()
                .model(model)
                .build();

        this.openAiRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + openaiApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.s3Service = s3Service;
        this.ffmpegPath = ffmpegPath;
    }

    @Override
    public ModerationVerdict moderateText(String text) {
        ModerationPrompt prompt = new ModerationPrompt(text, moderationOptions);
        ModerationResponse response = openAiModerationModel.call(prompt);

        var moderation = response.getResult().getOutput();
        var first = moderation.getResults().getFirst();

        Map<String, Boolean> categories = flattenCategories(first.getCategories());
        Map<String, Double> scores = flattenScores(first.getCategoryScores());

        ModerationDecision decision = decide(scores);

        return new ModerationVerdict(
                decision,
                first.isFlagged(),
                categories,
                scores,
                null,
                "openai",
                moderation.getModel()
        );
    }

    @Override
    public ModerationVerdict moderateImageUrl(String imageUrl) {
        OpenAiModerationRequest body = OpenAiModerationRequest.from(model, imageUrl);

        OpenAiModerationResponse resp = openAiRestClient.post()
                .uri("/v1/moderations")
                .body(body)
                .retrieve()
                .body(OpenAiModerationResponse.class);

        if (resp == null || resp.results() == null || resp.results().isEmpty()) {
            throw new IllegalStateException("Empty moderation response");
        }

        OpenAiModerationResponse.Result r0 = resp.results().getFirst();
        ModerationDecision decision = decide(r0.category_scores());

        return new ModerationVerdict(
                decision,
                r0.flagged(),
                r0.categories(),
                r0.category_scores(),
                r0.category_applied_input_types(),
                "openai",
                resp.model()
        );
    }

    @Override
    public List<ModerationVerdict> moderateVideoUrl(String videoUrl) {
        Path tempVideo = null;
        List<Path> extractedFrames = new ArrayList<>();
        List<String> uploadedFrameUrls = new ArrayList<>();
        List<ModerationVerdict> responses = new ArrayList<>();
        try {
            tempVideo = s3Service.downloadToTempFile(videoUrl);
            extractedFrames = extractFrames(tempVideo, 5);

            for (Path frame : extractedFrames) {
                String frameUrl = s3Service.uploadTempFrame(
                        frame,
                        "image/jpeg",
                        "moderations"
                );
                uploadedFrameUrls.add(frameUrl);

                ModerationVerdict verdict = moderateImageUrl(frameUrl);
                responses.add(verdict);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to moderate video " + videoUrl, e);
        } finally {
            extractedFrames.forEach(this::deleteQuietly);
            deleteQuietly(tempVideo);
            uploadedFrameUrls.forEach(s3Service::deleteObject);
        }
        return responses;
    }

    private ModerationDecision decide(Map<String, Double> scores) {
        String[] categories = {
                "sexual",
                "sexual/minors",
                "harassment",
                "harassment/threatening",
                "hate",
                "hate/threatening",
                "self-harm",
                "self-harm/intent",
                "self-harm/instructions",
                "violence",
                "violence/graphic"
        };

        boolean review = false;
        for (String category : categories) {
            if (scores.getOrDefault(category, 0.0) > 0.7)
                return ModerationDecision.BLOCK;

            if (scores.getOrDefault(category, 0.0) > 0.4)
                review = true;
        }
        return review
                ? ModerationDecision.REVIEW
                : ModerationDecision.ALLOW;
    }

    private Map<String, Double> flattenScores(CategoryScores scores) {
        Map<String, Double> map = new HashMap<>();
        map.put("sexual", scores.getSexual());
        map.put("sexual/minors", scores.getSexualMinors());
        map.put("harassment", scores.getHarassment());
        map.put("harassment/threatening", scores.getHarassmentThreatening());
        map.put("hate", scores.getHate());
        map.put("hate/threatening", scores.getHateThreatening());
        map.put("self-harm", scores.getSelfHarm());
        map.put("self-harm/intent", scores.getSelfHarmIntent());
        map.put("self-harm/instructions", scores.getSelfHarmInstructions());
        map.put("violence", scores.getViolence());
        map.put("violence/graphic", scores.getViolenceGraphic());
        return map;
    }

    private Map<String, Boolean> flattenCategories(Categories categories) {
        Map<String, Boolean> map = new HashMap<>();
        map.put("sexual", categories.isSexual());
        map.put("sexual/minors", categories.isSexualMinors());
        map.put("harassment", categories.isHarassment());
        map.put("harassment/threatening", categories.isHarassmentThreatening());
        map.put("hate", categories.isHate());
        map.put("hate/threatening", categories.isHateThreatening());
        map.put("self-harm", categories.isSelfHarm());
        map.put("self-harm/intent", categories.isSelfHarmIntent());
        map.put("self-harm/instructions", categories.isSelfHarmInstructions());
        map.put("violence", categories.isViolence());
        map.put("violence/graphic", categories.isViolenceGraphic());
        return map;
    }

    private List<Path> extractFrames(Path videoPath, int everySeconds) {
        try {
            Path outputDir = Files.createTempDirectory("frames-");

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-loglevel", "error",
                    "-i", videoPath.toString(),
                    "-vf", "fps=1/" + everySeconds,
                    "-q:v", "5",
                    outputDir.resolve("frame_%05d.jpg").toString()
            );

            pb.redirectErrorStream(true);
            pb.inheritIO();

            Process process = pb.start();
            int exit = process.waitFor();

            if (exit != 0) {
                throw new RuntimeException("ffmpeg failed with exit code " + exit);
            }

            try (Stream<Path> stream = Files.list(outputDir)) {
                return stream.filter(p -> p.getFileName().toString().endsWith(".jpg"))
                        .sorted()
                        .toList();
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot extract frames from video", e);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            if (Files.isDirectory(path)) {
                try (Stream<Path> stream = Files.walk(path)) {
                    stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    });
                }
            } else {
                Files.deleteIfExists(path);
            }
        } catch (Exception ignored) {
        }
    }
}
