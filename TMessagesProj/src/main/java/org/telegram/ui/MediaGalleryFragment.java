package org.telegram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ContextThemeWrapper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MediaGalleryFragment extends BaseFragment {
    private static final String PREF_NAME = "media_gallery_prefs";
    private static final String PREF_SERVER_IP = "server_ip";
    private static final String PREF_VIDEO_QUALITY = "video_quality";

    private RecyclerView recyclerView;
    private MediaGalleryAdapter adapter;
    private FloatingActionButton sendButton;

    private String serverBaseUrl;
    private final long targetDialogId; // Це ID чату

    // Конструктор, який приймає ID чату
    public MediaGalleryFragment(long dialogId) {
        super();
        this.targetDialogId = dialogId;
    }

    @Override
    public View createView(Context context) {
        // Отримуємо збережений IP або дефолтний
        SharedPreferences prefs = getParentActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        serverBaseUrl = prefs.getString(PREF_SERVER_IP, "http://192.168.31.176:8000");

        // Використовуємо тему Material для чіпів
        final Context materialContext = new ContextThemeWrapper(context, com.google.android.material.R.style.Theme_MaterialComponents_DayNight);

        // --- ACTION BAR ---
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Галерея сервера");
        // Додаємо кнопку налаштувань (3 крапки)
        actionBar.createMenu().addItem(1, R.drawable.ic_ab_other);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
                if (id == 1) {
                    // Відкриття налаштувань
                    if (getParentActivity() instanceof FragmentActivity) {
                        MediaGallerySettingsBottomSheet bottomSheet = new MediaGallerySettingsBottomSheet();
                        bottomSheet.show(((FragmentActivity) getParentActivity()).getSupportFragmentManager(), bottomSheet.getTag());
                    }
                }
            }
        });

        // Слухаємо зміни IP з BottomSheet
        if (getParentActivity() instanceof FragmentActivity) {
            ((FragmentActivity) getParentActivity()).getSupportFragmentManager().setFragmentResultListener(
                    MediaGallerySettingsBottomSheet.REQUEST_KEY_IP_UPDATE,
                    (LifecycleOwner) getParentActivity(),
                    (requestKey, result) -> {
                        String newIp = result.getString(MediaGallerySettingsBottomSheet.BUNDLE_KEY_NEW_IP);
                        if (newIp != null) {
                            serverBaseUrl = newIp;
                            adapter.updateServerUrl(newIp);
                            loadMediaItems();
                        }
                    }
            );
        }

        // --- ГОЛОВНИЙ LAYOUT ---
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        // 1. ЧІПИ (Фільтри)
        HorizontalScrollView chipScrollView = new HorizontalScrollView(context);
        chipScrollView.setHorizontalScrollBarEnabled(false);
        chipScrollView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));

        ChipGroup chipGroup = new ChipGroup(materialContext);
        chipGroup.setSingleSelection(true);
        String[] chipLabels = {"Всі", "Фото", "Відео", "Файли"};
        for (int i = 0; i < chipLabels.length; i++) {
            Chip chip = new Chip(materialContext);
            chip.setText(chipLabels[i]);
            chip.setCheckable(true);
            if (i == 0) chip.setChecked(true);
            chipGroup.addView(chip);
        }
        chipScrollView.addView(chipGroup);
        mainLayout.addView(chipScrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 2. СІТКА (RecyclerView)
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 3));
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(0, 0, 0, AndroidUtilities.dp(80)); // Відступ знизу для кнопки

        mainLayout.addView(recyclerView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // Контейнер для FAB
        FrameLayout containerLayout = new FrameLayout(context);
        containerLayout.addView(mainLayout);

        // 3. КНОПКА ВІДПРАВКИ (FAB)
        sendButton = new FloatingActionButton(materialContext);
        sendButton.setImageResource(R.drawable.ic_send);
        sendButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Theme.getColor(Theme.key_featuredStickers_addButton)));
        sendButton.setVisibility(View.GONE);

        FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fabLp.gravity = Gravity.BOTTOM | Gravity.END;
        fabLp.setMargins(0, 0, AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        containerLayout.addView(sendButton, fabLp);

        // Клік по кнопці відправки
        sendButton.setOnClickListener(v -> {
            ArrayList<String> selectedFiles = adapter.getSelectedItems();
            if (!selectedFiles.isEmpty()) {
                sendSelectionToServer(selectedFiles);
            }
        });

        // Ініціалізація адаптера
        // Передаємо 'this' (фрагмент) в конструктор адаптера
        adapter = new MediaGalleryAdapter(context, serverBaseUrl, this, count -> {
            if (count > 0 && sendButton.getVisibility() == View.GONE) {
                sendButton.show();
            } else if (count == 0 && sendButton.getVisibility() == View.VISIBLE) {
                sendButton.hide();
            }
        });
        recyclerView.setAdapter(adapter);

        fragmentView = containerLayout;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMediaItems();
    }

    // --- МЕТОД ДЛЯ ВІДКРИТТЯ ПЕРЕГЛЯДУ ---
    public void openPreview(MediaItem item) {
        // Формуємо URL до оригіналу
        String originalUrl = serverBaseUrl + "/original/" + item.getFilename();

        // Створюємо фрагмент перегляду (переконайся, що файл MediaPreviewFragment.java існує!)
        MediaPreviewFragment previewFragment = new MediaPreviewFragment(
                serverBaseUrl,  // <--- ПЕРЕДАЄМО ЧИСТИЙ BASE URL
                item.getType(),
                item.getFilename()
        );
        presentFragment(previewFragment);
    }

    // --- МЕТОДИ СЕРВЕРА ---

    private void loadMediaItems() {
        OkHttpClient client = new OkHttpClient();
        String url = serverBaseUrl + "/gallery/";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AndroidUtilities.runOnUIThread(() -> Toast.makeText(getParentActivity(), "Помилка з'єднання: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    try {
                        Gson gson = new Gson();
                        Type listType = new TypeToken<ArrayList<MediaItem>>(){}.getType();
                        ArrayList<MediaItem> items = gson.fromJson(json, listType);
                        AndroidUtilities.runOnUIThread(() -> adapter.setData(items));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void sendSelectionToServer(ArrayList<String> filenames) {
        OkHttpClient client = new OkHttpClient();
        Gson gson = new Gson();
        Map<String, Object> data = new HashMap<>();

        data.put("filenames", filenames);
        // Перетворюємо ID в рядок, щоб уникнути помилки 422
        data.put("chat_id", String.valueOf(targetDialogId));

        SharedPreferences prefs = getParentActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        data.put("video_quality", prefs.getString(PREF_VIDEO_QUALITY, "full"));

        String jsonBody = gson.toJson(data);
        okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.get("application/json"));

        String url = serverBaseUrl + "/gallery/send";

        Request request = new Request.Builder().url(url).post(body).build();

        Toast.makeText(getParentActivity(), "Відправка запиту на сервер...", Toast.LENGTH_SHORT).show();
        finishFragment();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AndroidUtilities.runOnUIThread(() -> Toast.makeText(getParentActivity(), "Не вдалося відправити команду", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    AndroidUtilities.runOnUIThread(() -> Toast.makeText(getParentActivity(), "Сервер повернув помилку: " + response.code(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    // --- ВНУТРІШНІ КЛАСИ ---

    public static class MediaItem {
        private String filename;
        private String thumbnail;
        private String type;

        public String getFilename() { return filename; }
        public String getThumbnail() { return thumbnail; }
        public String getType() { return type; }
    }

    public static class MediaGalleryAdapter extends RecyclerView.Adapter<MediaGalleryAdapter.ViewHolder> {
        public interface SelectionListener {
            void onSelectionChanged(int count);
        }

        private final Context context;
        private String serverBaseUrl;
        private ArrayList<MediaItem> items = new ArrayList<>();
        private final Set<String> selected = new HashSet<>();
        private final SelectionListener listener;
        private final MediaGalleryFragment parentFragment; // Додали посилання на фрагмент

        public MediaGalleryAdapter(Context context, String url, MediaGalleryFragment fragment, SelectionListener listener) {
            this.context = context;
            this.serverBaseUrl = url;
            this.parentFragment = fragment;
            this.listener = listener;
        }

        public void updateServerUrl(String newUrl) { this.serverBaseUrl = newUrl; }

        public void setData(ArrayList<MediaItem> newItems) {
            this.items = newItems;
            selected.clear();
            listener.onSelectionChanged(0);
            notifyDataSetChanged();
        }

        public ArrayList<String> getSelectedItems() { return new ArrayList<>(selected); }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.gallery_item_layout, parent, false);

            // КВАДРАТНІ ЕЛЕМЕНТИ (фікс розтягування)
            int totalWidth = parent.getMeasuredWidth();
            if (totalWidth == 0) {
                totalWidth = AndroidUtilities.displaySize.x;
            }
            int margin = AndroidUtilities.dp(2);
            int itemSize = (totalWidth / 3) - (margin * 2);
            GridLayoutManager.LayoutParams lp = new GridLayoutManager.LayoutParams(itemSize, itemSize);
            lp.setMargins(margin, margin, margin, margin);
            view.setLayoutParams(lp);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MediaItem item = items.get(position);
            String thumbUrl = serverBaseUrl + "/thumbnail/" + item.getThumbnail();

            Glide.with(context)
                    .load(thumbUrl)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(AndroidUtilities.dp(4))))
                    .into(holder.image);

            boolean isSelected = selected.contains(item.getFilename());

            holder.selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            if (isSelected) {
                // Заміни msg_round_check_filled на іконку яка є в проекті, якщо немає
                holder.checkIcon.setBackgroundResource(R.drawable.msg_check_s);
            } else {
                holder.checkIcon.setBackgroundResource(R.drawable.msg_select);
            }

            boolean isVideo = item.getType() != null && item.getType().contains("video");
            holder.videoIndicator.setVisibility(isVideo ? View.VISIBLE : View.GONE);

            // 1. Клік по кружечку (Вибір)
            holder.checkContainer.setOnClickListener(v -> {
                toggleSelection(item, position);
            });

            // 2. Клік по картинці (Відкриття)
            holder.itemView.setOnClickListener(v -> {
                if (!selected.isEmpty()) {
                    toggleSelection(item, position);
                } else {
                    // Викликаємо метод батьківського фрагмента
                    parentFragment.openPreview(item);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                toggleSelection(item, position);
                return true;
            });
        }

        private void toggleSelection(MediaItem item, int position) {
            if (selected.contains(item.getFilename())) {
                selected.remove(item.getFilename());
            } else {
                selected.add(item.getFilename());
            }
            notifyItemChanged(position);
            listener.onSelectionChanged(selected.size());
        }

        // --- ОСЬ ВІН, ЗНИКЛИЙ МЕТОД ---
        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView image, checkIcon;
            View selectionOverlay, checkContainer, videoIndicator;

            ViewHolder(View v) {
                super(v);
                image = v.findViewById(R.id.item_image);
                selectionOverlay = v.findViewById(R.id.selection_overlay);
                checkIcon = v.findViewById(R.id.check_icon);
                checkContainer = v.findViewById(R.id.check_container);
                videoIndicator = v.findViewById(R.id.video_indicator);
            }
        }
    }

    // --- BOTTOM SHEET (ВІДНОВЛЕНО) ---
    public static class MediaGallerySettingsBottomSheet extends BottomSheetDialogFragment {
        private static final String PREF_NAME = "media_gallery_prefs";
        private static final String PREF_VIDEO_QUALITY = "video_quality";
        private static final String PREF_SERVER_IP = "server_ip";

        public static final String REQUEST_KEY_IP_UPDATE = "ip_update_request_key";
        public static final String BUNDLE_KEY_NEW_IP = "new_ip_bundle_key";

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.bottom_sheet_settings_layout, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            view.findViewById(R.id.option_video_quality).setOnClickListener(v -> showVideoQualityDialog());
            view.findViewById(R.id.option_ip_address).setOnClickListener(v -> showIpAddressDialog());
        }

        private void showVideoQualityDialog() {
            // Логіка діалогу якості
            Toast.makeText(requireContext(), "Налаштування якості", Toast.LENGTH_SHORT).show();
        }

        private void showIpAddressDialog() {
            SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String currentIp = prefs.getString(PREF_SERVER_IP, "http://192.168.31.176:8000");
            EditText editText = new EditText(requireContext());
            editText.setText(currentIp);

            new AlertDialog.Builder(requireContext())
                    .setTitle("IP адреса сервера")
                    .setView(editText)
                    .setPositiveButton("Зберегти", (dialog, which) -> {
                        String newIp = editText.getText().toString().trim();
                        if (!newIp.isEmpty()) {
                            prefs.edit().putString(PREF_SERVER_IP, newIp).apply();
                            Bundle result = new Bundle();
                            result.putString(BUNDLE_KEY_NEW_IP, newIp);
                            getParentFragmentManager().setFragmentResult(REQUEST_KEY_IP_UPDATE, result);
                        }
                    })
                    .show();
        }
    }
}