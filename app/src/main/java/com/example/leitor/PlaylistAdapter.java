package com.example.leitor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // Importação adicionada
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.leitor.data.Playlist;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlists;
    // --- INÍCIO DA ALTERAÇÃO 1: Renomear e unificar o listener ---
    private final OnPlaylistInteractionListener interactionListener;

    /**
     * Interface unificada para que a Activity possa receber todos os eventos de interação:
     * - O clique normal no item.
     * - O clique no botão de opções (três pontinhos).
     */
    public interface OnPlaylistInteractionListener {
        void onPlaylistClick(Playlist playlist);
        // O segundo parâmetro 'View' é importante. Ele é a "âncora"
        // onde o menu de opções (PopupMenu) vai aparecer.
        void onPlaylistOptionsClick(Playlist playlist, View anchorView);
    }

    /**
     * Construtor atualizado para receber a nova interface unificada.
     */
    public PlaylistAdapter(List<Playlist> playlists, OnPlaylistInteractionListener listener) {
        this.playlists = playlists;
        this.interactionListener = listener;
    }
    // --- FIM DA ALTERAÇÃO 1 ---

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.bind(playlist, interactionListener);
    }

    @Override
    public int getItemCount() {
        return playlists != null ? playlists.size() : 0;
    }

    public void updatePlaylists(List<Playlist> newPlaylists) {
        this.playlists = newPlaylists;
        notifyDataSetChanged();
    }

    // --- INÍCIO DA ALTERAÇÃO 2: Atualizar o ViewHolder ---
    class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView playlistName;
        ImageButton optionsButton; // Referência para o novo botão de opções

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            playlistName = itemView.findViewById(R.id.tv_playlist_name);
            // Encontra o botão que adicionamos no XML pelo seu ID
            optionsButton = itemView.findViewById(R.id.btn_playlist_options);
        }

        /**
         * Método que liga os dados da playlist à view e configura os cliques.
         */
        public void bind(final Playlist playlist, final OnPlaylistInteractionListener listener) {
            playlistName.setText(playlist.getName());

            // 1. Configura o clique normal no item inteiro (para abrir a playlist)
            itemView.setOnClickListener(v -> listener.onPlaylistClick(playlist));

            // 2. Configura o clique APENAS no botão de opções
            optionsButton.setOnClickListener(v -> {
                // Chama o novo método da interface, passando a playlist e a view do botão
                listener.onPlaylistOptionsClick(playlist, v);
            });

            // 3. Removemos a lógica antiga do clique longo (OnLongClickListener)
        }
    }
    // --- FIM DA ALTERAÇÃO 2 ---
}
