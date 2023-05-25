package pl.pollub.android.app3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.widget.*;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private EditText url;
    private TextView fileSize;
    private TextView fileType;
    private TextView downloadedValue;
    private ProgressBar progressBar;
    private BroadcastReceiver receiver;
    private static final String ACTION_PROGRESS = "pl.pollub.android.app3.ACTION_PROGRESS";
    private static final String EXTRA_DOWNLOADED_BYTES = "pl.pollub.android.app3.EXTRA_DOWNLOADED_BYTES";
    private static final String EXTRA_PROGRESS_INFO = "pl.pollub.android.app3.EXTRA_PROGRESS_INFO";

    private ProgressInfo progressInfo;
    private String savedFileType;
    private String savedFileSize;
    private String savedUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button getInfoButton = findViewById(R.id.infoButton);
        this.url = findViewById(R.id.urlEditText);
        this.fileSize = findViewById(R.id.sizeValueTextView);
        this.fileType = findViewById(R.id.typeValueTextView);
        getInfoButton.setOnClickListener(view -> getInfo());
        Button downloadButton = findViewById(R.id.downloadButton);
        downloadButton.setOnClickListener(view -> startDownloadingFile());
        this.progressBar = findViewById(R.id.downloadProgress);
        this.downloadedValue = findViewById(R.id.downloadedValueTextView);

        // Rejestrujemy odbiorcę rozgłoszeń
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals(ACTION_PROGRESS)) {
                    progressInfo = intent.getParcelableExtra(EXTRA_PROGRESS_INFO);
                    if (progressInfo != null) {
                        updateProgress(progressInfo);
                    }
                }
                if (intent.getAction().equals(ACTION_PROGRESS)) {
                    int dwnloadedBytes = intent.getIntExtra(EXTRA_DOWNLOADED_BYTES, 0);
                    downloadedValue.setText(String.valueOf(dwnloadedBytes));
                }
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_PROGRESS);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        registerReceiver(receiver, filter);

        // Przywracanie stanu aktywności
        if (savedInstanceState != null) {
            progressInfo = savedInstanceState.getParcelable("progressInfo");
            if (progressInfo != null) {
                updateProgress(progressInfo);
            }
            savedFileType = savedInstanceState.getString("fileType");
            savedFileSize = savedInstanceState.getString("fileSize");
        }

        fileType.setText(savedFileType);
        fileSize.setText(savedFileSize);
    }

    private void startDownloadingFile() {
        DownloadService.startDownloadFile(this, this.url.getText().toString());
    }

    @SuppressLint("SetTextI18n")
    private void getInfo() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        FileInfo fileInfo = new FileInfo();
        executorService.execute(() -> {
            HttpsURLConnection conn = null;
            try {
                URL url = new URL(this.url.getText().toString());
                conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                fileInfo.rozmiarPliku = conn.getContentLength();
                fileInfo.typPliku = conn.getContentType();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                this.fileType.setText(fileInfo.typPliku);
                this.fileSize.setText(Long.toString(fileInfo.rozmiarPliku));
            });
        });
    }

    private void updateProgress(ProgressInfo progressInfo) {
        switch (progressInfo.mStatus) {
            case "IN_PROGRESS":
                progressBar.setMax((int) progressInfo.mRozmiar);
                progressBar.setProgress((int) progressInfo.mPobranychBajtow);
                downloadedValue.setText(String.valueOf(progressInfo.mPobranychBajtow));
                break;
            case "COMPLETED":
                progressBar.setMax((int) progressInfo.mRozmiar);
                progressBar.setProgress((int) progressInfo.mRozmiar);
                downloadedValue.setText(String.valueOf(progressInfo.mPobranychBajtow));
                Toast.makeText(this, "Download complete", Toast.LENGTH_SHORT).show();
                break;
            case "ERROR":
                Toast.makeText(this, "An error occurred while downloading", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Wyrejestrowanie odbiorcy rozgłoszeń
        unregisterReceiver(receiver);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("progressInfo", progressInfo);
        outState.putString("fileType", fileType.getText().toString());
        outState.putString("fileSize", fileSize.getText().toString());
    }


    static class FileInfo {
        String typPliku;
        long rozmiarPliku;
    }
}
