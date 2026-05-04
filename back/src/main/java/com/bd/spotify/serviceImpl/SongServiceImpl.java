package com.bd.spotify.serviceImpl;

import com.bd.spotify.dto.request.SongRequest;
import com.bd.spotify.dto.response.MessageResponse;
import com.bd.spotify.dto.response.PaginatedResponse;
import com.bd.spotify.dto.response.SongAIInsightsResponse;
import com.bd.spotify.dto.response.SongResponse;
import com.bd.spotify.entity.AppUser;
import com.bd.spotify.entity.Song;
import com.bd.spotify.repository.AppUserRepository;
import com.bd.spotify.repository.PlaylistSongRepository;
import com.bd.spotify.repository.SongRepository;
import com.bd.spotify.service.GenericGeminiService;
import com.bd.spotify.service.SongService;
import com.bd.spotify.util.FileHandlerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SongServiceImpl implements SongService {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PlaylistSongRepository playlistSongRepository;

    @Autowired
    private FileHandlerUtil fileHandlerUtil;

    @Autowired
    private GenericGeminiService geminiService;

    @Value("${app.base.url}")
    private String baseUrl;

    @Override
    public SongResponse addSong(SongRequest request, MultipartFile songFile, MultipartFile imageFile, String email) {
        AppUser appUser = getUserByEmail(email);
        String uniqueId = UUID.randomUUID().toString();

        Song song = new Song();
        song.setAppUser(appUser);
        updateSongMetadata(song, request);

        String songUrl = processSongFile(songFile, uniqueId);
        song.setSongUrl(songUrl);

        String imageUrl = processImageFile(imageFile, uniqueId);
        song.setImageUrl(imageUrl);

        Song savedSong = songRepository.save(song);

        return SongResponse.fromEntity(savedSong, baseUrl);
    }

    @Override
    public Object getAllSongs(Long userId, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Song> songPage;

        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasUserId = userId != null;

        if(hasSearch && hasUserId) {
            songPage = songRepository.findByAppUserIdAndTitleContainingIgnoreCaseOrAppUserIdAndArtistContainingIgnoreCase(
                    userId, search.trim(), userId, search.trim(), pageable);
        } else if (hasSearch) {
            songPage = songRepository.findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(search.trim(), search.trim(), pageable);
        } else if (hasUserId) {
            songPage = songRepository.findByAppUserId(userId, pageable);
        } else {
            songPage = songRepository.findAll(pageable);
        }

        List<SongResponse> songResponses = songPage.getContent().stream()
                .map(song -> SongResponse.fromEntity(song, baseUrl))
                .toList();

        return new PaginatedResponse<>(
                songResponses,
                songPage.getNumber(),
                songPage.getSize(),
                songPage.getTotalElements(),
                songPage.getTotalPages(),
                songPage.isLast(),
                songPage.isFirst()
        );
    }

    @Override
    public SongResponse getSongById(Long id) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found"));

        return SongResponse.fromEntity(song, baseUrl);
    }

    @Override
    public SongResponse updateSong(Long id, SongRequest songRequest, MultipartFile songFile, MultipartFile imageFile, String email) {
        Song song = validateSongAccess(id, email);
        updateSongMetadata(song, songRequest);

        if(songFile != null && !songFile.isEmpty()) {
            deleteOldSongFile(song.getSongUrl());
            String uniqueId = UUID.randomUUID().toString();
            String songUrl = processSongFile(songFile, uniqueId);
            song.setSongUrl(songUrl);
        }

        if(imageFile != null && !imageFile.isEmpty()) {
            deleteOldImageFile(song.getImageUrl());
            String uniqueId = UUID.randomUUID().toString();
            String imageUrl = processImageFile(imageFile, uniqueId);
            song.setImageUrl(imageUrl);
        }

        Song updatedSong = songRepository.save(song);

        return SongResponse.fromEntity(updatedSong, baseUrl);
    }

    @Override
    public MessageResponse deleteSong(Long id, String email) {
        Song song = validateSongAccess(id, email);
        playlistSongRepository.deleteBySongId(id);
        deleteSongFiles(song);
        songRepository.delete(song);
        return new MessageResponse("Song deleted successfully");
    }

    @Override
    public SongAIInsightsResponse getSongAiInsights(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found"));

        String prompt = buildSongAnalysisPrompt(song);
        return geminiService.generateContent(prompt, SongAIInsightsResponse.class);
    }

    private String buildSongAnalysisPrompt(Song song) {
        return String.format("""
                Analyze the song '%s' by '%s' and provide detailed insights in JSON format.
                
                Return a JSON object with the following structure:
                {
                    "analysis": "A detailed 2-3 sentence analysis of the track's musical characteristics, production quality, and emotional impact",
                    "moods": ["List", "of", "4-6", "mood", "keywords"],
                    "genre": "Primary genre classification",
                    "tempo": 120,
                    "key": "Musical key (e.g., C Major, D Minor)",
                    "energy": 7,
                    "similarArtists": ["List", "of", "4-6", "similar", "artists"],
                    "recommendedFor": "A 1-2 sentence recommendation about when and where to listen to this song"
                }
                
                Important:
                - The 'tempo' should be an estimated BPM (beats per minute) between 60-200
                - The 'energy' should be a rating from 1-10
                - Base your analysis on the artist's typical style and the song title
                - Be creative but realistic
                - Return ONLY the JSON object, no additional text
                """,
                song.getTitle(),
                song.getArtist());
    }

    private void updateSongMetadata(Song song, SongRequest request) {
        song.setTitle(request.getTitle());
        song.setArtist(request.getArtist());
    }

    private AppUser getUserByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String processSongFile(MultipartFile songFile, String uniqueId) {
        String songExtension = fileHandlerUtil.getFileExtension(songFile.getOriginalFilename());
        String songFilename = uniqueId + songExtension;
        fileHandlerUtil.saveSongFileWithName(songFile, songFilename);
        return "/api/file/song/" + songFilename;
    }

    private String processImageFile(MultipartFile imageFile, String uniqueId) {
        if (imageFile == null && imageFile.isEmpty()) {
            return null;
        }

        String imageExtension = fileHandlerUtil.getFileExtension(imageFile.getOriginalFilename());
        String imageFilename = uniqueId + imageExtension;
        fileHandlerUtil.saveImageFileWithName(imageFile, imageFilename);
        return "/api/file/image/" + imageFilename;
    }

    private Song validateSongAccess(Long id, String email) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found"));

        AppUser appUser = getUserByEmail(email);

        boolean isOwner = song.getAppUser().getId().equals(appUser.getId());
        boolean isAdmin = "ADMIN".equals(appUser.getRole());

        if(!isOwner && !isAdmin) {
            throw new RuntimeException("You do not have permission to access this song");
        }

        return song;
    }

    private void deleteOldSongFile(String songUrl) {
        if (songUrl != null) {
            String olfSongFilename = fileHandlerUtil.extractFilename(songUrl);
            if (olfSongFilename != null) {
                fileHandlerUtil.deleteSongFile(olfSongFilename);
            }
        }
    }

    private void deleteOldImageFile(String imageUrl) {
        if (imageUrl != null) {
            String olfImageFilename = fileHandlerUtil.extractFilename(imageUrl);
            if (olfImageFilename != null) {
                fileHandlerUtil.deleteImageFile(olfImageFilename);
            }
        }
    }

    private void deleteSongFiles(Song song) {
        deleteOldSongFile(song.getSongUrl());
        deleteOldImageFile(song.getImageUrl());
    }
}
