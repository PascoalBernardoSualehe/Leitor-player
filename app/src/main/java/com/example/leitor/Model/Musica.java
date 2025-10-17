package com.example.leitor.Model;

import java.io.Serializable;

// A classe já implementa Serializable, o que está correto para passá-la entre Activities.
public class Musica implements Serializable {

    // Os campos são Strings, que são serializáveis, o que está correto.
    private final String titulo;
    private final String artista;
    private final String uri; // Guarda a String do URI do conteúdo da música
    private final String albumArtUri; // Guarda a String do URI da arte do álbum

    // O construtor está correto, aceitando as Strings.
    public Musica(String titulo, String artista, String uri, String albumArtUri) {
        this.titulo = titulo;
        this.artista = artista;
        this.uri = uri;
        this.albumArtUri = albumArtUri;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getArtista() {
        return artista;
    }

    // O seu getter para a String do URI do conteúdo.
    public String getUriString() {
        return uri;
    }

    // O seu getter para a String do URI da arte do álbum.
    public String getAlbumArtUriString() {
        return albumArtUri;
    }

    /**
     * Adiciona o método getUri() que estava a faltar e a causar o erro de compilação
     * Ele simplesmente retorna a mesma String que getUriString().
     * @return A String do URI do conteúdo da música.
     */
    public String getUri() {
        return this.uri;
    }
}
