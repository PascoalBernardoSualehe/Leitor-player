package com.example.leitor.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// --- INÍCIO DA CORREÇÃO ---
// Adicionamos um índice único na coluna 'uri'. Isto impede que a mesma música
// (com o mesmo URI) seja inserida no banco de dados mais de uma vez, evitando duplicatas.
@Entity(tableName = "songs", indices = {@Index(value = "uri", unique = true)})
public class Song {

    // Renomeamos 'id' para 'songId' e usamos @ColumnInfo para consistência.
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "songId")
    public long songId;

    @NonNull
    @ColumnInfo(name = "title")
    public String title;

    @NonNull
    @ColumnInfo(name = "artist")
    public String artist;

    // Renomeamos 'path' para 'uri' para maior clareza e consistência.
    @NonNull
    @ColumnInfo(name = "uri")
    public String uri; // URI da música como String

    // Adicionamos a coluna que faltava para a capa do álbum.
    // Pode ser nula, por isso não usamos @NonNull.
    @ColumnInfo(name = "albumArtUri")
    public String albumArtUri;

    /**
     * Construtor atualizado para incluir todos os campos.
     */
    public Song(@NonNull String title, @NonNull String artist, @NonNull String uri, String albumArtUri) {
        this.title = title;
        this.artist = artist;
        this.uri = uri;
        this.albumArtUri = albumArtUri;
    }
}
// --- FIM DA CORREÇÃO ---
