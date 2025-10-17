package com.example.leitor.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import com.example.leitor.Model.Musica;
import com.example.leitor.Model.QueueState; // Importação correta que já tínhamos
import com.google.gson.Gson;
// --- INÍCIO DA CORREÇÃO ---
// Garante que a importação correta para TypeToken está a ser usada
import com.google.gson.reflect.TypeToken;
// --- FIM DA CORREÇÃO ---
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * A classe principal da base de dados Room.
 */
@Database(entities = {Song.class, Playlist.class, PlaylistSongCrossRef.class, QueueState.class}, version = 3, exportSchema = false)
@TypeConverters({AppDatabase.Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract PlaylistDao playlistDao();
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "music_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static class Converters {
        private static final Gson gson = new Gson();

        @TypeConverter
        public static String fromMusicaList(List<Musica> musicaList) {
            if (musicaList == null) return null;
            return gson.toJson(musicaList);
        }

        @TypeConverter
        public static List<Musica> toMusicaList(String musicaListString) {
            if (musicaListString == null) return Collections.emptyList();

            // --- INÍCIO DA CORREÇÃO ---
            // Usa o TypeToken da GSON diretamente, sem o caminho completo e errado.
            Type listType = new TypeToken<List<Musica>>() {}.getType();
            // --- FIM DA CORREÇÃO ---

            return gson.fromJson(musicaListString, listType);
        }
    }
}
