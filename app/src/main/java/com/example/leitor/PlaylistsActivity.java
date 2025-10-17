package com.example.leitor;

// --- INÍCIO DAS ADIÇÕES ---
import android.content.DialogInterface;
import android.view.View;
import android.widget.PopupMenu;
import androidx.appcompat.app.AlertDialog;
// --- FIM DAS ADIÇÕES ---
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.leitor.data.Playlist;
import com.example.leitor.data.PlaylistRepository;

import java.util.List;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

// --- INÍCIO DA ALTERAÇÃO 1: Implementar a nova interface unificada ---
public class PlaylistsActivity extends AppCompatActivity implements PlaylistAdapter.OnPlaylistInteractionListener {
// --- FIM DA ALTERAÇÃO 1 ---

    private static final String TAG = "PlaylistsActivity";
    private RecyclerView rvPlaylists;
    private PlaylistAdapter adapter;
    private PlaylistRepository playlistRepository;

    private final CompositeDisposable disposables = new CompositeDisposable();

    public static final String EXTRA_PLAYLIST_ID = "com.example.leitor.EXTRA_PLAYLIST_ID";
    public static final String EXTRA_PLAYLIST_NAME = "com.example.leitor.EXTRA_PLAYLIST_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlists);

        ImageButton btnBack = findViewById(R.id.btn_back_from_playlists);
        btnBack.setOnClickListener(v -> finish());

        playlistRepository = new PlaylistRepository(this);

        rvPlaylists = findViewById(R.id.rv_playlists);
        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));

        loadPlaylists();
    }

    private void loadPlaylists() {
        Log.d(TAG, "Iniciando a subscrição para buscar playlists.");

        disposables.add(
                playlistRepository.getAllPlaylists()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                playlists -> {
                                    Log.d(TAG, playlists.size() + " playlists recebidas do banco de dados.");
                                    if (playlists.isEmpty() && adapter == null) {
                                        Toast.makeText(PlaylistsActivity.this, "Nenhuma playlist criada ainda.", Toast.LENGTH_SHORT).show();
                                    }

                                    if (adapter == null) {
                                        // A activity 'this' agora é passada como o listener unificado.
                                        adapter = new PlaylistAdapter(playlists, this);
                                        rvPlaylists.setAdapter(adapter);
                                    } else {
                                        adapter.updatePlaylists(playlists);
                                    }
                                },
                                throwable -> {
                                    Log.e(TAG, "Erro ao buscar playlists", throwable);
                                    Toast.makeText(PlaylistsActivity.this, "Erro ao carregar playlists.", Toast.LENGTH_SHORT).show();
                                }
                        )
        );
    }

    @Override
    public void onPlaylistClick(Playlist playlist) {
        Intent intent = new Intent(this, PlaylistDetailActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlist.playlistId);
        intent.putExtra(EXTRA_PLAYLIST_NAME, playlist.getName());
        startActivity(intent);
    }

    // --- INÍCIO DA ALTERAÇÃO 2: Implementação do clique no botão de opções ---
    /**
     * Este método é chamado pelo adapter quando o botão de três pontinhos é clicado.
     * @param playlist A playlist correspondente.
     * @param anchorView A view do botão, usada para "ancorar" o menu.
     */
    @Override
    public void onPlaylistOptionsClick(Playlist playlist, View anchorView) {
        // 1. Cria um PopupMenu ancorado na view do botão.
        PopupMenu popup = new PopupMenu(this, anchorView);

        // 2. Infla (carrega) o menu a partir de um ficheiro XML que vamos criar.
        popup.getMenuInflater().inflate(R.menu.playlist_item_menu, popup.getMenu());

        // 3. Define a ação para o clique nos itens do menu.
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_playlist) {
                // Se o item "Apagar" for clicado, mostra o diálogo de confirmação.
                showDeleteConfirmationDialog(playlist);
                return true;
            }
            return false;
        });

        // 4. Mostra o menu.
        popup.show();
    }

    /**
     * O método de clique longo foi removido, pois a sua lógica agora está no menu.
     */

    // O nome do método foi alterado para maior clareza.
    private void showDeleteConfirmationDialog(Playlist playlist) {
        // Cria e exibe um diálogo de confirmação (lógica que você já tinha).
        new AlertDialog.Builder(this)
                .setTitle("Apagar Playlist")
                .setMessage("Tem a certeza que quer apagar a playlist '" + playlist.getName() + "'?")
                .setPositiveButton("Apagar", (dialog, which) -> {
                    // Se o utilizador clicar em "Apagar", chama o método para apagar.
                    deletePlaylist(playlist);
                })
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deletePlaylist(Playlist playlist) {
        playlistRepository.deletePlaylist(playlist);
        // O Flowable do Room irá atualizar a UI automaticamente.
        Toast.makeText(this, "Playlist '" + playlist.getName() + "' apagada.", Toast.LENGTH_SHORT).show();
    }
    // --- FIM DA ALTERAÇÃO 2 ---

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
        Log.d(TAG, "Subscrições RxJava limpas no onDestroy.");
    }
}
