package com.example.leitor;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.leitor.Model.Musica;
import com.example.leitor.presenter.Presenter;
import com.example.leitor.view.Contrato;
import android.database.Cursor;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements Contrato.View {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";
    private ImageButton btn_shuffle, btn_previous, btn_play, btn_next, btn_repeat, btn_back, btn_menu;
    private TextView textartista, texttitulo, tv_current_time, tv_total_time;
    private SeekBar seek_bar;
    private RecyclerView rv_songs;
    private Presenter presenter;
    private SongAdapter songAdapter;
    private ActivityResultLauncher<Intent> songListLauncher;
    private MusicService musicService;
    private boolean isBound = false;
    private List<Musica> loadedMusicas = new ArrayList<>();

    // ✅ LISTA COMPLETA DE TODAS AS MÚSICAS DO DISPOSITIVO
    private List<Musica> allMusicasFromDevice = new ArrayList<>();

    private CircleImageView ivMusicIcon;
    private ObjectAnimator rotationAnimator;

    private LinearLayout app_bar;
    private ImageButton btn_search;
    private SearchView search_view;

    // === RECONHECIMENTO DE MÚSICAS ACRCLOUD ===
    private ImageButton btn_voice;
    private AudioRecognizer audioRecognizer;
    private boolean isRecognizing = false;
    private final Handler recognitionHandler = new Handler(Looper.getMainLooper());
    // ===========================================

    private final BroadcastReceiver songChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MusicService.ACTION_SONG_CHANGED.equals(intent.getAction())) {
                if (presenter != null) {
                    presenter.requestUiUpdate();
                }
            }
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            presenter.setMusicService(musicService);

            if (musicService != null) {
                List<Musica> serviceSongs = musicService.getCurrentSongs();
                if (serviceSongs != null && !serviceSongs.isEmpty()) {
                    onQueueChanged(serviceSongs);
                } else if (loadedMusicas.isEmpty()) {
                    handlePermissionsAndLoadMusic();
                }
                presenter.requestUiUpdate();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            presenter.setMusicService(null);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        presenter = new Presenter(this, this);

        initializeACRCloud();

        songListLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();

                // ✅ TRATA A LISTA COMPLETA QUE VOLTA DA SONGLISTACTIVITY
                if (data.hasExtra("ALL_SONGS")) {
                    List<Musica> todasMusicas = (List<Musica>) data.getSerializableExtra("ALL_SONGS");
                    if (todasMusicas != null && !todasMusicas.isEmpty()) {
                        allMusicasFromDevice = new ArrayList<>(todasMusicas);

                        // ✅ ATUALIZA O MUSIC SERVICE COM TODAS AS MÚSICAS
                        if (isBound && musicService != null) {
                            musicService.setSongs(allMusicasFromDevice);
                        }
                    }
                }

                if (SongListActivity.ACTION_PLAY_NEXT.equals(data.getAction())) {
                    Musica songToPlayNext = (Musica) data.getSerializableExtra(SongListActivity.EXTRA_SONG_TO_PLAY_NEXT);
                    if (songToPlayNext != null && presenter != null) {
                        presenter.playSongNext(songToPlayNext);
                    }
                } else {
                    int position = data.getIntExtra("selected_position", -1);
                    if (position != -1) {
                        if (isBound && presenter != null) {
                            // ✅ CORREÇÃO: ATUALIZA A LISTA NO MUSIC SERVICE PRIMEIRO
                            if (allMusicasFromDevice != null && !allMusicasFromDevice.isEmpty()) {
                                musicService.setSongs(allMusicasFromDevice);
                            }

                            // ✅ AGORA REPRODUZ A MÚSICA SELECIONADA
                            presenter.playSongAt(position);
                            updateCurrentSongHighlight(true);

                            // ✅ FORÇA ATUALIZAÇÃO DA UI
                            presenter.requestUiUpdate();
                        }
                    }
                }
            }
        });

        btn_back.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SongListActivity.class);

            // ✅ ENVIA TODAS AS MÚSICAS DO DISPOSITIVO PARA A SONGLISTACTIVITY
            intent.putExtra("ALL_SONGS", new ArrayList<>(allMusicasFromDevice));

            songListLauncher.launch(intent);
        });

        setupButtonClickListeners();
        setupSeekBar();
        setupSearch();

        processIntent(getIntent());

        Intent serviceIntent = new Intent(this, MusicService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(songChangedReceiver, new IntentFilter(MusicService.ACTION_SONG_CHANGED));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent intent) {
        if (intent != null && intent.hasExtra("PLAYLIST_TO_SHOW")) {
            List<Musica> playlistToShow = (List<Musica>) intent.getSerializableExtra("PLAYLIST_TO_SHOW");
            if (playlistToShow != null) {
                Intent serviceIntent = new Intent(this, MusicService.class);
                serviceIntent.setAction(MusicService.ACTION_PLAY_PLAYLIST);
                serviceIntent.putExtra("SONG_LIST", (Serializable) playlistToShow);
                serviceIntent.putExtra("SONG_POSITION", 0);
                startService(serviceIntent);

                // ✅ ATUALIZA A UI COM A PLAYLIST, MAS MANTÉM allMusicasFromDevice INTACTO
                onQueueChanged(playlistToShow);
            }
        }
    }

    private void updateCurrentSongHighlight(boolean scrollToPosition) {
        if (musicService != null && songAdapter != null) {
            int currentPosition = musicService.getCurrentSongIndex();
            songAdapter.setCurrentPlayingSong(currentPosition);

            if (scrollToPosition && currentPosition != -1) {
                rv_songs.post(() -> rv_songs.smoothScrollToPosition(currentPosition));
            }
        }
    }

    private void setupRecyclerView() {
        if (songAdapter == null) {
            songAdapter = new SongAdapter(
                    musica -> {
                        if (isBound && presenter != null) {
                            int originalPosition = loadedMusicas.indexOf(musica);
                            if (originalPosition != -1) {
                                presenter.playSongAt(originalPosition);
                            }
                        }
                        toggleSearch(false);
                    },
                    this::showSongOptionsPopupMenu
            );
            rv_songs.setLayoutManager(new LinearLayoutManager(this));
            rv_songs.setAdapter(songAdapter);
        }
    }

    @Override
    public void onQueueChanged(List<Musica> newQueue) {
        if (newQueue == null) return;

        // ✅ ATUALIZA APENAS A LISTA DE REPRODUÇÃO ATUAL, NÃO A LISTA COMPLETA
        loadedMusicas.clear();
        loadedMusicas.addAll(newQueue);

        if (songAdapter == null) {
            setupRecyclerView();
        }
        songAdapter.submitList(new ArrayList<>(newQueue));
        updateCurrentSongHighlight(false);
    }

    @Override
    public void informacaoMusica(String artista, String titulo) {
        texttitulo.setText(titulo);
        textartista.setText(artista);

        Musica currentSong = (musicService != null) ? musicService.getCurrentSong() : null;

        if (currentSong != null) {
            Glide.with(this)
                    .load(currentSong.getAlbumArtUriString())
                    .placeholder(R.drawable.reprodutor_de_musica)
                    .error(R.drawable.reprodutor_de_musica)
                    .into(ivMusicIcon);
        } else {
            ivMusicIcon.setImageResource(R.drawable.reprodutor_de_musica);
        }

        if (rotationAnimator != null) {
            rotationAnimator.cancel();
            ivMusicIcon.setRotation(0);
            if (musicService != null && musicService.isPlaying()) {
                rotationAnimator.start();
            }
        }
        updateCurrentSongHighlight(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(songChangedReceiver);

        stopMusicRecognition();
        if (audioRecognizer != null) {
            audioRecognizer.release();
            audioRecognizer = null;
        }

        if (recognitionHandler != null) {
            recognitionHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isBound && presenter != null) {
            presenter.requestUiUpdate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopMusicRecognition();
    }

    private void initializeACRCloud() {
        audioRecognizer = new AudioRecognizer(this, new AudioRecognizer.RecognitionCallback() {
            @Override
            public void onResult(String title, String artist) {
                isRecognizing = false;
                if (btn_voice != null) btn_voice.setEnabled(true);

                // ✅ MÚSICA NÃO PARA - APENAS MOSTRA O RESULTADO
                showRecognitionResult(title, artist);
                Log.d(TAG, "Música identificada: " + title + " - " + artist);
            }

            @Override
            public void onError(String message) {
                isRecognizing = false;
                if (btn_voice != null) btn_voice.setEnabled(true);

                // ✅ MÚSICA NÃO PARA - APENAS MOSTRA ERRO
                Toast.makeText(MainActivity.this, "Erro: " + message, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Erro no reconhecimento ACRCloud: " + message);
            }
        });
    }

    private void startMusicRecognition() {
        if (isRecognizing) {
            Toast.makeText(this, "Já está a identificar música...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
            return;
        }

        // ✅ MÚSICA CONTINUA TOCANDO - NÃO PAUSA
        isRecognizing = true;
        if (btn_voice != null) btn_voice.setEnabled(false);

        Toast.makeText(this, "🎵 A identificar música... Toque a música perto do microfone!", Toast.LENGTH_LONG).show();

        recognitionHandler.postDelayed(() -> {
            try {
                if (audioRecognizer != null) {
                    audioRecognizer.startRecognition();
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao iniciar reconhecimento de música: ", e);
                isRecognizing = false;
                if (btn_voice != null) btn_voice.setEnabled(true);
                Toast.makeText(MainActivity.this, "Erro ao iniciar identificação", Toast.LENGTH_SHORT).show();
            }
        }, 1000);
    }

    private void stopMusicRecognition() {
        if (audioRecognizer != null && isRecognizing) {
            try {
                audioRecognizer.stopRecognition();
                Log.d(TAG, "Reconhecimento de música parado");
            } catch (Exception e) {
                Log.e(TAG, "Erro ao parar reconhecimento de música", e);
            }
        }
        isRecognizing = false;
        if (btn_voice != null) btn_voice.setEnabled(true);
    }

    private void showRecognitionResult(String title, String artist) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("🎵 Música Identificada");

            String message = "🎤 Cantor: " + artist + "\n\n📀 Título: " + title;

            TextView messageView = new TextView(this);
            messageView.setText(message);
            messageView.setTextSize(14);
            messageView.setTextColor(Color.parseColor("#555555"));
            messageView.setGravity(Gravity.CENTER);
            messageView.setPadding(30, 20, 30, 20);
            messageView.setBackgroundColor(Color.parseColor("#FAFAFA"));
            messageView.setLineSpacing(0, 1.2f);

            builder.setView(messageView);
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.parseColor("#F0F0F0")));
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(window.getAttributes());
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.gravity = Gravity.CENTER;
                window.setAttributes(layoutParams);
            }

            try {
                TextView titleView = dialog.findViewById(android.R.id.title);
                if (titleView != null) {
                    titleView.setTextSize(16);
                    titleView.setTextColor(Color.parseColor("#2E7D32"));
                    titleView.setGravity(Gravity.CENTER);
                }
            } catch (Exception e) {
                // Ignorar
            }

            dialog.setOnShowListener(dialogInterface -> {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positiveButton != null) {
                    positiveButton.setTextSize(12);
                    positiveButton.setTextColor(Color.WHITE);
                    positiveButton.setBackgroundColor(Color.parseColor("#4CAF50"));
                    positiveButton.setPadding(20, 8, 20, 8);
                }
            });

            dialog.show();
        });
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (search_view.getVisibility() == View.VISIBLE) {
            toggleSearch(false);
        } else {
            super.onBackPressed();
        }
    }

    private void initViews() {
        textartista = findViewById(R.id.text_artista);
        texttitulo = findViewById(R.id.texttitulo);
        tv_current_time = findViewById(R.id.tv_current_time);
        tv_total_time = findViewById(R.id.tv_total_time);
        seek_bar = findViewById(R.id.seek_bar);
        rv_songs = findViewById(R.id.rv_songs);
        btn_shuffle = findViewById(R.id.btn_shuffle);
        btn_previous = findViewById(R.id.btn_previous);
        btn_play = findViewById(R.id.btn_play);
        btn_next = findViewById(R.id.btn_next);
        btn_repeat = findViewById(R.id.btn_repeat);
        btn_back = findViewById(R.id.btn_back);
        btn_menu = findViewById(R.id.btn_menu);
        ivMusicIcon = findViewById(R.id.iv_music_icon);
        app_bar = findViewById(R.id.app_bar);
        btn_search = findViewById(R.id.btn_search);
        search_view = findViewById(R.id.search_view);
        btn_voice = findViewById(R.id.btn_recognize);

        rotationAnimator = ObjectAnimator.ofFloat(ivMusicIcon, "rotation", 0f, 360f);
        rotationAnimator.setDuration(20000);
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setInterpolator(new LinearInterpolator());
    }

    private void setupButtonClickListeners() {
        btn_shuffle.setOnClickListener(v -> presenter.toggleShuffle());
        btn_previous.setOnClickListener(v -> presenter.mostrarPrevious());
        btn_play.setOnClickListener(v -> presenter.togglePlayPause());
        btn_next.setOnClickListener(v -> presenter.mostrarNext());
        btn_repeat.setOnClickListener(v -> presenter.toggleRepeat());
        btn_menu.setOnClickListener(this::showMainMenu);
        btn_search.setOnClickListener(v -> toggleSearch(true));

        if (btn_voice != null) {
            btn_voice.setOnClickListener(v -> startMusicRecognition());
        }
    }

    private void setupSearch() {
        EditText searchText = search_view.findViewById(androidx.appcompat.R.id.search_src_text);
        searchText.setTextColor(Color.WHITE);
        searchText.setHintTextColor(Color.LTGRAY);
        ImageView closeButton = search_view.findViewById(androidx.appcompat.R.id.search_close_btn);
        closeButton.setColorFilter(Color.WHITE);
        closeButton.setOnClickListener(v -> {
            if (search_view.getQuery().length() == 0) {
                toggleSearch(false);
            } else {
                search_view.setQuery("", false);
            }
        });
        search_view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText);
                return true;
            }
        });
    }

    private void toggleSearch(boolean show) {
        if (show) {
            app_bar.setVisibility(View.GONE);
            search_view.setVisibility(View.VISIBLE);
            search_view.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(search_view.findViewById(androidx.appcompat.R.id.search_src_text), InputMethodManager.SHOW_IMPLICIT);
        } else {
            app_bar.setVisibility(View.VISIBLE);
            search_view.setVisibility(View.GONE);
            search_view.setQuery("", false);

            // ✅ QUANDO FECHA A BUSCA, VOLTA A MOSTRAR A LISTA ATUAL (PLAYLIST OU TODAS)
            if (songAdapter != null) {
                songAdapter.submitList(new ArrayList<>(loadedMusicas));
            }
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(search_view.getWindowToken(), 0);
        }
    }

    private void filterSongs(String query) {
        if (songAdapter == null) return;
        List<Musica> filteredList = new ArrayList<>();
        if (query.isEmpty()) {
            filteredList.addAll(loadedMusicas);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
            for (Musica musica : loadedMusicas) {
                if (musica.getTitulo().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                        musica.getArtista().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredList.add(musica);
                }
            }
        }
        songAdapter.submitList(filteredList);
    }

    private void showMainMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_view_playlists) {
                startActivity(new Intent(MainActivity.this, PlaylistsActivity.class));
                return true;
            } else if (itemId == R.id.action_create_playlist) {
                showCreatePlaylistDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setHint("Nome da nova playlist");
        builder.setTitle("Criar Nova Playlist");
        builder.setView(input);
        builder.setPositiveButton("Criar", (dialog, which) -> {
            String playlistName = input.getText().toString().trim();
            if (!playlistName.isEmpty()) {
                presenter.createPlaylist(playlistName);
            } else {
                Toast.makeText(this, "O nome da playlist não pode ser vazio.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showSongOptionsPopupMenu(Musica musica, View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.song_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_play_next) {
                presenter.playSongNext(musica);
                return true;
            } else if (itemId == R.id.action_add_to_playlist) {
                presenter.onAddToPlaylistRequested(musica);
                return true;
            } else if (itemId == R.id.action_details) {
                // ✅ VERSÃO SIMPLES COM DIRETÓRIO REAL
                showSimpleSongDetails(musica);
                return true;
            }
            return false;
        });
        popup.show();
    }

    // ✅ MÉTODO SIMPLES PARA MOSTRAR DETALHES COM DIRETÓRIO
    private void showSimpleSongDetails(Musica musica) {
        String directoryPath = getSongDirectory(musica);

        String details = "📀 Título: " + musica.getTitulo() +
                "\n🎤 Artista: " + musica.getArtista() +
                "\n📁 Diretório: " + directoryPath;

        new AlertDialog.Builder(this)
                .setTitle("Detalhes da Música")
                .setMessage(details)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // ✅ MÉTODO PARA OBTER APENAS O DIRETÓRIO DA MÚSICA
    private String getSongDirectory(Musica musica) {
        try {
            String[] proj = { MediaStore.Audio.Media.DATA };
            Cursor cursor = getContentResolver().query(
                    Uri.parse(musica.getUriString()),
                    proj, null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                String fullPath = cursor.getString(column_index);
                cursor.close();

                // ✅ EXTRAIR APENAS O DIRETÓRIO (PASTA)
                if (fullPath != null) {
                    File file = new File(fullPath);
                    File directory = file.getParentFile();
                    return directory != null ? directory.getAbsolutePath() : "Diretório não encontrado";
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao obter diretório: " + e.getMessage());
        }
        return "Diretório não disponível";
    }

    private void setupSeekBar() {
        seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    presenter.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void loadAndSetupMusic() {
        List<Musica> musicas = loadMusicasFromDevice();

        // ✅ SALVA TODAS AS MÚSICAS EM UMA LISTA SEPARADA
        allMusicasFromDevice.clear();
        allMusicasFromDevice.addAll(musicas);

        if (isBound && musicService != null) {
            musicService.setSongs(musicas);
        }
        onQueueChanged(musicas);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean canLoadMusic = false;
            boolean canUseMic = false;

            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                boolean isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;

                if (permission.equals(Manifest.permission.READ_MEDIA_AUDIO) || permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    if (isGranted) canLoadMusic = true;
                }
                if (permission.equals(Manifest.permission.RECORD_AUDIO)) {
                    if (isGranted) canUseMic = true;
                }
            }

            if (canUseMic) {
                // Permissão concedida - pode usar reconhecimento quando quiser
            } else {
                Toast.makeText(this, "Permissão de microfone negada.", Toast.LENGTH_SHORT).show();
            }

            if (canLoadMusic && loadedMusicas.isEmpty()) {
                loadAndSetupMusic();
            } else if (!canLoadMusic) {
                Toast.makeText(this, "Permissão de leitura negada. Não é possível carregar músicas.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private List<Musica> loadMusicasFromDevice() {
        List<Musica> musicas = new ArrayList<>();
        ContentResolver resolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID};
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        try (Cursor cursor = resolver.query(uri, projection, selection, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                do {
                    long id = cursor.getLong(idColumn);
                    String titulo = cursor.getString(titleColumn);
                    String artista = cursor.getString(artistColumn);
                    if (artista == null || artista.equals("<unknown>")) {
                        artista = "Artista Desconhecido";
                    }
                    long albumId = cursor.getLong(albumIdColumn);
                    Uri contentUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                    Uri albumArtUri = Uri.parse("content://media/external/audio/albumart/" + albumId);
                    musicas.add(new Musica(titulo, artista, contentUri.toString(), albumArtUri.toString()));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar músicas do dispositivo: ", e);
        }
        return musicas;
    }

    @Override
    public void mostrarPlay() {
        btn_play.setImageResource(R.drawable.play1);
        if (rotationAnimator != null && rotationAnimator.isStarted()) {
            rotationAnimator.pause();
        }
    }

    @Override
    public void mostrarPause() {
        btn_play.setImageResource(R.drawable.pause);
        if (rotationAnimator != null) {
            if (rotationAnimator.isPaused()) {
                rotationAnimator.resume();
            } else {
                rotationAnimator.start();
            }
        }
    }

    @Override
    public void mostrarStop() {}

    @Override
    public void mensagemErro(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void updateProgress(int current, int total) {
        tv_current_time.setText(formatTime(current));
        tv_total_time.setText(formatTime(total));
        seek_bar.setMax(total);
        seek_bar.setProgress(current);
    }

    @Override
    public void setRepeatButtonState(MusicService.RepeatState state) {
        if (state == null) {
            state = MusicService.RepeatState.OFF;
        }
        int color = (state == MusicService.RepeatState.REPEAT_ONE) ? Color.parseColor("#4CAF50") : Color.parseColor("#B0B0B0");
        btn_repeat.setImageTintList(ColorStateList.valueOf(color));
    }

    private String formatTime(int millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    public void updateShuffleState(boolean isShuffleOn) {
        int color = isShuffleOn ? Color.parseColor("#4CAF50") : Color.parseColor("#B0B0B0");
        btn_shuffle.setImageTintList(ColorStateList.valueOf(color));
    }

    @Override
    public void mostrarDialogoPlaylists(Musica musica, List<String> nomesPlaylists) {
        if (musica == null) {
            mensagemErro("Nenhuma música selecionada.");
            return;
        }
        if (nomesPlaylists == null || nomesPlaylists.isEmpty()) {
            Toast.makeText(this, "Nenhuma playlist encontrada. Crie uma primeiro.", Toast.LENGTH_LONG).show();
            return;
        }
        showExistingPlaylistsDialog(musica, nomesPlaylists);
    }

    private void showExistingPlaylistsDialog(Musica musica, List<String> nomesPlaylists) {
        final CharSequence[] items = nomesPlaylists.toArray(new CharSequence[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Adicionar '" + musica.getTitulo() + "' a:");
        builder.setItems(items, (dialog, which) -> {
            String nomePlaylistEscolhida = nomesPlaylists.get(which);
            presenter.addSongToExistingPlaylist(nomePlaylistEscolhida, musica);
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void handlePermissionsAndLoadMusic() {
        String[] permissionsToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest = new String[]{
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissionsToRequest = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            loadAndSetupMusic();
        }
    }

    // ✅ MÉTODO PARA RESTAURAR A LISTA COMPLETA QUANDO NECESSÁRIO
    public void showAllSongs() {
        if (!allMusicasFromDevice.isEmpty()) {
            onQueueChanged(allMusicasFromDevice);
        }
    }
}