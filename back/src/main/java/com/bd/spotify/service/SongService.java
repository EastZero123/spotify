package com.bd.spotify.service;

import com.bd.spotify.dto.request.SongRequest;
import com.bd.spotify.dto.response.MessageResponse;
import com.bd.spotify.dto.response.SongAIInsightsResponse;
import com.bd.spotify.dto.response.SongResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SongService {
    SongResponse addSong(SongRequest request, MultipartFile songFile, MultipartFile imageFile, String email);

    Object getAllSongs(Long userId, int page, int size, String search);

    SongResponse getSongById(Long id);

    SongResponse updateSong(Long id, SongRequest songRequest, MultipartFile songFile, MultipartFile imageFile, String email);

    MessageResponse deleteSong(Long id, String email);

    SongAIInsightsResponse getSongAiInsights(Long songId);
}
