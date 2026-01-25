package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.yalantis.ucrop.UCrop;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MediaPreviewFragment extends BaseFragment {

    private String serverBaseUrl;
    private String mediaType;
    private String filename;
    private String fullUrl;

    private ImageView imageView;
    private VideoView videoView;
    private ProgressBar progressBar;
    private FrameLayout contentLayout;

    public MediaPreviewFragment(String serverUrl, String type, String filename) {
        this.serverBaseUrl = serverUrl;
        this.mediaType = type;
        this.filename = filename;
        this.fullUrl = serverUrl + "/original/" + filename;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(filename);

        // Додаємо кнопку "Редагувати" (олівець) тільки для фото
        if ("image".equals(mediaType)) {
            actionBar.createMenu().addItem(1, R.drawable.group_edit);
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
                if (id == 1) {
                    // 1. Починаємо цикл: Скачуємо файл
                    downloadAndEdit();
                }
            }
        });

        FrameLayout layout = new FrameLayout(context);
        layout.setBackgroundColor(0xFF000000);

        contentLayout = new FrameLayout(context);
        layout.addView(contentLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Прогрес бар (крутілка)
        progressBar = new ProgressBar(context);
        FrameLayout.LayoutParams progressLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        progressLp.gravity = Gravity.CENTER;
        progressBar.setVisibility(View.GONE);
        layout.addView(progressBar, progressLp);

        loadImageOrVideo(context);

        fragmentView = layout;
        return fragmentView;
    }

    private void loadImageOrVideo(Context context) {
        contentLayout.removeAllViews();
        if ("image".equals(mediaType)) {
            imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            contentLayout.addView(imageView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            // skipMemoryCache(true) і DiskCacheStrategy.NONE потрібні, щоб після редагування ми побачили нове фото, а не старе з кешу
            Glide.with(context)
                    .load(fullUrl)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(imageView);

        } else if ("video".equals(mediaType)) {
            videoView = new VideoView(context);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            contentLayout.addView(videoView, lp);

            MediaController mediaController = new MediaController(context);
            videoView.setMediaController(mediaController);
            videoView.setVideoPath(fullUrl);
            videoView.start();
        }
    }

    // --- ЛОГІКА РЕДАГУВАННЯ ---

    // КРОК 1: Скачуємо файл з сервера в кеш телефону
    private void downloadAndEdit() {
        setLoading(true);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(fullUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showError("Помилка завантаження: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    showError("Сервер помилка: " + response.code());
                    return;
                }

                // Зберігаємо файл у тимчасову папку
                File cacheDir = getParentActivity().getCacheDir();
                File localFile = new File(cacheDir, "temp_edit_" + filename);

                try (InputStream is = response.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(localFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }

                    // КРОК 2: Запускаємо uCrop
                    startUCrop(localFile);

                } catch (Exception e) {
                    showError("Помилка збереження: " + e.getMessage());
                }
            }
        });
    }

    // КРОК 2: Запуск Активіті uCrop
    private void startUCrop(File sourceFile) {
        AndroidUtilities.runOnUIThread(() -> {
            setLoading(false);

            Uri sourceUri = Uri.fromFile(sourceFile);
            // Файл, куди uCrop збереже результат
            File destFile = new File(getParentActivity().getCacheDir(), "cropped_" + filename);
            Uri destUri = Uri.fromFile(destFile);

            UCrop.Options options = new UCrop.Options();
            options.setToolbarColor(Theme.getColor(Theme.key_actionBarDefault));
            options.setStatusBarColor(Theme.getColor(Theme.key_actionBarDefault));
            options.setActiveControlsWidgetColor(Theme.getColor(Theme.key_radioBackgroundChecked));

            // Запускаємо uCrop. Важливо використовувати start(activity, fragment)
            Intent cropIntent = UCrop.of(sourceUri, destUri)
                    .withOptions(options)
                    .getIntent(getParentActivity()); // Отримати Intent для UCrop

            // Запускаємо Activity для отримання результату.
            // Припускаємо, що BaseFragment має метод startActivityForResult,
            // який коректно перенаправляє результат до onActivityResult цього фрагмента.
            startActivityForResult(cropIntent, UCrop.REQUEST_CROP);

        });
    }

    // КРОК 3: Обробка результату від uCrop

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                // КРОК 4: Завантажуємо результат назад на сервер
                uploadEditedFile(new File(resultUri.getPath()));
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            showError("Помилка кропу: " + (cropError != null ? cropError.getMessage() : "unknown"));
        }
    }

    // КРОК 4: Відправка на сервер
    private void uploadEditedFile(File file) {
        setLoading(true);
        OkHttpClient client = new OkHttpClient();

        // Формуємо Multipart запит (як форма в HTML)
        RequestBody fileBody = RequestBody.create(file, MediaType.parse("image/*"));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename, fileBody) // Важливо: використовуємо те ж ім'я filename
                .build();

        // Використовуємо твій існуючий endpoint /upload/
        // Він просто перезапише файл, якщо ім'я співпадає (а ми передаємо filename)
        Request request = new Request.Builder()
                .url(serverBaseUrl + "/upload/")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showError("Помилка вивантаження: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    AndroidUtilities.runOnUIThread(() -> {
                        setLoading(false);
                        Toast.makeText(getParentActivity(), "Збережено!", Toast.LENGTH_SHORT).show();
                        // Оновлюємо картинку на екрані
                        loadImageOrVideo(getParentActivity());

                        // Треба ще оновити кеш мініатюр на сервері, але це сервер зробить сам
                    });
                } else {
                    showError("Сервер відповів: " + response.code());
                }
            }
        });
    }

    private void setLoading(boolean loading) {
        AndroidUtilities.runOnUIThread(() -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            contentLayout.setAlpha(loading ? 0.5f : 1.0f);
        });
    }

    private void showError(String msg) {
        AndroidUtilities.runOnUIThread(() -> {
            setLoading(false);
            Toast.makeText(getParentActivity(), msg, Toast.LENGTH_LONG).show();
        });
    }
}