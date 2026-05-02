package com.bd.spotify.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaylistRequest {

    @NotBlank(message = "Playlist name is required")
    @Size(max = 100, message = "Playlist name should be less than 100 characters")
    private String name;

    @Size(max = 500, message = "Playlist description should be less than 500 characters")
    private String description;

    private Boolean isPublic;
}
