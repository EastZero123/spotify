package com.bd.spotify.serviceImpl;

import com.bd.spotify.service.GenericGeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.genai.Client;
import com.google.genai.errors.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GenericGeminiServiceImpl implements GenericGeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GenericGeminiServiceImpl.class);

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model}")
    private String geminiModels;

    private final ObjectMapper objectMapper;

    public GenericGeminiServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T generateContent(String prompt, Class<T> responseType) {
        if(prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        Client client = new Client.Builder().apiKey(geminiApiKey).build();
        String[] models = geminiModels.split(",");
        Exception lastException = null;

        for(int i = 0; i < models.length; i++) {
            try {
                logger.info("Calling Gemini API with model: {} ({}/{})", models[i], i+1, models.length);
                String response = client.models.generateContent(models[i].trim(), prompt, null).text();

                if(response == null || response.trim().isEmpty()) {
                    throw new RuntimeException("Empty response from Gemini API");
                }

                return parseResponse(response, responseType);
            } catch (ClientException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("429")) {
                    logger.warn("Rate limit exceeded for {}. Trying next model...", models[i].trim());
                    lastException = ex;
                    if(i < models.length - 1) {
                        continue;
                    }
                } else {
                    throw new RuntimeException("Failed to call Gemini API", ex);
                }
            }
        }
        throw new RuntimeException("All models failed to generate response", lastException);
    }

    private <T> T parseResponse(String response, Class<T> responseType) {
        if (responseType == String.class) {
            return responseType.cast(response);
        }

        try {
            String json = response.trim();
            if (json.startsWith("```json")) json = json.substring(7);
            else if (json.startsWith("```")) json = json.substring(3);
            if (json.endsWith("```")) json = json.substring(0, json.length() - 3);

            return objectMapper.readValue(json.trim(), responseType);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse response", ex);
        }
    }
}
