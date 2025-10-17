package com.example.leitor;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.leitor.Model.Musica;
import com.example.leitor.data.PlaylistRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SongListActivity extends AppCompatActivity {
    private static final String TAG = "SongListActivity";
    private RecyclerView rvSongs;
    private SongAdapter songAdapter;
    private SearchView searchView;
    private boolean isSearchViewVisible = false;

    // --- VARIÁVEIS ATUALIZADAS ---
    private MusicService musicService;
    private boolean isBound = false;
    private List<Musica> listaMusicasCompleta = new ArrayList<>(); // Lista para pesquisa

    public static final String ACTION_PLAY_NEXT = "com.example.leitor.ACTION_PLAY_NEXT";
    public static final String EXTRA_SONG_TO_PLAY_NEXT = "EXTRA_SONG_TO_PLAY_NEXT";

    // --- SERVICE CONNECTION ATUALIZADO ---
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            Log.d(TAG, "Conectado ao MusicService.");

            if (musicService != null) {
                // ✅ VERIFICA SE RECEBEMOS TODAS AS MÚSICAS DA MAINACTIVITY
                Intent intent = getIntent();
                if (intent != null && intent.hasExtra("ALL_SONGS")) {
                    List<Musica> todasMusicas = (List<Musica>) intent.getSerializableExtra("ALL_SONGS");
                    if (todasMusicas != null && !todasMusicas.isEmpty()) {
                        Log.d(TAG, "Recebidas " + todasMusicas.size() + " músicas da MainActivity");
                        listaMusicasCompleta = new ArrayList<>(todasMusicas);
                        updateRecyclerView(listaMusicasCompleta);
                        return; // Sai aqui para não usar as músicas do serviço
                    }
                }

                // ✅ FALLBACK: Se não recebeu da MainActivity, usa as do serviço
                List<Musica> currentSongs = musicService.getCurrentSongs();
                if (currentSongs != null && !currentSongs.isEmpty()) {
                    Log.d(TAG, "Lista de " + currentSongs.size() + " músicas recebida do serviço.");
                    listaMusicasCompleta = new ArrayList<>(currentSongs);
                    updateRecyclerView(listaMusicasCompleta);
                } else {
                    Toast.makeText(SongListActivity.this, "Nenhuma música carregada no leitor.", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            musicService = null;
            Log.d(TAG, "Desconectado do MusicService.");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnSearch = findViewById(R.id.btn_search);
        searchView = findViewById(R.id.search_view);
        rvSongs = findViewById(R.id.rv_songs);

        setupSearchViewColors(searchView);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));

        // ✅ INICIALIZA O ADAPTER COM LISTA VAZIA - SERÁ PREENCHIDA PELO SERVICE CONNECTION
        setupRecyclerView();

        btnBack.setOnClickListener(v -> finish());
        btnSearch.setOnClickListener(v -> toggleSearchView());

        searchView.setOnCloseListener(() -> {
            toggleSearchView();
            return true;
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText);
                return true;
            }
        });

        // ✅ LOG PARA DEBUG - VER O QUE RECEBEMOS DA MAINACTIVITY
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("ALL_SONGS")) {
            List<Musica> todasMusicas = (List<Musica>) intent.getSerializableExtra("ALL_SONGS");
            Log.d(TAG, "onCreate - Recebido ALL_SONGS: " + (todasMusicas != null ? todasMusicas.size() : "null"));
        } else {
            Log.d(TAG, "onCreate - Nenhum ALL_SONGS recebido");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // ✅ TRATA NOVOS INTS (útil se a activity já estiver criada)
        if (intent != null && intent.hasExtra("ALL_SONGS")) {
            List<Musica> todasMusicas = (List<Musica>) intent.getSerializableExtra("ALL_SONGS");
            if (todasMusicas != null && !todasMusicas.isEmpty()) {
                Log.d(TAG, "onNewIntent - Recebidas " + todasMusicas.size() + " músicas");
                listaMusicasCompleta = new ArrayList<>(todasMusicas);
                updateRecyclerView(listaMusicasCompleta);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Liga-se ao serviço sempre que a activity fica visível.
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Desliga-se do serviço para evitar memory leaks.
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    // No método setupRecyclerView(), atualize o onSongClick:
    private void setupRecyclerView() {
        if (songAdapter == null) {
            songAdapter = new SongAdapter(
                    musicaClicada -> { // onSongClick
                        if (listaMusicasCompleta != null && !listaMusicasCompleta.isEmpty()) {
                            int indiceOriginal = listaMusicasCompleta.indexOf(musicaClicada);
                            if (indiceOriginal != -1) {
                                // ✅ ENVIA A MÚSICA SELECIONADA PARA A MAINACTIVITY REPRODUZIR
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("selected_position", indiceOriginal);

                                // ✅ ENVIA TAMBÉM A LISTA COMPLETA PARA GARANTIR
                                resultIntent.putExtra("ALL_SONGS", new ArrayList<>(listaMusicasCompleta));

                                setResult(RESULT_OK, resultIntent);

                                // ✅ MOSTRA FEEDBACK PARA O USUÁRIO
                                Toast.makeText(this, "A tocar: " + musicaClicada.getTitulo(), Toast.LENGTH_SHORT).show();

                                finish();
                            } else {
                                Toast.makeText(this, "Erro ao selecionar a música.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    this::showSongOptionsPopupMenu // onSongOptionsClick
            );
            rvSongs.setAdapter(songAdapter);
        }
    }
    private void updateRecyclerView(List<Musica> songList) {
        if (songAdapter != null) {
            // Usa submitList para uma atualização de alta performance.
            songAdapter.submitList(songList);

            // ✅ ATUALIZA O DESTAQUE DA MÚSICA ATUAL (SE ESTIVER TOCANDO)
            if (isBound && musicService != null) {
                Musica currentSong = musicService.getCurrentSong();
                if (currentSong != null && songList.contains(currentSong)) {
                    int currentPosition = songList.indexOf(currentSong);
                    songAdapter.setCurrentPlayingSong(currentPosition);
                    // Rola a lista para a música atual
                    if (currentPosition != -1) {
                        rvSongs.post(() -> rvSongs.scrollToPosition(currentPosition));
                    }
                } else {
                    songAdapter.setCurrentPlayingSong(-1); // Nenhuma música destacada
                }
            }
        }
    }

    private void filterSongs(String query) {
        if (songAdapter == null) return;

        List<Musica> filteredList = new ArrayList<>();
        if (query.isEmpty()) {
            filteredList.addAll(listaMusicasCompleta);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
            for (Musica musica : listaMusicasCompleta) {
                if (musica.getTitulo().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                        musica.getArtista().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredList.add(musica);
                }
            }
        }
        songAdapter.submitList(filteredList);
    }

    private void showSongOptionsPopupMenu(Musica musica, View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.song_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_play_next) {
                Intent resultIntent = new Intent();
                resultIntent.setAction(ACTION_PLAY_NEXT);
                resultIntent.putExtra(EXTRA_SONG_TO_PLAY_NEXT, musica);
                setResult(RESULT_OK, resultIntent);
                Toast.makeText(this, "'" + musica.getTitulo() + "' tocará a seguir", Toast.LENGTH_SHORT).show();
                finish();
                return true;
            }
            else if (itemId == R.id.action_add_to_playlist) {
                showChoosePlaylistDialog(musica);
                return true;

            } else if (itemId == R.id.action_details) {
                new AlertDialog.Builder(this)
                        .setTitle("Detalhes da Música")
                        .setMessage("Título: " + musica.getTitulo() +
                                "\nArtista: " + musica.getArtista() +
                                "\n\nCaminho: " + musica.getUriString())
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showChoosePlaylistDialog(final Musica musica) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            PlaylistRepository tempRepo = new PlaylistRepository(this);
            final List<String> playlistNames = tempRepo.getAllPlaylistNames();

            handler.post(() -> {
                if (playlistNames == null || playlistNames.isEmpty()) {
                    Toast.makeText(this, "Nenhuma playlist encontrada. Crie uma primeiro.", Toast.LENGTH_LONG).show();
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Adicionar '" + musica.getTitulo() + "' a:");
                builder.setItems(playlistNames.toArray(new CharSequence[0]), (dialog, which) -> {
                    String nomePlaylistEscolhida = playlistNames.get(which);
                    Executors.newSingleThreadExecutor().execute(() -> {
                        tempRepo.addSongToPlaylist(nomePlaylistEscolhida, musica, new PlaylistRepository.PlaylistCallback() {
                            @Override public void onPlaylistCreated(String name) {}
                            @Override public void onSongAdded(String playlistName) {
                                runOnUiThread(() -> Toast.makeText(SongListActivity.this, "'" + musica.getTitulo() + "' adicionado a " + playlistName, Toast.LENGTH_SHORT).show());
                            }
                            @Override public void onError(String message) {
                                runOnUiThread(() -> Toast.makeText(SongListActivity.this, message, Toast.LENGTH_SHORT).show());
                            }
                        });
                    });
                });
                builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
                builder.show();
            });
        });
    }

    private void toggleSearchView() {
        isSearchViewVisible = !isSearchViewVisible;
        if (isSearchViewVisible) {
            searchView.setVisibility(View.VISIBLE);
            searchView.setIconified(false);
            searchView.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchView.findViewById(androidx.appcompat.R.id.search_src_text), InputMethodManager.SHOW_IMPLICIT);
            }
        } else {
            searchView.setQuery("", false);
            searchView.clearFocus();
            searchView.setVisibility(View.GONE);
            // Restaura a lista completa quando a pesquisa é fechada
            if (songAdapter != null && listaMusicasCompleta != null) {
                songAdapter.submitList(listaMusicasCompleta);
            }
        }
    }

    private void setupSearchViewColors(SearchView searchView) {
        try {
            EditText searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (searchText != null) {
                searchText.setTextColor(Color.WHITE);
                searchText.setHintTextColor(Color.LTGRAY);
            }
            ImageView closeIcon = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
            if (closeIcon != null) {
                closeIcon.setColorFilter(Color.WHITE);
            }
            ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
            if (searchIcon != null) {
                searchIcon.setColorFilter(Color.WHITE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}