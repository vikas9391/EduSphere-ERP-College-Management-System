package com.collegeerp.Backend.timetable.service;

import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.timetable.dto.TimetableImportResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class TimetableImportService {

    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final List<String> ALLOWED_TYPES = List.of("image/png", "image/jpeg", "image/webp", "application/pdf");

    private final ClassSubjectRepository classSubjectRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public TimetableImportService(
            ClassSubjectRepository classSubjectRepository,
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.timetable-model:gpt-5-mini}") String model) {
        this.classSubjectRepository = classSubjectRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public TimetableImportResponse inspect(MultipartFile file, UserPrincipal principal) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Upload a timetable image or PDF");
        if (file.getSize() > MAX_FILE_BYTES) throw new BadRequestException("Timetable file must be 10 MB or smaller");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) throw new BadRequestException("Supported timetable files are PNG, JPEG, WebP and PDF");
        if (apiKey.isBlank()) throw new BadRequestException("Timetable AI import is not configured. Set OPENAI_API_KEY on the backend.");

        List<ClassSubject> subjects = "TEACHER".equalsIgnoreCase(principal.getRole())
                ? classSubjectRepository.findAllByTeacherId(principal.getId())
                : classSubjectRepository.findAllWithRelations();
        if (subjects.isEmpty()) throw new BadRequestException("No class subjects are available for timetable import");

        try {
            String catalog = buildCatalog(subjects);
            String requestBody = buildRequestBody(file, contentType, catalog);
            String response = restClient.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            return parseResponse(response, subjects);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Could not inspect the timetable. Check the file and OpenAI backend configuration.");
        }
    }

    private String buildCatalog(List<ClassSubject> subjects) throws IOException {
        List<Object> catalog = new ArrayList<>();
        for (ClassSubject cs : subjects) {
            catalog.add(new Object[]{
                    cs.getId(),
                    cs.getSchoolClass().getName(),
                    cs.getSubjectName(),
                    cs.getSubjectCode(),
                    cs.getTeacher() == null ? null : (cs.getTeacher().getFirstName() + " " + cs.getTeacher().getLastName()).trim()
            });
        }
        return objectMapper.writeValueAsString(catalog);
    }

    private String buildRequestBody(MultipartFile file, String contentType, String catalog) throws IOException {
        String instruction = "Inspect this college timetable and extract every scheduled class slot you can read. " +
                "Match each slot to exactly one class subject from the supplied catalog using classSubjectId; never invent an ID. " +
                "Return ONLY valid JSON with this shape: {\"entries\":[{\"classSubjectId\":1,\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"10:00\",\"room\":\"204\",\"confidence\":0.95}],\"warnings\":[\"text\"]}. " +
                "Use 24-hour HH:mm times and uppercase English day names. Omit unreadable or ambiguous slots and explain them in warnings. " +
                "Catalog rows are [classSubjectId, className, subjectName, subjectCode, teacherName]. Catalog: " + catalog;

        List<Object> content = new ArrayList<>();
        content.add(new Object[]{"type", "input_text", "text", instruction});
        if (contentType.equals("application/pdf")) {
            String fileId = uploadPdf(file);
            content.add(new Object[]{"type", "input_file", "file_id", fileId});
        } else {
            String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
            content.add(new Object[]{"type", "input_image", "image_url", dataUrl, "detail", "high"});
        }

        // Use a tree rather than hand-written JSON string escaping.
        var root = objectMapper.createObjectNode();
        root.put("model", model);
        var input = root.putArray("input");
        var message = input.addObject();
        message.put("role", "user");
        var parts = message.putArray("content");
        for (int i = 0; i < content.size(); i++) {
            Object[] values = (Object[]) content.get(i);
            var part = parts.addObject();
            for (int j = 0; j < values.length; j += 2) part.put(String.valueOf(values[j]), String.valueOf(values[j + 1]));
        }
        var text = root.putObject("text");
        var format = text.putObject("format");
        format.put("type", "json_object");
        return objectMapper.writeValueAsString(root);
    }

    private String uploadPdf(MultipartFile file) throws IOException {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override public String getFilename() { return file.getOriginalFilename() == null ? "timetable.pdf" : file.getOriginalFilename(); }
        };
        parts.add("file", resource);
        parts.add("purpose", "user_data");
        String response = restClient.post()
                .uri("/files")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .body(String.class);
        try {
            JsonNode node = objectMapper.readTree(response);
            String id = node.path("id").asText("");
            if (id.isBlank()) throw new BadRequestException("OpenAI did not accept the timetable PDF");
            return id;
        } catch (IOException e) {
            throw new BadRequestException("OpenAI returned an invalid PDF upload response");
        }
    }

    private TimetableImportResponse parseResponse(String response, List<ClassSubject> subjects) throws IOException {
        JsonNode root = objectMapper.readTree(response);
        String text = extractOutputText(root);
        if (text.isBlank()) throw new BadRequestException("The timetable could not be read");
        JsonNode json = objectMapper.readTree(cleanJson(text));
        List<TimetableImportResponse.Candidate> entries = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        json.path("warnings").forEach(node -> warnings.add(node.asText()));
        json.path("entries").forEach(node -> {
            try {
                Long id = node.path("classSubjectId").isNumber() ? node.get("classSubjectId").longValue() : null;
                ClassSubject cs = subjects.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
                if (cs == null) { warnings.add("An extracted slot was discarded because its class subject was not in your allowed catalog."); return; }
                String day = node.path("dayOfWeek").asText("").toUpperCase(Locale.ROOT);
                String start = node.path("startTime").asText("");
                String end = node.path("endTime").asText("");
                if (!isValidDay(day) || !isValidTime(start) || !isValidTime(end) || !LocalTime.parse(end).isAfter(LocalTime.parse(start))) {
                    warnings.add("A slot was discarded because its day or time was invalid."); return;
                }
                entries.add(new TimetableImportResponse.Candidate(id, cs.getSchoolClass().getName(), cs.getSubjectName(),
                        cs.getTeacher() == null ? null : (cs.getTeacher().getFirstName() + " " + cs.getTeacher().getLastName()).trim(),
                        day, start, end, node.path("room").asText(null), node.path("confidence").isNumber() ? node.get("confidence").doubleValue() : null));
            } catch (Exception ignored) { warnings.add("A timetable slot could not be normalized and was discarded."); }
        });
        return new TimetableImportResponse(entries, warnings);
    }

    private String extractOutputText(JsonNode root) {
        StringBuilder out = new StringBuilder();
        root.path("output").forEach(item -> item.path("content").forEach(content -> {
            if (content.has("text")) out.append(content.path("text").asText());
        }));
        return out.toString().trim();
    }

    private String cleanJson(String text) {
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        return cleaned.trim();
    }

    private boolean isValidDay(String day) { return List.of("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY").contains(day); }
    private boolean isValidTime(String value) { try { LocalTime.parse(value); return value.matches("\\d{2}:\\d{2}"); } catch (Exception e) { return false; } }
}
