package com.bd.spotify.controller;

import com.bd.spotify.dto.response.SongAIInsightsResponse;
import com.bd.spotify.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/song")
public class SongController {

    @Autowired
    private SongService songService;

    @GetMapping("/getSongAiInsights/{songId}")
    public ResponseEntity<SongAIInsightsResponse> getSongAiInsights(@PathVariable Long songId) {
        SongAIInsightsResponse response = songService.getSongAiInsights(songId);
        return ResponseEntity.ok(response);
    }
}
