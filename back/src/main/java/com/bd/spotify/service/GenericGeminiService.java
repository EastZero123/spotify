package com.bd.spotify.service;

public interface GenericGeminiService {

    <T> T generateContent(String prompt, Class<T> responseType);
}
