package com.example.leitor.Model;

import android.content.Context;
import android.media.MediaPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Model {
    private MediaPlayer mediaPlayer;
    private List<Musica> listaMusica = new ArrayList<>();
    private int indiceActual = 0;
    private quandoMudaDeMusica musicaMudou;
    private boolean isShuffleOn = false;
    private boolean isRepeatOn = false;
    private Random random = new Random();
    private Context context;

    public interface quandoMudaDeMusica {
        void musicaMudou(Musica musica);
    }

    public Model(quandoMudaDeMusica mudaDeMusica, List<Musica> playList, Context context) {
        this.musicaMudou = mudaDeMusica;
        this.listaMusica = new ArrayList<>(playList);
        this.context = context;
    }

    public void setListaMusicas(List<Musica> musicas) {
        this.listaMusica.clear();
        this.listaMusica.addAll(musicas);
        if (!musicas.isEmpty()) {
            indiceActual = 0;
        }
    }

    public boolean seEstiverTocandoMusica() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void resumeOrPlay() {
        // Lógica movida para MusicService
    }

    private void playNew() {
        // Lógica movida para MusicService
    }

    public void playSongAt(int position) {
        // Lógica movida para MusicService
    }

    private void setupCompletionListener() {
        // Lógica movida para MusicService
    }

    public void pause() {
        // Lógica movida para MusicService
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void next() {
        // Lógica movida para MusicService
    }

    public void previous() {
        // Lógica movida para MusicService
    }

    public void seekTo(int position) {
        // Lógica movida para MusicService
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public void setShuffle(boolean on) {
        isShuffleOn = on;
    }

    public void setRepeat(boolean on) {
        isRepeatOn = on;
    }
}