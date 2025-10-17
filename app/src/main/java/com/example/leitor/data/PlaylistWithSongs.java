package com.example.leitor.data;
import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;
import java.util.List;

public class PlaylistWithSongs {
    @Embedded
    public Playlist playlist;

    @Relation(
            // O nome da coluna na entidade "pai" (Playlist) é "playlistId"
            parentColumn = "playlistId",
            // O nome da coluna na entidade "filha" (Song) é "songId"
            entityColumn = "songId",
            associateBy = @Junction(
                    value = PlaylistSongCrossRef.class,
                    parentColumn = "playlistId",
                    entityColumn = "songId"
            )
    )
    public List<Song> songs;
}
