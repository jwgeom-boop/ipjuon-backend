package com.ipjuon.backend.b2c;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipjuon.backend.consultation.ConsultationRequest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 상담건의 b2c_messages JSON 배열에 새 메시지 append. */
public class MessagesHelper {
    private static final ObjectMapper M = new ObjectMapper();

    /** from = "RESIDENT" | "CONSULTANT" */
    @SuppressWarnings("unchecked")
    public static void append(ConsultationRequest c, String from, String byName, String text) {
        if (text == null || text.isBlank()) return;
        List<Map<String, Object>> messages;
        try {
            String existing = c.getB2c_messages();
            messages = (existing == null || existing.isBlank())
                    ? new ArrayList<>()
                    : M.readValue(existing, List.class);
        } catch (Exception e) {
            messages = new ArrayList<>();
        }
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("id", UUID.randomUUID().toString());
        msg.put("from", from);
        if (byName != null && !byName.isBlank()) msg.put("by", byName);
        msg.put("text", text.trim());
        msg.put("at", OffsetDateTime.now().toString());
        messages.add(msg);
        try {
            c.setB2c_messages(M.writeValueAsString(messages));
        } catch (Exception ignore) {}
    }
}
