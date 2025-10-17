package com.example.leitor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.leitor.Model.Musica;
import java.util.Objects;

// CORREÇÃO: Herda de ListAdapter para atualizações de alta performance
public class SongAdapter extends ListAdapter<Musica, SongAdapter.SongViewHolder> {

    private final OnSongClickListener listener;
    private final OnSongOptionsClickListener optionsClickListener;
    private OnRemoveFromPlaylistClickListener removeFromPlaylistClickListener;
    private boolean isPlaylistDetailView = false;
    private int currentPlayingPosition = -1; // -1 significa que nenhuma música está a tocar

    // CORREÇÃO: A interface de clique agora passa o objeto Musica, que é mais robusto
    public interface OnSongClickListener {
        void onSongClick(Musica musica);
    }

    public interface OnSongOptionsClickListener {
        void onSongOptionsClick(Musica musica, View anchorView);
    }

    public interface OnRemoveFromPlaylistClickListener {
        void onRemoveClick(Musica musica, int position);
    }

    public SongAdapter(OnSongClickListener listener, OnSongOptionsClickListener optionsClickListener) {
        // O construtor agora usa um DiffUtil.Callback para calcular as mudanças na lista
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.optionsClickListener = optionsClickListener;
    }

    public void setOnRemoveFromPlaylistClickListener(OnRemoveFromPlaylistClickListener listener) {
        this.isPlaylistDetailView = true;
        this.removeFromPlaylistClickListener = listener;
    }

    /**
     * Atualiza qual música deve ser destacada como "a tocar".
     * Notifica apenas os itens que mudaram (o antigo e o novo) para máxima performance.
     * @param position A nova posição da música que está a tocar.
     */
    public void setCurrentPlayingSong(int position) {
        int previousPosition = this.currentPlayingPosition;
        this.currentPlayingPosition = position;

        // Notifica o item que deixou de tocar para remover o destaque
        if (previousPosition != -1 && previousPosition < getItemCount()) {
            notifyItemChanged(previousPosition);
        }
        // Notifica o novo item que começou a tocar para adicionar o destaque
        if (position != -1 && position < getItemCount()) {
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Musica musica = getItem(position);
        // Verifica se a música na posição atual é a que está a tocar
        boolean isCurrentSong = (position == currentPlayingPosition);
        holder.bind(musica, listener, optionsClickListener, removeFromPlaylistClickListener, isPlaylistDetailView, isCurrentSong);
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textArtist;
        ImageView imageAlbumArt;
        ImageButton btnSongOptions;
        ImageButton btnRemoveFromPlaylist;

        SongViewHolder(View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.tv_song_title);
            textArtist = itemView.findViewById(R.id.tv_song_artist);
            imageAlbumArt = itemView.findViewById(R.id.iv_album_art);
            btnSongOptions = itemView.findViewById(R.id.btn_song_options);
            btnRemoveFromPlaylist = itemView.findViewById(R.id.btn_remove_from_playlist);
        }

        public void bind(final Musica musica, final OnSongClickListener clickListener,
                         final OnSongOptionsClickListener optionsListener,
                         final OnRemoveFromPlaylistClickListener removeListener,
                         final boolean isPlaylistView, final boolean isCurrentSong) {

            textTitle.setText(musica.getTitulo());
            textArtist.setText(musica.getArtista());

            Glide.with(itemView.getContext())
                    .load(musica.getAlbumArtUriString())
                    .placeholder(R.drawable.reprodutor_de_musica)
                    .error(R.drawable.reprodutor_de_musica)
                    .into(imageAlbumArt);

            // Lógica de destaque visual
            if (isCurrentSong) {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.current_song_background));
                textTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.current_song_text));
                textArtist.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.current_song_text));
            } else {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), android.R.color.transparent));
                textTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.song_title_normal));
                textArtist.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.song_artist_normal));
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    // Passa o objeto Musica em vez da posição, que é mais seguro
                    clickListener.onSongClick(musica);
                }
            });

            // Lógica para mostrar o botão correto (opções ou remover)
            if (isPlaylistView) {
                btnSongOptions.setVisibility(View.GONE);
                btnRemoveFromPlaylist.setVisibility(View.VISIBLE);

                btnRemoveFromPlaylist.setOnClickListener(v -> {
                    if (removeListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        removeListener.onRemoveClick(musica, getAdapterPosition());
                    }
                });
            } else {
                btnSongOptions.setVisibility(View.VISIBLE);
                btnRemoveFromPlaylist.setVisibility(View.GONE);

                btnSongOptions.setOnClickListener(v -> {
                    if (optionsListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        optionsListener.onSongOptionsClick(musica, btnSongOptions);
                    }
                });
            }
        }
    }

    // CORREÇÃO: Implementação do DiffUtil para o ListAdapter funcionar
    private static final DiffUtil.ItemCallback<Musica> DIFF_CALLBACK = new DiffUtil.ItemCallback<Musica>() {
        @Override
        public boolean areItemsTheSame(@NonNull Musica oldItem, @NonNull Musica newItem) {
            // O URI é um identificador único perfeito para cada música
            return oldItem.getUriString().equals(newItem.getUriString());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Musica oldItem, @NonNull Musica newItem) {
            // Compara os conteúdos para ver se o item precisa ser redesenhado
            return oldItem.getTitulo().equals(newItem.getTitulo()) &&
                    oldItem.getArtista().equals(newItem.getArtista()) &&
                    Objects.equals(oldItem.getAlbumArtUriString(), newItem.getAlbumArtUriString());
        }
    };
}
