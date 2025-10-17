package com.example.leitor.Model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;
import java.util.List;

@Entity(tableName = "queue_state")
public class QueueState implements Serializable {
    @PrimaryKey
    public int id = 1; // Usaremos sempre o ID 1 para ter apenas um estado guardado

    private List<Musica> songQueue;
    private List<Musica> originalSongQueue;
    private int currentSongIndex;
    private boolean isShuffleOn;
    private String repeatState; // Guardamos como String ("OFF", "REPEAT_ONE")

    // Getters e Setters
    public List<Musica> getSongQueue() { return songQueue; }
    public void setSongQueue(List<Musica> songQueue) { this.songQueue = songQueue; }
    public List<Musica> getOriginalSongQueue() { return originalSongQueue; }
    public void setOriginalSongQueue(List<Musica> o) { this.originalSongQueue = o; }
    public int getCurrentSongIndex() { return currentSongIndex; }
    public void setCurrentSongIndex(int i) { this.currentSongIndex = i; }
    public boolean isShuffleOn() { return isShuffleOn; }
    public void setShuffleOn(boolean shuffleOn) { isShuffleOn = shuffleOn; }
    public String getRepeatState() { return repeatState; }
    public void setRepeatState(String repeatState) { this.repeatState = repeatState; }
}

