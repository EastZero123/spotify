package com.bd.spotify.repository;

import com.bd.spotify.entity.PlaylistSong;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {

    @Modifying
    @Transactional
    void deleteBySongId(Long songId);
}
