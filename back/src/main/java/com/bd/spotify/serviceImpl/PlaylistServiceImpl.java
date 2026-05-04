package com.bd.spotify.serviceImpl;

import com.bd.spotify.dto.request.PlaylistRequest;
import com.bd.spotify.dto.response.MessageResponse;
import com.bd.spotify.dto.response.PaginatedResponse;
import com.bd.spotify.dto.response.PlaylistResponse;
import com.bd.spotify.dto.response.PlaylistWithSongsResponse;
import com.bd.spotify.entity.AppUser;
import com.bd.spotify.entity.Playlist;
import com.bd.spotify.entity.PlaylistSong;
import com.bd.spotify.entity.Song;
import com.bd.spotify.repository.AppUserRepository;
import com.bd.spotify.repository.PlaylistRepository;
import com.bd.spotify.repository.PlaylistSongRepository;
import com.bd.spotify.repository.SongRepository;
import com.bd.spotify.service.PlaylistService;
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
public class PlaylistServiceImpl implements PlaylistService {

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PlaylistSongRepository playlistSongRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private FileHandlerUtil fileHandlerUtil;

    @Value("${app.base.url}")
    private String baseUrl;

    @Override
    public PlaylistResponse createPlaylist(PlaylistRequest request, MultipartFile imageFile, String email) {
        AppUser appUser = getUserByEmail(email);

        Playlist playlist = new Playlist();
        playlist.setName(request.getName());
        playlist.setDescription(request.getDescription());
        playlist.setIsPublic(request.getIsPublic());
        playlist.setAppUser(appUser);

        if(imageFile != null && !imageFile.isEmpty()) {
            String uniqueId = UUID.randomUUID().toString();
            String imageExtension = fileHandlerUtil.getFileExtension(imageFile.getOriginalFilename());
            String imageFilename = uniqueId + imageExtension;
            fileHandlerUtil.saveImageFileWithName(imageFile, imageFilename);
            playlist.setImageUrl("/api/file/image/" + imageFilename);
        }

        Playlist savedPlaylist = playlistRepository.save(playlist);

        return PlaylistResponse.fromEntity(savedPlaylist, baseUrl);
    }

    @Override
    public PlaylistResponse updatePlaylistPrivacy(Long id, Boolean isPublic, String email) {
        Playlist playlist = validatePlaylistAccess(id, email);

        playlist.setIsPublic(isPublic);

        Playlist updatedPlaylist = playlistRepository.save(playlist);

        return PlaylistResponse.fromEntity(updatedPlaylist, baseUrl);
    }

    @Override
    public MessageResponse addSongToPlaylist(Long playlistId, Long songId, String email) {
        Playlist playlist = validatePlaylistAccess(playlistId, email);

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found"));

        if(playlistSongRepository.existsByPlaylistIdAndSongId(playlistId, songId)) {
            throw new RuntimeException("Song already exists in playlist");
        }

        List<PlaylistSong> existingSongs = playlistSongRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
        int nextPosition = existingSongs.isEmpty() ? 1 : existingSongs.get(existingSongs.size() - 1).getPosition() + 1;

        PlaylistSong playlistSong = new PlaylistSong();
        playlistSong.setPlaylist(playlist);
        playlistSong.setSong(song);
        playlistSong.setPosition(nextPosition);

        playlistSongRepository.save(playlistSong);

        return new MessageResponse("Song added to playlist successfully");
    }

    @Override
    public MessageResponse removeSongFromPlaylist(Long playlistId, Long songId, String email) {
        validatePlaylistAccess(playlistId, email);

        PlaylistSong playlistSong = playlistSongRepository.findByPlaylistIdAndSongId(playlistId, songId)
                .orElseThrow(() -> new RuntimeException("Song not found in playlist"));

        int removedPosition = playlistSong.getPosition();

        playlistSongRepository.delete(playlistSong);

        List<PlaylistSong> songAfterRemoved = playlistSongRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
        for(PlaylistSong song : songAfterRemoved) {
            if(song.getPosition() > removedPosition) {
                song.setPosition(song.getPosition() - 1);
                playlistSongRepository.save(song);
            }
        }

        return new MessageResponse("Song removed from playlist successfully");
    }

    @Override
    public MessageResponse reorderSongInPlaylist(Long playlistId, Long songId, Integer newPosition, String email) {
        validatePlaylistAccess(playlistId, email);

        PlaylistSong playlistSong = playlistSongRepository.findByPlaylistIdAndSongId(playlistId, songId)
                .orElseThrow(() -> new RuntimeException("Song not found in playlist"));

        List<PlaylistSong> allSongs = playlistSongRepository.findByPlaylistIdOrderByPositionAsc(playlistId);

        if(newPosition < 1 || newPosition > allSongs.size()) {
            throw new RuntimeException("Invalid position");
        }

        int currentPosition = playlistSong.getPosition();

        if(currentPosition == newPosition) {
            return new MessageResponse("Song already at the correct position");
        }

        if(newPosition > currentPosition) {
            for(PlaylistSong song : allSongs) {
                if(song.getPosition() > currentPosition && song.getPosition() <= newPosition) {
                    song.setPosition(song.getPosition() - 1);
                    playlistSongRepository.save(song);
                }
            }
        } else {
            for(PlaylistSong song : allSongs) {
                if(song.getPosition() >= newPosition && song.getPosition() < currentPosition) {
                    song.setPosition(song.getPosition() + 1);
                    playlistSongRepository.save(song);
                }
            }
        }

        playlistSong.setPosition(newPosition);
        playlistSongRepository.save(playlistSong);

        List<PlaylistSong> finalSongs = playlistSongRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
        int normalizedPosition = 1;
        for(PlaylistSong song : finalSongs) {
            if(song.getPosition() != normalizedPosition) {
                song.setPosition(normalizedPosition);
                playlistSongRepository.save(song);
            }
            normalizedPosition++;
        }

        return new MessageResponse("Song reordered successfully");
    }

    @Override
    public PaginatedResponse<PlaylistResponse> getAllPublicPlaylists(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Playlist> playlistPage;

        if(search != null && !search.trim().isEmpty()) {
            playlistPage = playlistRepository.findPublicPlaylistsWithSongsByNameOrDescription(search.trim(), pageable);
        } else {
            playlistPage = playlistRepository.findPublicPlaylistsWithSongs(pageable);
        }

        List<PlaylistResponse> playlistResponses = playlistPage.getContent().stream()
                .map(playlist -> PlaylistResponse.fromEntity(playlist, baseUrl))
                .toList();

        return new PaginatedResponse<>(
                playlistResponses,
                playlistPage.getNumber(),
                playlistPage.getSize(),
                playlistPage.getTotalElements(),
                playlistPage.getTotalPages(),
                playlistPage.isLast(),
                playlistPage.isFirst()
        );
    }

    @Override
    public PaginatedResponse<PlaylistResponse> getMyPlaylists(String email, int page, int size, String search) {

        AppUser appUser = getUserByEmail(email);
        Pageable pageable = PageRequest.of(page, size);
        Page<Playlist> playlistPage;

        if(search != null && !search.trim().isEmpty()) {
            playlistPage = playlistRepository.findByAppUserIdAndNameContainingIgnoreCaseOrAppUserIdAndDescriptionContainingIgnoreCase(
                    appUser.getId(), search.trim(), appUser.getId(), search.trim(), pageable);
        } else {
            playlistPage = playlistRepository.findByAppUserId(appUser.getId(), pageable);
        }

        List<PlaylistResponse> playlistResponses = playlistPage.getContent().stream()
                .map(playlist -> PlaylistResponse.fromEntity(playlist, baseUrl))
                .toList();

        return new PaginatedResponse<>(
                playlistResponses,
                playlistPage.getNumber(),
                playlistPage.getSize(),
                playlistPage.getTotalElements(),
                playlistPage.getTotalPages(),
                playlistPage.isLast(),
                playlistPage.isFirst()
        );
    }

    @Override
    public PlaylistWithSongsResponse getPlaylistWithSongs(Long playlistId, String email) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (!playlist.getIsPublic()) {
            if(email == null) {
                throw new RuntimeException("You must be logged in to access this playlist");
            }

            AppUser appUser = getUserByEmail(email);
            boolean isOwner = playlist.getAppUser().getId().equals(appUser.getId());
            boolean isAdmin = "ADMIN".equals(appUser.getRole());

            if(!isOwner && !isAdmin) {
                throw new RuntimeException("You do not have permission to access this playlist");
            }
        }

        List<PlaylistSong> playlistSongs = playlistSongRepository.findByPlaylistIdOrderByPositionAsc(playlistId);

        return PlaylistWithSongsResponse.fromEntity(playlist, playlistSongs, baseUrl);
    }

    @Override
    public MessageResponse deletePlaylist(Long playlistId, String email) {
        Playlist playlist = validatePlaylistAccess(playlistId, email);

        playlistSongRepository.deleteByPlaylistId(playlistId);

        playlistRepository.delete(playlist);

        return new MessageResponse("Playlist deleted successfully");
    }

    private AppUser getUserByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Playlist validatePlaylistAccess(Long id, String email) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        AppUser appUser = getUserByEmail(email);

        boolean isOwner = playlist.getAppUser().getId().equals(appUser.getId());
        boolean isAdmin = "ADMIN".equals(appUser.getRole());

        if(!isOwner && !isAdmin) {
            throw new RuntimeException("You do not have permission to access this playlist");
        }

        return playlist;
    }
}
