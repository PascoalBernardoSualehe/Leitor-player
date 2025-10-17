package com.example.leitor;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.leitor.Model.Musica;
// --- INÍCIO DA ADIÇÃO ---
import com.example.leitor.data.PlaylistRepository;
import com.example.leitor.Model.QueueState;
// --- FIM DA ADIÇÃO ---
import com.example.leitor.presenter.Presenter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MusicServiceChannel";

    public static final String ACTION_PLAY = "com.example.leitor.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.leitor.ACTION_PAUSE";
    public static final String ACTION_NEXT = "com.example.leitor.ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "com.example.leitor.ACTION_PREVIOUS";
    public static final String ACTION_PLAY_PLAYLIST = "com.example.leitor.ACTION_PLAY_PLAYLIST";
    public static final String ACTION_SONG_CHANGED = "com.example.leitor.ACTION_SONG_CHANGED";
    // ✅ NOVA ACTION PARA TOCAR MÚSICA ESPECÍFICA
    public static final String ACTION_PLAY_SONG = "com.example.leitor.ACTION_PLAY_SONG";

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private List<Musica> songs = new ArrayList<>();
    private List<Musica> originalSongs = new ArrayList<>();
    private int currentSongIndex = -1;

    private MediaSessionCompat mediaSession;
    private boolean isShuffleOn = false;
    private final Random random = new Random();
    private Presenter presenter;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    private AudioManager audioManager;

    // ✅ AUDIO FOCUS MELHORADO - VARIÁVEIS DE CONTROLE
    private boolean hasAudioFocus = false;
    private boolean playerWasPlaying = false;
    private AudioFocusRequest audioFocusRequest;

    // ✅ NOVAS VARIÁVEIS PARA CONTROLE DE ESTADO
    private boolean isPreparing = false;
    private boolean shouldPlayWhenPrepared = false;

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "AudioFocus mudou: " + focusChange + ", hasAudioFocus: " + hasAudioFocus);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    // ✅ GANHOU FOCO - RESTAURAR SE ESTAVA TOCANDO
                    hasAudioFocus = true;
                    if (mediaPlayer != null && !mediaPlayer.isPlaying() && playerWasPlaying) {
                        Log.d(TAG, "AudioFocus GAIN - retomando reprodução");
                        mediaPlayer.setVolume(1.0f, 1.0f);
                        startPlayback(); // ✅ USAR MÉTODO SEGURO
                    } else if (mediaPlayer != null) {
                        mediaPlayer.setVolume(1.0f, 1.0f);
                    }
                    playerWasPlaying = false;
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    // ✅ PERDA PERMANENTE DE FOCO - PARAR COMPLETAMENTE
                    hasAudioFocus = false;
                    Log.d(TAG, "AudioFocus LOSS - parando reprodução");
                    if (isPlaying()) {
                        playerWasPlaying = true;
                        pausePlayback(); // ✅ USAR MÉTODO SEGURO
                    }
                    abandonAudioFocus(); // Liberar foco permanentemente
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // ✅ PERDA TEMPORÁRIA - PAUSAR MAS MANTER ESTADO
                    hasAudioFocus = false;
                    Log.d(TAG, "AudioFocus LOSS_TRANSIENT - pausando temporariamente");
                    if (isPlaying()) {
                        playerWasPlaying = true;
                        pausePlayback();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // ✅ PODE CONTINUAR MAS COM VOLUME REDUZIDO
                    Log.d(TAG, "AudioFocus LOSS_TRANSIENT_CAN_DUCK - reduzindo volume");
                    if (mediaPlayer != null && isPlaying()) {
                        mediaPlayer.setVolume(0.3f, 0.3f);
                    }
                    break;
            }
        }
    };

    public enum RepeatState { OFF, REPEAT_ONE }
    private RepeatState currentRepeatState = RepeatState.OFF;

    // --- INÍCIO DA ADIÇÃO ---
    private PlaylistRepository playlistRepository;
    // --- FIM DA ADIÇÃO ---

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mediaSession = new MediaSessionCompat(this, TAG);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlay() { resume(); }
            @Override public void onPause() { pause(); }
            @Override public void onSkipToNext() { playNext(); }
            @Override public void onSkipToPrevious() { playPrevious(); }
            @Override public void onSeekTo(long pos) { seekTo((int) pos); }
        });
        mediaSession.setActive(true);

        startForeground(NOTIFICATION_ID, createNotification(null, null));

        // --- INÍCIO DA ADIÇÃO ---
        playlistRepository = new PlaylistRepository(getApplicationContext());
        loadLastQueueState(); // Tenta carregar o estado anterior ao iniciar o serviço.
        // --- FIM DA ADIÇÃO ---
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }
        String action = intent.getAction();
        Log.d(TAG, "onStartCommand: Ação recebida: " + action);

        switch (action) {
            case ACTION_PLAY_PLAYLIST:
                List<Musica> newSongList = (List<Musica>) intent.getSerializableExtra("SONG_LIST");
                int position = intent.getIntExtra("SONG_POSITION", 0);
                if (newSongList != null && !newSongList.isEmpty()) {
                    setSongs(newSongList);
                    playSongAt(position);
                }
                break;
            case ACTION_PLAY_SONG:
                // ✅ NOVO CASO: TOCAR MÚSICA ESPECÍFICA
                List<Musica> songList = (List<Musica>) intent.getSerializableExtra("SONG_LIST");
                int songPosition = intent.getIntExtra("SONG_POSITION", 0);
                if (songList != null && !songList.isEmpty()) {
                    setSongs(songList);
                    playSongAt(songPosition);
                }
                break;
            case ACTION_PLAY: resume(); break;
            case ACTION_PAUSE: pause(); break;
            case ACTION_NEXT: playNext(); break;
            case ACTION_PREVIOUS: playPrevious(); break;
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onDestroy() {
        // --- INÍCIO DA ADIÇÃO ---
        saveCurrentQueueState(); // Guarda o estado atual antes de o serviço ser destruído.
        // --- FIM DA ADIÇÃO ---
        super.onDestroy();
        cleanupMediaPlayer(); // ✅ USAR MÉTODO DE LIMPEZA SEGURA
        mediaSession.release();
        abandonAudioFocus();
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    // ✅ NOVO MÉTODO: LIMPEZA SEGURA DO MEDIA PLAYER
    private void cleanupMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Erro ao limpar media player", e);
            }
            mediaPlayer = null;
        }
        isPreparing = false;
        shouldPlayWhenPrepared = false;
    }

    // --- INÍCIO DA ADIÇÃO: Métodos de Persistência ---
    /**
     * Carrega o último estado da fila de reprodução a partir da base de dados.
     * Esta operação é assíncrona.
     */
    private void loadLastQueueState() {
        playlistRepository.loadQueueState(state -> {
            if (state != null && state.getSongQueue() != null && !state.getSongQueue().isEmpty()) {
                // Atualiza as variáveis do serviço com os dados carregados
                this.songs = state.getSongQueue();
                this.originalSongs = state.getOriginalSongQueue();
                this.currentSongIndex = state.getCurrentSongIndex();
                this.isShuffleOn = state.isShuffleOn();
                try {
                    this.currentRepeatState = RepeatState.valueOf(state.getRepeatState());
                } catch (Exception e) {
                    this.currentRepeatState = RepeatState.OFF; // Padrão seguro
                }

                Log.d(TAG, "Último estado da fila carregado com sucesso. " + this.songs.size() + " músicas.");

                // Notifica a MainActivity para atualizar a UI com os dados carregados.
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_SONG_CHANGED));
                // Atualiza a notificação com a música que estava a tocar.
                updateNotificationWithAlbumArt(getCurrentSong());
            } else {
                Log.d(TAG, "Nenhum estado de fila anterior encontrado na base de dados.");
            }
        });
    }

    /**
     * Guarda o estado atual da fila de reprodução na base de dados.
     */
    private void saveCurrentQueueState() {
        // Só guarda se houver uma fila válida.
        if (songs == null || songs.isEmpty()) {
            playlistRepository.clearQueueState(); // Limpa o estado antigo se a fila estiver vazia.
            return;
        }

        QueueState currentState = new QueueState();
        currentState.setSongQueue(new ArrayList<>(songs));
        currentState.setOriginalSongQueue(new ArrayList<>(originalSongs));
        currentState.setCurrentSongIndex(currentSongIndex);
        currentState.setShuffleOn(isShuffleOn);
        currentState.setRepeatState(currentRepeatState.name());

        playlistRepository.saveQueueState(currentState);
    }
    // --- FIM DA ADIÇÃO ---

    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    public void setSongs(List<Musica> songList) {
        this.originalSongs.clear();
        if (songList != null) this.originalSongs.addAll(songList);

        this.songs.clear();
        if (songList != null) this.songs.addAll(songList);

        if (presenter != null) {
            presenter.onQueueChanged(new ArrayList<>(this.songs));
        }
        Log.d(TAG, "Lista de músicas atualizada com " + (this.songs != null ? this.songs.size() : 0) + " itens.");
    }

    @SuppressLint("NotificationPermission")
    public void playSongAt(int position) {
        if (songs == null || songs.isEmpty() || position < 0 || position >= songs.size()) {
            Log.w(TAG, "playSongAt falhou: lista de músicas inválida ou posição fora dos limites.");
            return;
        }

        // ✅ VERIFICAR AUDIO FOCUS ANTES DE TOCAR
        if (!ensureAudioFocus()) {
            Log.w(TAG, "playSongAt: Não foi possível obter audio focus");
            Toast.makeText(this, "Não foi possível obter o foco de áudio", Toast.LENGTH_SHORT).show();
            return;
        }

        currentSongIndex = position;
        Musica musicaParaTocar = songs.get(currentSongIndex);

        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnCompletionListener(mp -> {
                    if (currentRepeatState == RepeatState.REPEAT_ONE) {
                        playSongAt(currentSongIndex);
                    } else {
                        playNext();
                    }
                });

                // ✅ ADICIONAR LISTENER PARA ERROS
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error - what: " + what + ", extra: " + extra);
                    isPreparing = false;
                    shouldPlayWhenPrepared = false;
                    return true; // Error handled
                });
            } else {
                mediaPlayer.reset();
            }

            mediaPlayer.setDataSource(this, Uri.parse(musicaParaTocar.getUriString()));
            mediaPlayer.prepare();
            startPlayback(); // ✅ USAR MÉTODO SEGURO PARA INICIAR

            Log.d(TAG, "🎵 Tocando música: " + musicaParaTocar.getTitulo() + " - " + musicaParaTocar.getArtista());

            // ✅ SALVA O ESTADO ATUAL DA FILA
            saveCurrentQueueState();
        } catch (IOException e) {
            Log.e(TAG, "Erro ao preparar a música", e);
            isPreparing = false;
            shouldPlayWhenPrepared = false;
            if (presenter != null) presenter.onUpdatePlay();
        }
    }

    // ✅ NOVO MÉTODO: INICIAR REPRODUÇÃO DE FORMA SEGURA
    private void startPlayback() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying() && hasAudioFocus) {
            try {
                mediaPlayer.start();
                Log.d(TAG, "Reprodução iniciada com sucesso");

                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_SONG_CHANGED));
                requestUpdate();
                updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING);
                updateNotificationWithAlbumArt(getCurrentSong());

            } catch (Exception e) {
                Log.e(TAG, "Erro ao iniciar reprodução", e);
            }
        }
    }

    // ✅ NOVO MÉTODO: PAUSAR REPRODUÇÃO DE FORMA SEGURA
    private void pausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.pause();
                Log.d(TAG, "Reprodução pausada com sucesso");

                requestUpdate();
                updateMediaSessionState(PlaybackStateCompat.STATE_PAUSED);
                stopForeground(false);
                updateNotificationWithAlbumArt(getCurrentSong());

            } catch (Exception e) {
                Log.e(TAG, "Erro ao pausar reprodução", e);
            }
        }
    }

    // ✅ NOVO MÉTODO: AUDIO FOCUS MELHORADO
    private boolean ensureAudioFocus() {
        if (audioManager == null) return false;

        // Se já tem audio focus, retornar true
        if (hasAudioFocus) {
            Log.d(TAG, "Já tem audio focus");
            return true;
        }

        int result;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setOnAudioFocusChangeListener(audioFocusChangeListener)
                        .setWillPauseWhenDucked(true)
                        .build();
                result = audioManager.requestAudioFocus(audioFocusRequest);
            } else {
                result = audioManager.requestAudioFocus(audioFocusChangeListener,
                        AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }

            hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
            Log.d(TAG, "Solicitação de audio focus: " + (hasAudioFocus ? "CONCEDIDO" : "NEGADO"));

            return hasAudioFocus;

        } catch (Exception e) {
            Log.e(TAG, "Erro ao solicitar audio focus", e);
            return false;
        }
    }

    // ✅ MÉTODO AUDIO FOCUS ATUALIZADO
    private boolean requestAudioFocus() {
        return ensureAudioFocus(); // ✅ USAR O NOVO MÉTODO MELHORADO
    }

    // ✅ NOVO MÉTODO: PARA TOCAR MÚSICA ESPECÍFICA DIRETAMENTE
    public void playSpecificSong(List<Musica> songList, int position) {
        if (songList == null || songList.isEmpty() || position < 0 || position >= songList.size()) {
            Log.w(TAG, "playSpecificSong falhou: lista de músicas inválida ou posição fora dos limites.");
            return;
        }

        setSongs(songList);
        playSongAt(position);
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            pausePlayback(); // ✅ USAR MÉTODO SEGURO
            // Não abandonamos o foco aqui, para que a música possa ser resumida.
            // O sistema operativo irá geri-lo através do onAudioFocusChangeListener.
        }
    }

    public void resume() {
        if (songs == null || songs.isEmpty()) return;

        // ✅ VERIFICAR AUDIO FOCUS ANTES DE RETOMAR
        if (!ensureAudioFocus()) {
            Log.w(TAG, "resume: Não foi possível obter audio focus");
            Toast.makeText(this, "Não foi possível obter o foco de áudio", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentSongIndex == -1) {
            playSongAt(0);
            return;
        }
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            startPlayback(); // ✅ USAR MÉTODO SEGURO
            // Re-inicia o serviço em foreground com a notificação
            startForeground(NOTIFICATION_ID, createNotification(getCurrentSong(), null));
        }
    }

    public void playNext() {
        if (songs == null || songs.isEmpty()) return;
        if (isShuffleOn) {
            currentSongIndex = random.nextInt(songs.size());
        } else {
            currentSongIndex = (currentSongIndex < songs.size() - 1) ? currentSongIndex + 1 : 0;
        }
        playSongAt(currentSongIndex);
    }

    public void playPrevious() {
        if (songs == null || songs.isEmpty()) return;
        if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 3000) {
            playSongAt(currentSongIndex);
        } else {
            if (isShuffleOn) {
                playNext(); // Em modo aleatório, "anterior" pode ser outra música aleatória
            } else {
                currentSongIndex = (currentSongIndex > 0) ? currentSongIndex - 1 : songs.size() - 1;
                playSongAt(currentSongIndex);
            }
        }
    }

    public void toggleShuffle() {
        isShuffleOn = !isShuffleOn;
        Musica currentSong = getCurrentSong();
        if (isShuffleOn) {
            if (currentSong != null) {
                songs.remove(currentSong);
                Collections.shuffle(songs);
                songs.add(0, currentSong);
                currentSongIndex = 0;
            }
        } else {
            songs.clear();
            songs.addAll(originalSongs);
            if (currentSong != null) {
                currentSongIndex = songs.indexOf(currentSong);
            }
        }
        if (presenter != null) {
            presenter.onUpdateShuffleState(isShuffleOn);
            presenter.onQueueChanged(new ArrayList<>(songs));
        }
        Toast.makeText(this, isShuffleOn ? "Shuffle ativado" : "Shuffle desativado", Toast.LENGTH_SHORT).show();
    }

    public void toggleRepeat() {
        currentRepeatState = (currentRepeatState == RepeatState.OFF) ? RepeatState.REPEAT_ONE : RepeatState.OFF;
        Toast.makeText(this, (currentRepeatState == RepeatState.REPEAT_ONE) ? "Repetir música atual" : "Repetição desligada", Toast.LENGTH_SHORT).show();
        if (presenter != null) presenter.onUpdateRepeatState(currentRepeatState);
    }

    public void addSongToQueue(Musica musica) {
        if (songs == null || musica == null) return;
        if (currentSongIndex < 0) {
            if (songs.contains(musica)) playSongAt(songs.indexOf(musica));
            return;
        }
        songs.remove(musica);
        int nextPosition = currentSongIndex + 1;
        if (nextPosition > songs.size()) {
            songs.add(musica);
        } else {
            songs.add(nextPosition, musica);
        }
        if (presenter != null) presenter.onQueueChanged(new ArrayList<>(songs));
    }

    public List<Musica> getCurrentSongs() {
        return new ArrayList<>(songs);
    }

    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    public Musica getCurrentSong() {
        if (currentSongIndex != -1 && songs != null && !songs.isEmpty() && currentSongIndex < songs.size()) {
            return songs.get(currentSongIndex);
        }
        return null;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
            updateMediaSessionState(mediaPlayer.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
        }
    }

    public void requestUpdate() {
        if (presenter != null) {
            Musica currentSong = getCurrentSong();
            presenter.onUpdateSongInfo(
                    (currentSong != null) ? currentSong.getArtista() : "Artista",
                    (currentSong != null) ? currentSong.getTitulo() : "Título da Música"
            );
            if (isPlaying()) presenter.onUpdatePause(); else presenter.onUpdatePlay();
            presenter.onUpdateShuffleState(isShuffleOn);
            presenter.onUpdateRepeatState(currentRepeatState);
        }
    }

    private void abandonAudioFocus() {
        if (audioManager == null || !hasAudioFocus) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest != null) {
                    audioManager.abandonAudioFocusRequest(audioFocusRequest);
                }
            } else {
                audioManager.abandonAudioFocus(audioFocusChangeListener);
            }
            hasAudioFocus = false;
            Log.d(TAG, "Audio focus abandonado");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao abandonar audio focus", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Music Player Channel", NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
        }
    }

    // --- MÉTODOS DE NOTIFICAÇÃO ATUALIZADOS PARA SUPORTAR CAPA DO ÁLBUM ---

    private void updateNotificationWithAlbumArt(Musica musica) {
        if (musica == null) {
            if (isPlaying()) stopForeground(true);
            return;
        }
        Glide.with(this)
                .asBitmap()
                .load(musica.getAlbumArtUriString())
                .error(R.drawable.reprodutor_de_musica)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        NotificationManagerCompat.from(MusicService.this).notify(NOTIFICATION_ID, createNotification(musica, resource));
                        updateMediaSessionMetadata(musica, resource);
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        Bitmap defaultIcon = BitmapFactory.decodeResource(getResources(), R.drawable.reprodutor_de_musica);
                        NotificationManagerCompat.from(MusicService.this).notify(NOTIFICATION_ID, createNotification(musica, defaultIcon));
                        updateMediaSessionMetadata(musica, defaultIcon);
                    }
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        Bitmap defaultIcon = BitmapFactory.decodeResource(getResources(), R.drawable.reprodutor_de_musica);
                        NotificationManagerCompat.from(MusicService.this).notify(NOTIFICATION_ID, createNotification(musica, defaultIcon));
                        updateMediaSessionMetadata(musica, defaultIcon);
                    }
                });
    }

    private Notification createNotification(Musica musica, @Nullable Bitmap albumArt) {
        boolean isPlaying = isPlaying();
        PendingIntent prevPI = PendingIntent.getService(this, 0, new Intent(this, MusicService.class).setAction(ACTION_PREVIOUS), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent playPausePI = PendingIntent.getService(this, 0, new Intent(this, MusicService.class).setAction(isPlaying ? ACTION_PAUSE : ACTION_PLAY), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent nextPI = PendingIntent.getService(this, 0, new Intent(this, MusicService.class).setAction(ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = (musica != null) ? musica.getTitulo() : "Leitor de Música";
        String artist = (musica != null) ? musica.getArtista() : "Selecione uma música";

        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.reprodutor_de_musica)
                .setContentIntent(contentPendingIntent)
                .setOnlyAlertOnce(true)
                .addAction(R.drawable.anterior, "Previous", prevPI)
                .addAction(isPlaying ? R.drawable.pause : R.drawable.play1, "Play/Pause", playPausePI)
                .addAction(R.drawable.proximo, "Next", nextPI)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2)
                )
                .setOngoing(isPlaying);

        if (albumArt != null) {
            builder.setLargeIcon(albumArt);
        } else {
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.reprodutor_de_musica));
        }
        return builder.build();
    }

    private void updateMediaSessionMetadata(Musica track, @Nullable Bitmap albumArt) {
        if (track == null) return;
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getTitulo())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getArtista())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (mediaPlayer != null) ? mediaPlayer.getDuration() : 0);
        if (albumArt != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt);
        }
        mediaSession.setMetadata(metadataBuilder.build());
    }

    private void updateMediaSessionState(int state) {
        long position = (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
        updateMediaSession(position, (mediaPlayer != null) ? mediaPlayer.getDuration() : 0, state);
    }

    private void updateMediaSession(long position, long duration, int state) {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SEEK_TO)
                .setState(state, position, 1.0f)
                .setActiveQueueItemId(currentSongIndex);
        mediaSession.setPlaybackState(stateBuilder.build());

        updateMediaSessionMetadata(getCurrentSong(), null);

        if (progressRunnable != null) progressHandler.removeCallbacks(progressRunnable);
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            progressRunnable = () -> {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    if (presenter != null) presenter.onUpdateProgress(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
                    progressHandler.postDelayed(progressRunnable, 1000);
                }
            };
            progressHandler.post(progressRunnable);
        }
    }
}