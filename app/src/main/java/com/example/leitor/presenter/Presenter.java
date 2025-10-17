package com.example.leitor.presenter;

import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.leitor.Model.Musica;
import com.example.leitor.MusicService;
import com.example.leitor.data.Playlist;
import com.example.leitor.data.PlaylistRepository;
import com.example.leitor.view.Contrato;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Presenter implements Contrato.Presenter {
    private static final String TAG = "Leitor_Presenter";
    private final Contrato.View view;
    private MusicService musicService;
    private final PlaylistRepository playlistRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();


    public Presenter(Contrato.View view, Context context) {
        this.view = view;
        this.playlistRepository = new PlaylistRepository(context);
    }

    public void setMusicService(MusicService service) {
        this.musicService = service;
        if (musicService != null) {
            musicService.setPresenter(this);
            Log.d(TAG, "MusicService conectado e referência do Presenter definida no serviço.");
        } else {
            Log.w(TAG, "Atenção: setMusicService foi chamado com um serviço nulo.");
        }
    }

    public void requestUiUpdate() {
        if (musicService != null) {
            Log.d(TAG, "Presenter a pedir atualização da UI ao MusicService.");
            musicService.requestUpdate();
        } else {
            Log.w(TAG, "requestUiUpdate chamado, mas o MusicService é nulo.");
        }
    }

    @Override
    public void setListaMusicas(List<Musica> musicas) {
        if (musicService != null) {
            musicService.setSongs(musicas);
            Log.d(TAG, "SUCESSO: Lista com " + (musicas != null ? musicas.size() : 0) + " músicas foi enviada para o MusicService.");
        } else {
            Log.e(TAG, "FALHA: setListaMusicas foi chamado, mas o MusicService ainda é nulo.");
            if (view != null) {
                view.mensagemErro("Erro crítico: Serviço de música indisponível.");
            }
        }
    }

    @Override
    public void playSongAt(int position) {
        if (musicService != null) {
            Log.d(TAG, "Recebido pedido para tocar a música na posição: " + position);
            musicService.playSongAt(position);
        } else {
            Log.e(TAG, "playSongAt falhou: MusicService é nulo.");
            if (view != null) {
                view.mensagemErro("Não é possível tocar: o serviço de música não está a responder.");
            }
        }
    }

    // ================== INÍCIO DA ADIÇÃO ==================
    @Override
    public void playSongNext(Musica musica) {
        if (musicService != null) {
            musicService.addSongToQueue(musica);
            if (view != null) {
                view.mensagemErro("'" + musica.getTitulo() + "' tocará a seguir");
            }
        } else {
            if (view != null) {
                view.mensagemErro("Serviço de música não disponível.");
            }
        }
    }
    // =================== FIM DA ADIÇÃO ===================

    @Override
    public void togglePlayPause() {
        if (musicService != null) {
            if (musicService.isPlaying()) {
                musicService.pause();
            } else {
                musicService.resume();
            }
        }
    }

    @Override
    public void mostrarNext() {
        if (musicService != null) {
            musicService.playNext();
        }
    }

    @Override
    public void mostrarPrevious() {
        if (musicService != null) {
            musicService.playPrevious();
        }
    }

    @Override
    public void seekTo(int position) {
        if (musicService != null) {
            musicService.seekTo(position);
        }
    }

    @Override
    public void toggleShuffle() {
        if (musicService != null) {
            musicService.toggleShuffle();
        }
    }

    @Override
    public void toggleRepeat() {
        if (musicService != null) {
            musicService.toggleRepeat();
        }
    }

    @Override
    public void mudancaMusica(Musica musica) {
        if (musica != null && view != null) {
            view.informacaoMusica(musica.getArtista(), musica.getTitulo());
        }
    }

    @Override
    public void createPlaylistAndAddSong(String playlistName, Musica musica) {
        if (musica == null) {
            if (view != null) {
                view.mensagemErro("Erro: Nenhuma música selecionada para adicionar.");
            }
            return;
        }

        playlistRepository.findOrCreatePlaylistAndAddSong(
                playlistName,
                musica,
                new PlaylistRepository.PlaylistCallback() {
                    @Override
                    public void onPlaylistCreated(String name) {
                        if (view != null) view.mensagemErro("Playlist '" + name + "' criada e música adicionada.");
                    }

                    @Override
                    public void onSongAdded(String name) {
                        if (view != null) view.mensagemErro("Música adicionada à playlist '" + name + "'.");
                    }

                    @Override
                    public void onError(String message) {
                        if (view != null) view.mensagemErro(message);
                    }
                }
        );
    }


    @Override
    public void onSongLongPressed(Musica musica) {
        onAddToPlaylistRequested(musica);
    }

    @Override
    public void addSongToExistingPlaylist(String nomePlaylist, Musica musica) {
        Log.d(TAG, "Adicionando a música '" + musica.getTitulo() + "' à playlist '" + nomePlaylist + "'");
        playlistRepository.addSongToPlaylist(nomePlaylist, musica, new PlaylistRepository.PlaylistCallback() {
            @Override
            public void onSongAdded(String name) {
                if (view != null) view.mensagemErro("Música adicionada a '" + name + "'");
            }

            @Override
            public void onError(String message) {
                if (view != null) view.mensagemErro(message);
            }

            @Override
            public void onPlaylistCreated(String name) {}
        });
    }

    public void onUpdatePlay() {
        if (view != null) view.mostrarPlay();
    }

    public void onUpdatePause() {
        if (view != null) view.mostrarPause();
    }

    public void onUpdateShuffleState(boolean isShuffleOn) {
        if (view != null) view.updateShuffleState(isShuffleOn);
    }

    public void onUpdateRepeatState(MusicService.RepeatState repeatState) {
        if (view != null) {
            view.setRepeatButtonState(repeatState);
        }
    }

    // ================== INÍCIO DA ADIÇÃO ==================
    /**
     * Chamado pelo MusicService quando a fila de reprodução é alterada.
     * Notifica a View para atualizar a sua lista.
     * @param newQueue A nova lista de músicas na ordem correta.
     */
    public void onQueueChanged(List<Musica> newQueue) {
        if (view != null) {
            // Garante que a atualização da UI ocorra na thread principal.
            if (view instanceof AppCompatActivity) {
                ((AppCompatActivity) view).runOnUiThread(() -> view.onQueueChanged(newQueue));
            }
        }
    }
    // =================== FIM DA ADIÇÃO ===================

    public void onUpdateProgress(int current, int total) {
        if (view != null) view.updateProgress(current, total);
    }

    public void onUpdateSongInfo(String artist, String title) {
        if (view != null) view.informacaoMusica(artist, title);
    }

    public void createPlaylist(String playlistName) {
        if (playlistName == null || playlistName.trim().isEmpty()) {
            if (view != null) {
                view.mensagemErro("O nome da playlist não pode ser vazio.");
            }
            return;
        }
        playlistRepository.createPlaylist(playlistName.trim());
        if (view != null) {
            view.mensagemErro("Playlist '" + playlistName.trim() + "' criada.");
        }
    }

    public void onAddToPlaylistRequested(Musica musica) {
        if (musica == null) {
            if (view != null) {
                view.mensagemErro("Nenhuma música selecionada.");
            }
            return;
        }

        executor.execute(() -> {
            List<String> playlistNames = playlistRepository.getAllPlaylistNames();

            if (view instanceof AppCompatActivity) {
                ((AppCompatActivity) view).runOnUiThread(() -> {
                    if (view != null) {
                        view.mostrarDialogoPlaylists(musica, playlistNames);
                    }
                });
            }
        });
    }
}
