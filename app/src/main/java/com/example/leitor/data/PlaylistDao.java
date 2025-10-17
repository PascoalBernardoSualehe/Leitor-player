package com.example.leitor.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

// --- INÍCIO DA CORREÇÃO ---
import com.example.leitor.Model.QueueState; // Adiciona a importação para QueueState
// --- FIM DA CORREÇÃO ---

import java.util.List;
import io.reactivex.rxjava3.core.Flowable;

@Dao
public interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long createPlaylist(Playlist playlist);

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    Flowable<List<Playlist>> getAllPlaylists();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addSongToPlaylistCrossRef(PlaylistSongCrossRef crossRef);

    @Transaction
    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    PlaylistWithSongs getPlaylistWithSongs(long playlistId);

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    Playlist findPlaylistByName(String name);

    @Delete
    void delete(Playlist playlist);

    @Query("SELECT name FROM playlists ORDER BY name ASC")
    List<String> getAllPlaylistNames();

    @Query("DELETE FROM playlist_songs WHERE songId = :songId AND playlistId = :playlistId")
    void removeSongFromPlaylist(long songId, long playlistId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertSong(Song song);

    @Query("SELECT * FROM songs WHERE uri = :uri LIMIT 1")
    Song findByUri(String uri);

    // --- SEÇÃO DE PERSISTÊNCIA (O seu código original está correto) ---
    /**
     * Guarda ou atualiza o estado atual da fila de reprodução na base de dados.
     * Usa OnConflictStrategy.REPLACE para sempre substituir o estado antigo pelo novo.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveQueueState(QueueState queueState);

    /**
     * Carrega o estado da fila de reprodução da base de dados.
     * Como usamos sempre o ID 1, esta query devolve o único estado guardado.
     */
    @Query("SELECT * FROM queue_state WHERE id = 1")
    QueueState loadQueueState();

    /**
     * Apaga o estado guardado da fila de reprodução. Útil quando a fila fica vazia.
     */
    @Query("DELETE FROM queue_state")
    void clearQueueState();
    // --- FIM DA SEÇÃO ---
}
