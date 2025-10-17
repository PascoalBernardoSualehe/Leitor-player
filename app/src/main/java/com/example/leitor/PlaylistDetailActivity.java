package com.example.leitor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.leitor.Model.Musica;
import com.example.leitor.data.PlaylistRepository;
import com.example.leitor.data.PlaylistWithSongs;
import com.example.leitor.data.Song;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// CORREÇÃO: A interface de clique foi atualizada
public class PlaylistDetailActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener, SongAdapter.OnRemoveFromPlaylistClickListener {

    private RecyclerView rvPlaylistSongs;
    private SongAdapter songAdapter;
    private TextView tvPlaylistDetailName;
    private PlaylistRepository playlistRepository;
    private long playlistId;
    private List<Musica> musicasDaPlaylist;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        ImageButton btnBack = findViewById(R.id.btn_back_from_detail);
        btnBack.setOnClickListener(v -> finish());

        playlistRepository = new PlaylistRepository(this);
        tvPlaylistDetailName = findViewById(R.id.tv_playlist_detail_name);
        rvPlaylistSongs = findViewById(R.id.rv_playlist_songs);
        rvPlaylistSongs.setLayoutManager(new LinearLayoutManager(this));

        musicasDaPlaylist = new ArrayList<>();

        Intent intent = getIntent();
        playlistId = intent.getLongExtra(PlaylistsActivity.EXTRA_PLAYLIST_ID, -1);
        String playlistName = intent.getStringExtra(PlaylistsActivity.EXTRA_PLAYLIST_NAME);

        tvPlaylistDetailName.setText(playlistName);

        if (playlistId == -1) {
            Toast.makeText(this, "Erro: ID da Playlist inválido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerView();
        loadSongsFromPlaylist();
    }

    private void setupRecyclerView() {
        // CORREÇÃO: Inicializa o Adapter usando o novo construtor do ListAdapter
        songAdapter = new SongAdapter(this, (musica, anchorView) -> {
            // A lógica do menu de opções não é necessária aqui, então pode ficar vazia.
        });
        songAdapter.setOnRemoveFromPlaylistClickListener(this);
        rvPlaylistSongs.setAdapter(songAdapter);
    }

    private void loadSongsFromPlaylist() {
        executor.execute(() -> {
            PlaylistWithSongs playlistWithSongs = playlistRepository.getPlaylistWithSongs(playlistId);
            handler.post(() -> {
                if (playlistWithSongs != null && playlistWithSongs.songs != null) {
                    musicasDaPlaylist.clear();
                    musicasDaPlaylist.addAll(convertSongsToMusicas(playlistWithSongs.songs));

                    // CORREÇÃO: Usa submitList para uma atualização de alta performance
                    songAdapter.submitList(new ArrayList<>(musicasDaPlaylist));

                    if (musicasDaPlaylist.isEmpty()) {
                        Toast.makeText(this, "Nenhuma música nesta playlist.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    // ============================ INÍCIO DA CORREÇÃO DO ERRO ============================
    /**
     * Este método agora recebe o objeto Musica diretamente, o que é mais seguro e moderno.
     * @param musica O objeto Musica que foi clicado.
     */
    @Override
    public void onSongClick(Musica musica) {
        // Encontra a posição da música clicada na lista atual da playlist.
        int position = musicasDaPlaylist.indexOf(musica);

        if (position == -1) {
            Toast.makeText(this, "Erro ao encontrar a música na playlist.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepara o Intent para o MusicService
        Intent serviceIntent = new Intent(PlaylistDetailActivity.this, MusicService.class);
        // Define uma ação clara para o serviço saber o que fazer
        serviceIntent.setAction(MusicService.ACTION_PLAY_PLAYLIST);
        serviceIntent.putExtra("SONG_LIST", (Serializable) musicasDaPlaylist);
        serviceIntent.putExtra("SONG_POSITION", position);
        startService(serviceIntent);

        // Abre a MainActivity para mostrar a música a tocar
        Intent mainActivityIntent = new Intent(PlaylistDetailActivity.this, MainActivity.class);
        // Usa flags para trazer a MainActivity para a frente sem criar uma nova instância se ela já existir
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(mainActivityIntent);

        // Fecha a tela de detalhes da playlist
        finish();
    }
    // ============================ FIM DA CORREÇÃO DO ERRO ============================

    @Override
    public void onRemoveClick(Musica musica, int position) {
        executor.execute(() -> {
            // Procura o ID da música na base de dados pelo seu URI único
            Song songToRemove = playlistRepository.findSongByUri(musica.getUri());
            if (songToRemove != null) {
                playlistRepository.removeSongFromPlaylist(songToRemove.songId, playlistId);
            }
        });

        // Atualiza a UI de forma otimizada
        musicasDaPlaylist.remove(position);
        songAdapter.submitList(new ArrayList<>(musicasDaPlaylist));
        Toast.makeText(this, "Música removida da playlist", Toast.LENGTH_SHORT).show();
    }

    private List<Musica> convertSongsToMusicas(List<Song> songs) {
        List<Musica> musicas = new ArrayList<>();
        for (Song song : songs) {
            musicas.add(new Musica(
                    song.title,
                    song.artist,
                    song.uri,
                    song.albumArtUri
            ));
        }
        return musicas;
    }
}
