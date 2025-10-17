package com.example.leitor.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// O índice 'unique = true' no nome impede a criação de duas playlists com o mesmo nome.
@Entity(tableName = "playlists", indices = {@Index(value = "name", unique = true)})
public class Playlist {

    // --- INÍCIO DA CORREÇÃO ---
    // Renomeamos 'id' para 'playlistId' para corresponder ao que a classe de relação espera.
    // Usamos @ColumnInfo para garantir que o nome da coluna no banco de dados seja 'playlistId'.
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "playlistId")
    public long playlistId;
    // --- FIM DA CORREÇÃO ---

    @NonNull
    @ColumnInfo(name = "name")
    public String name;

    public Playlist(@NonNull String name) {
        this.name = name;
    }

    /**
     * Retorna o nome da playlist.
     * @return O nome da playlist.
     */
    @NonNull
    public String getName() {
        return name;
    }
}
