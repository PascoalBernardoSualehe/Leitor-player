package com.example.leitor.view;

import com.example.leitor.Model.Musica;
// --- INÍCIO DA IMPORTAÇÃO NECESSÁRIA ---
import com.example.leitor.MusicService;
// --- FIM DA IMPORTAÇÃO NECESSÁRIA ---
import java.util.List;

public interface Contrato {

    interface View {
        void mostrarPlay();
        void mostrarPause();
        void mostrarStop();
        void mensagemErro(String mensagem);
        void informacaoMusica(String artista, String titulo);
        void updateProgress(int current, int total);
        void updateShuffleState(boolean isShuffleOn);

        // ================== INÍCIO DA CORREÇÃO ==================
        /**
         * Atualiza o estado do botão de repetição na UI.
         * @param state O estado atual de repetição (OFF, REPEAT_ALL, REPEAT_ONE).
         */
        void setRepeatButtonState(MusicService.RepeatState state);
        // =================== FIM DA CORREÇÃO ===================

        // ================== INÍCIO DA ADIÇÃO ==================
        /**
         * Chamado pelo Presenter quando a ordem da fila de reprodução muda.
         * A View deve atualizar a sua lista visual (RecyclerView).
         * @param newQueue A nova lista de músicas na ordem correta.
         */
        void onQueueChanged(List<Musica> newQueue);
        // =================== FIM DA ADIÇÃO ===================

        /**
         * Pede à View (MainActivity) para mostrar um diálogo de escolha
         * contendo a lista de nomes de playlists existentes.
         * @param musica A música que será adicionada.
         * @param nomesPlaylists A lista de nomes de playlists para mostrar.
         */
        void mostrarDialogoPlaylists(Musica musica, List<String> nomesPlaylists);
    }

    interface Presenter {
        // --- MÉTODOS DE CONTROLO DE MÚSICA (EXISTENTES) ---
        void togglePlayPause();
        void mostrarPrevious();
        void mostrarNext();
        void toggleShuffle();
        void toggleRepeat();
        void seekTo(int position);
        void mudancaMusica(Musica musica);
        void playSongAt(int position);
        void setListaMusicas(List<Musica> musicas);

        // ================== INÍCIO DA ADIÇÃO ==================
        /**
         * Chamado pela View quando o utilizador seleciona "Tocar a seguir".
         * @param musica A música a ser adicionada à fila.
         */
        void playSongNext(Musica musica);
        // =================== FIM DA ADIÇÃO ===================

        /**
         * Pede ao Presenter para criar uma nova playlist e adicionar a música especificada.
         * @param playlistName O nome da nova playlist.
         * @param musica A música a ser adicionada.
         */
        void createPlaylistAndAddSong(String playlistName, Musica musica);


        /**
         * Chamado pela View quando uma música é pressionada por um longo tempo.
         * O Presenter deve buscar as playlists e pedir à View para mostrar as opções.
         * @param musica A música que foi pressionada.
         */
        void onSongLongPressed(Musica musica);

        /**
         * Chamado pela View depois que o utilizador seleciona uma playlist no diálogo.
         * @param nomePlaylist O nome da playlist escolhida.
         * @param musica A música a ser adicionada.
         */
        void addSongToExistingPlaylist(String nomePlaylist, Musica musica);
    }

    // A interface Model não está a ser usada na nossa arquitetura atual
    // e pode ser removida para simplificar o código.
    interface Model {
        void play(android.content.Context context);
        void pause();
        void stop();
        void next();
        void previous();
        void seekTo(int position);
        boolean seEstiverTocandoMusica();
        int getCurrentPosition();
        int getDuration();
        void setShuffle(boolean on);
        void setRepeat(boolean on);
        void playSongAt(int position);
    }
}