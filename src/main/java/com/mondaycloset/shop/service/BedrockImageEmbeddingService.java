package com.mondaycloset.shop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import java.util.Base64;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * AWS Bedrock Amazon Nova Multimodal Embeddings로 이미지를 벡터로 변환한다(이미지 유사도 검색용).
 * 이 모델은 us-east-1 In-Region 전용이라(2026년 기준 리전 간 라우팅 미지원) app.bedrock.region을
 * 다른 값으로 바꾸면 안 된다. app.bedrock.enabled 가 false 면 클라이언트를 아예 만들지 않고 항상
 * 빈 결과를 반환한다 - Bedrock 모델 액세스/자격증명이 없는 환경(로컬 개발 등)에서도 앱은 정상 기동해야 한다.
 * 자격증명은 AWS SDK 기본 체인을 사용한다 - EC2에서는 인스턴스에 연결된 IAM 역할을 그대로 쓰므로
 * 액세스키를 서버에 따로 보관하지 않아도 된다.
 */
@Slf4j
@Service
public class BedrockImageEmbeddingService {

    private static final String MODEL_ID = "amazon.nova-2-multimodal-embeddings-v1:0";
    private static final int EMBEDDING_DIMENSION = 1024;
    private static final String DETAIL_LEVEL = "STANDARD_IMAGE";

    /** 색인용(카탈로그 상품 이미지)과 검색어용(사용자가 업로드한 쿼리 이미지)은 최적화 목적이 다르다 - Nova 권장 사용법. */
    public enum Purpose {
        INDEX("GENERIC_INDEX"),
        QUERY("IMAGE_RETRIEVAL");

        private final String value;

        Purpose(String value) {
            this.value = value;
        }
    }

    @Value("${app.bedrock.enabled:false}")
    private boolean enabled;

    @Value("${app.bedrock.region:us-east-1}")
    private String region;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BedrockRuntimeClient client;

    @PostConstruct
    void init() {
        if (enabled) {
            client = BedrockRuntimeClient.builder()
                    .region(Region.of(region))
                    .build();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** 이미지 바이트를 임베딩 벡터로 변환한다. 비활성화 상태이거나 호출/형식 인식이 실패하면 빈 값을 반환한다(예외를 던지지 않음). */
    public Optional<float[]> embed(byte[] imageBytes, Purpose purpose) {
        if (!enabled) {
            return Optional.empty();
        }
        String format = detectFormat(imageBytes);
        if (format == null) {
            log.warn("[Bedrock] 지원하지 않는 이미지 형식(PNG/JPEG/GIF/WEBP만 지원)");
            return Optional.empty();
        }
        try {
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("schemaVersion", "nova-multimodal-embed-v1");
            requestNode.put("taskType", "SINGLE_EMBEDDING");
            ObjectNode params = requestNode.putObject("singleEmbeddingParams");
            params.put("embeddingPurpose", purpose.value);
            params.put("embeddingDimension", EMBEDDING_DIMENSION);
            ObjectNode image = params.putObject("image");
            image.put("format", format);
            image.put("detailLevel", DETAIL_LEVEL);
            image.putObject("source").put("bytes", Base64.getEncoder().encodeToString(imageBytes));

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(MODEL_ID)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(requestNode)))
                    .build();

            InvokeModelResponse response = client.invokeModel(request);
            JsonNode embeddingNode = objectMapper.readTree(response.body().asUtf8String())
                    .get("embeddings").get(0).get("embedding");
            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }
            return Optional.of(vector);
        } catch (Exception e) {
            log.warn("[Bedrock] 이미지 임베딩 계산 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** 매직 바이트로 실제 이미지 형식을 판별한다 - URL 확장자나 브라우저가 보낸 Content-Type은 신뢰할 수 없다. */
    private String detectFormat(byte[] bytes) {
        if (bytes.length < 12) {
            return null;
        }
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "png";
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return "gif";
        }
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }

    /** float[] <-> JSON 문자열(DB 저장 형태) 변환. */
    public String toJson(float[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new IllegalStateException("임베딩 직렬화 실패", e);
        }
    }

    public Optional<float[]> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, float[].class));
        } catch (Exception e) {
            log.warn("[Bedrock] 저장된 임베딩 역직렬화 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** 코사인 유사도(-1~1, 1이 완전히 동일). 두 벡터 차원이 다르면 0을 반환한다(비교 불가). */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
