package com.example.leitor;

import android.content.Context;
import android.util.Log;
import com.acrcloud.rec.ACRCloudClient;
import com.acrcloud.rec.ACRCloudConfig;
import com.acrcloud.rec.ACRCloudResult;
import com.acrcloud.rec.IACRCloudListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AudioRecognizer {

    private static final String TAG = "AudioRecognizer";

    private final ACRCloudClient mClient;
    private boolean mProcessing = false;
    private final RecognitionCallback callback;

    public interface RecognitionCallback {
        void onResult(String title, String artist);
        void onError(String message);
    }

    public AudioRecognizer(Context context, RecognitionCallback callback) {
        this.callback = callback;

        ACRCloudConfig mConfig = new ACRCloudConfig();
        mConfig.acrcloudListener = new IACRCloudListener() {
            @Override
            public void onResult(ACRCloudResult acrResult) {
                mProcessing = false;
                if (acrResult == null) {
                    if (callback != null) callback.onError("Erro: resultado nulo do ACRCloud.");
                    return;
                }

                String result = acrResult.getResult();
                Log.d(TAG, "Raw Result: " + result);
                parseResult(result);
            }

            @Override
            public void onVolumeChanged(double volume) {
                // Você pode usar isso pra mostrar um visualizador de volume, se quiser.
            }
        };

        // ✅ Suas credenciais ACRCloud
        mConfig.host = "identify-eu-west-1.acrcloud.com";
        mConfig.accessKey = "00b2c680deeb30f98e05d0a984f329b4";
        mConfig.accessSecret = "UPmzBRWTUEOuHtA07lAuLHb3HlbCTX7pYYCMw41F";

        mConfig.context = context;
        mConfig.recMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_REMOTE;

        mClient = new ACRCloudClient();
        boolean success = mClient.initWithConfig(mConfig);
        if (!success) {
            Log.e(TAG, "Falha ao inicializar o cliente ACRCloud");
            if (this.callback != null) {
                this.callback.onError("Falha ao inicializar o reconhecimento.");
            }
        }
    }

    private void parseResult(String result) {
        try {
            if (result == null) {
                if (callback != null) callback.onError("Nenhum resultado recebido.");
                return;
            }

            JSONObject json = new JSONObject(result);
            JSONObject status = json.getJSONObject("status");
            int code = status.getInt("code");

            if (code == 0 && json.has("metadata")) {
                JSONObject metadata = json.getJSONObject("metadata");
                if (metadata.has("music")) {
                    JSONArray musicArray = metadata.getJSONArray("music");
                    if (musicArray.length() > 0) {
                        JSONObject music = musicArray.getJSONObject(0);
                        String title = music.optString("title", "Título desconhecido");
                        String artist = "Artista desconhecido";

                        if (music.has("artists")) {
                            JSONArray artists = music.getJSONArray("artists");
                            if (artists.length() > 0) {
                                artist = artists.getJSONObject(0).optString("name", artist);
                            }
                        }

                        if (callback != null) callback.onResult(title, artist);
                        return;
                    }
                }
            }

            // ❌ Caso não reconheça nada
            String msg = status.has("msg") ? status.getString("msg") : "Nenhuma música reconhecida.";
            if (callback != null) callback.onError(msg);
            Log.e(TAG, "ACRCloud Error: code=" + code + ", msg=" + msg);

        } catch (JSONException e) {
            Log.e(TAG, "Erro ao interpretar JSON: " + e.getMessage());
            if (callback != null) callback.onError("Erro ao processar o resultado.");
        }
    }

    public void startRecognition() {
        if (mClient == null) {
            if (callback != null) callback.onError("Cliente ACRCloud não inicializado.");
            return;
        }
        if (mProcessing) return;

        mProcessing = true;
        boolean started = mClient.startRecognize();
        if (!started) {
            mProcessing = false;
            if (callback != null) callback.onError("Falha ao iniciar reconhecimento.");
        }
    }

    public void stopRecognition() {
        if (mClient != null && mProcessing) {
            mClient.cancel();
        }
        mProcessing = false;
    }

    public void release() {
        if (mClient != null) {
            mClient.release();
        }
    }

    public boolean isProcessing() {
        return mProcessing;
    }
}
