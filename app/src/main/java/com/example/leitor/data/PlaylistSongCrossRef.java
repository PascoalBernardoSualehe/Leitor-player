package com.example.leitor.data;
import androidx.room.Entity;

@Entity(tableName = "playlist_songs", primaryKeys = {"playlistId", "songId"})
public class PlaylistSongCrossRef {
    public long playlistId;
    public long songId;

    public PlaylistSongCrossRef(long playlistId, long songId) {
        this.playlistId = playlistId;
        this.songId = songId;
    }
}
