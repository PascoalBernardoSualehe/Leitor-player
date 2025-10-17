package com.example.leitor.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.leitor.Model.Musica;
// --- INÍCIO DA CORREÇÃO ---
import com.example.leitor.Model.QueueState; // Adiciona a importação que falta para QueueState
// --- FIM DA CORREÇÃO ---
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import io.reactivex.rxjava3.core.Flowable;

public class PlaylistRepository {

    private final PlaylistDao playlistDao;
    private final ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String TAG = "PlaylistRepository";

    public PlaylistRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.playlistDao = db.playlistDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Interface para retornar o resultado das operações assíncronas para a UI thread.
     */
    public static interface PlaylistCallback {
        void onPlaylistCreated(String name);
        void onSongAdded(String playlistName);
        void onError(String message);
    }

    // --- INÍCIO DA ADIÇÃO: Interface de Callback para o Estado da Fila ---
    /**
     * Interface para retornar o estado da fila de reprodução de forma assíncrona.
     */
    public interface QueueStateCallback {
        void onStateLoaded(QueueState state);
    }
    // --- FIM DA ADIÇÃO ---


    public Flowable<List<Playlist>> getAllPlaylists() {
        return playlistDao.getAllPlaylists();
    }

    public List<String> getAllPlaylistNames() {
        return playlistDao.getAllPlaylistNames();
    }

    public PlaylistWithSongs getPlaylistWithSongs(long playlistId) {
        // Esta operação pode ser demorada, idealmente deveria ser assíncrona
        // mas mantendo a estrutura original por enquanto.
        return playlistDao.getPlaylistWithSongs(playlistId);
    }

    public void deletePlaylist(Playlist playlist) {
        executorService.execute(() -> {
            try {
                playlistDao.delete(playlist);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao apagar a playlist '" + playlist.getName() + "'", e);
            }
        });
    }

    public void addSongToPlaylist(String playlistName, Musica musica, PlaylistCallback callback) {
        executorService.execute(() -> {
            try {
                Playlist playlist = playlistDao.findPlaylistByName(playlistName);
                if (playlist == null) {
                    mainHandler.post(() -> callback.onError("Playlist '" + playlistName + "' não encontrada."));
                    return;
                }

                Song songToSave = new Song(
                        musica.getTitulo(),
                        musica.getArtista(),
                        musica.getUri(),
                        musica.getAlbumArtUriString()
                );
                // O Room ignora a inserção se a música já existir, mas devolve o ID correto.
                long songId = playlistDao.insertSong(songToSave);
                if (songId == -1) { // Se a inserção foi ignorada, o ID pode ser -1. Precisamos buscar o ID existente.
                    Song existingSong = playlistDao.findByUri(musica.getUri());
                    if (existingSong != null) {
                        songId = existingSong.songId;
                    }
                }

                playlistDao.addSongToPlaylistCrossRef(new PlaylistSongCrossRef(playlist.playlistId, songId));
                mainHandler.post(() -> callback.onSongAdded(playlistName));

            } catch (Exception e) {
                Log.e(TAG, "Erro ao adicionar música à playlist", e);
                mainHandler.post(() -> callback.onError("Erro ao adicionar música."));
            }
        });
    }

    public void findOrCreatePlaylistAndAddSong(String playlistName, Musica musica, PlaylistCallback callback) {
        // ... (seu método original aqui, sem alterações)
        executorService.execute(() -> {
            try {
                Playlist existingPlaylist = playlistDao.findPlaylistByName(playlistName);
                long playlistId;
                boolean wasPlaylistCreated = false;

                if (existingPlaylist == null) {
                    Playlist newPlaylist = new Playlist(playlistName);
                    playlistId = playlistDao.createPlaylist(newPlaylist);
                    wasPlaylistCreated = true;
                } else {
                    playlistId = existingPlaylist.playlistId;
                }

                Song songToSave = new Song(musica.getTitulo(), musica.getArtista(), musica.getUri(), musica.getAlbumArtUriString());
                long songId = playlistDao.insertSong(songToSave);
                if (songId == -1) {
                    Song existingSong = playlistDao.findByUri(musica.getUri());
                    if (existingSong != null) songId = existingSong.songId;
                }

                playlistDao.addSongToPlaylistCrossRef(new PlaylistSongCrossRef(playlistId, songId));

                if (wasPlaylistCreated) {
                    mainHandler.post(() -> callback.onPlaylistCreated(playlistName));
                } else {
                    mainHandler.post(() -> callback.onSongAdded(playlistName));
                }

            } catch (Exception e) {
                Log.e(TAG, "Erro em findOrCreatePlaylistAndAddSong", e);
                mainHandler.post(() -> callback.onError("Erro ao salvar na playlist."));
            }
        });
    }

    public void createPlaylist(String playlistName) {
        executorService.execute(() -> {
            if (playlistDao.findPlaylistByName(playlistName) == null) {
                playlistDao.createPlaylist(new Playlist(playlistName));
                Log.d(TAG, "Playlist '" + playlistName + "' criada com sucesso.");
            } else {
                Log.w(TAG, "Tentativa de criar playlist que já existe: " + playlistName);
            }
        });
    }

    public Song findSongByUri(String uri) {
        // Esta operação pode ser demorada, idealmente deveria ser assíncrona.
        return playlistDao.findByUri(uri);
    }

    public void removeSongFromPlaylist(long songId, long playlistId) {
        executorService.execute(() -> playlistDao.removeSongFromPlaylist(songId, playlistId));
    }

    public void addSongToPlaylist(String playlistName, Musica musica) {
        // ... (seu método original aqui, sem alterações)
        executorService.execute(() -> {
            try {
                Playlist playlist = playlistDao.findPlaylistByName(playlistName);
                if (playlist != null) {
                    Song songToSave = new Song(musica.getTitulo(), musica.getArtista(), musica.getUri(), musica.getAlbumArtUriString());
                    long songId = playlistDao.insertSong(songToSave);
                    if (songId == -1) {
                        Song existingSong = playlistDao.findByUri(musica.getUri());
                        if (existingSong != null) songId = existingSong.songId;
                    }
                    playlistDao.addSongToPlaylistCrossRef(new PlaylistSongCrossRef(playlist.playlistId, songId));
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao adicionar música à playlist (versão simplificada)", e);
            }
        });
    }

    // --- INÍCIO DA ADIÇÃO: Métodos para Gerir o Estado da Fila ---

    /**
     * Guarda o estado da fila de reprodução na base de dados.
     * Esta operação é executada numa thread de background.
     * @param state O objeto QueueState a ser guardado.
     */
    public void saveQueueState(QueueState state) {
        executorService.execute(() -> {
            playlistDao.saveQueueState(state);
            Log.d(TAG, "Estado da fila guardado com sucesso.");
        });
    }

    /**
     * Carrega o estado da fila de reprodução da base de dados de forma assíncrona.
     * @param callback O callback que será chamado na thread principal com o estado carregado.
     */
    public void loadQueueState(QueueStateCallback callback) {
        executorService.execute(() -> {
            final QueueState state = playlistDao.loadQueueState();
            mainHandler.post(() -> callback.onStateLoaded(state));
        });
    }

    /**
     * Limpa o estado da fila de reprodução da base de dados.
     * Útil quando o utilizador limpa a fila ou a aplicação é redefinida.
     */
    public void clearQueueState() {
        executorService.execute(playlistDao::clearQueueState);
        Log.d(TAG, "Estado da fila limpo da base de dados.");
    }
    // --- FIM DA ADIÇÃO ---
}
