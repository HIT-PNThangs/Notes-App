package com.example.hit.pnt.appnote.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.hit.pnt.appnote.R;
import com.example.hit.pnt.appnote.database.NotesDatabase;
import com.example.hit.pnt.appnote.databinding.ActivityCreateNoteBinding;
import com.example.hit.pnt.appnote.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends AppCompatActivity {
    // ở phần xml: app:behavior_peekHeight="@dimen/_30sdp" (ở phần hiện thị thêm)

    ActivityCreateNoteBinding binding;

    private String selectNoteColor;
    private String selectImagePath;

    private static final Integer REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final Integer REQUEST_CODE_SELECT_IMAGE = 2;

    private AlertDialog dialogAddURL;
    private AlertDialog dialogDeleteNote;

    private Note alreadyAvailableNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.imgBack.setOnClickListener(v -> onBackPressed());

        binding.dateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
                        .format(new Date())
        );

        binding.imgDone.setOnClickListener(view -> saveNote());

        selectNoteColor = "#292929";
        selectImagePath = "";

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        findViewById(R.id.imageRemoveWebURL).setOnClickListener(view -> {
            binding.textWebURL.setText(null);
            binding.layoutWebURL.setVisibility(View.GONE);
        });

        findViewById(R.id.imageRemoveImage).setOnClickListener(view -> {
            binding.imageNote.setImageBitmap(null);
            binding.imageNote.setVisibility(View.GONE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
            selectImagePath = "";
        });

        if (getIntent().getBooleanExtra("isFromQuickActions", false)) {
            String type = getIntent().getStringExtra("quickActionType");

            if (type != null) {
                if (type.equals("image")) {
                    selectImagePath = getIntent().getStringExtra("imagePath");
                    binding.imageNote.setImageBitmap(BitmapFactory.decodeFile(selectImagePath));
                    binding.imageNote.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setOnClickListener(view -> {

                    });
                } else if (type.equals("URL")) {
                    binding.textWebURL.setText(getIntent().getStringExtra("URL"));
                    binding.layoutWebURL.setVisibility(View.VISIBLE);
                }
            }
        }

        initMisce();
        setSubtitleIndicatorColor();
    }

    private void setViewOrUpdateNote() {
        binding.noteTitle.setText(alreadyAvailableNote.getTitle());
        binding.subtitle.setText(alreadyAvailableNote.getSubtitle());
        binding.noteText.setText(alreadyAvailableNote.getNoteText());
        binding.dateTime.setText(alreadyAvailableNote.getDateTime());

        if (alreadyAvailableNote.getImagePath() != null &&
                !alreadyAvailableNote.getImagePath().trim().isEmpty()) {
            binding.imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            binding.imageNote.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            selectImagePath = alreadyAvailableNote.getImagePath();
        }

        if (alreadyAvailableNote.getWebLink() != null &&
                !alreadyAvailableNote.getWebLink().trim().isEmpty()) {
            binding.textWebURL.setText(alreadyAvailableNote.getWebLink());
            binding.layoutWebURL.setVisibility(View.VISIBLE);
        }
    }

    private void saveNote() {
        if (binding.noteTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note title can't be empty", Toast.LENGTH_LONG).show();
            return;
        } else if (binding.subtitle.getText().toString().trim().isEmpty()
                && binding.noteText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note can't be empty", Toast.LENGTH_LONG).show();
            return;
        }

        final Note note = new Note();
        note.setTitle(binding.noteTitle.getText().toString());
        note.setSubtitle(binding.subtitle.getText().toString());
        note.setNoteText(binding.noteText.getText().toString());
        note.setDateTime(binding.dateTime.getText().toString());

        note.setColor(selectNoteColor);
        note.setImagePath(selectImagePath);

        if (binding.layoutWebURL.getVisibility() == View.VISIBLE) {
            note.setWebLink(binding.textWebURL.getText().toString());
        }

        if (alreadyAvailableNote != null) {
            note.setId(alreadyAvailableNote.getId());
        }

        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);

                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        }

        new SaveNoteTask().execute();
    }

    private void initMisce() {
        final LinearLayout colorLinearLayout = findViewById(R.id.layoutColor);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(colorLinearLayout);

        colorLinearLayout.findViewById(R.id.textMisce).setOnClickListener(view -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        final ImageView imageColor1 = colorLinearLayout.findViewById(R.id.imageColor1);
        final ImageView imageColor2 = colorLinearLayout.findViewById(R.id.imageColor2);
        final ImageView imageColor3 = colorLinearLayout.findViewById(R.id.imageColor3);
        final ImageView imageColor4 = colorLinearLayout.findViewById(R.id.imageColor4);
        final ImageView imageColor5 = colorLinearLayout.findViewById(R.id.imageColor5);

        colorLinearLayout.findViewById(R.id.viewColor1).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View view) {
                selectNoteColor = "#292929";
                imageColor1.setImageResource(R.drawable.ic_done);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                setSubtitleIndicatorColor();
            }
        });

        colorLinearLayout.findViewById(R.id.viewColor2).setOnClickListener(view -> {
            selectNoteColor = "#66FFFF";
            imageColor2.setImageResource(R.drawable.ic_done);
            imageColor1.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        colorLinearLayout.findViewById(R.id.viewColor3).setOnClickListener(view -> {
            selectNoteColor = "#FF3333";
            imageColor3.setImageResource(R.drawable.ic_done);
            imageColor1.setImageResource(0);
            imageColor2.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        colorLinearLayout.findViewById(R.id.viewColor4).setOnClickListener(view -> {
            selectNoteColor = "#CCCCCC";
            imageColor4.setImageResource(R.drawable.ic_done);
            imageColor1.setImageResource(0);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        colorLinearLayout.findViewById(R.id.viewColor5).setOnClickListener(view -> {
            selectNoteColor = "#FF99FF";
            imageColor5.setImageResource(R.drawable.ic_done);
            imageColor1.setImageResource(0);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        if (alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null
                && !alreadyAvailableNote.getColor().trim().isEmpty()) {
            switch (alreadyAvailableNote.getColor()) {
                case "#66FFFF":
                    colorLinearLayout.findViewById(R.id.viewColor2).performClick();
                    break;

                case "#FF3333":
                    colorLinearLayout.findViewById(R.id.viewColor3).performClick();
                    break;

                case "#CCCCCC":
                    colorLinearLayout.findViewById(R.id.viewColor4).performClick();
                    break;

                case "#FF99FF":
                    colorLinearLayout.findViewById(R.id.viewColor5).performClick();
                    break;
            }
        }

        colorLinearLayout.findViewById(R.id.layoutAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            CreateNoteActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_STORAGE_PERMISSION
                    );
                } else {
                    selectImage();
                }
            }
        });

        colorLinearLayout.findViewById(R.id.layoutAddUrl).setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            showAddURLDialog();
        });

        if (alreadyAvailableNote != null) {
            colorLinearLayout.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            colorLinearLayout.findViewById(R.id.layoutDeleteNote).setOnClickListener(view -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteDialog();
            });
        }
    }

    private void showDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);

        View view = LayoutInflater.from(this).inflate(
                R.layout.layout_delete_note,
                findViewById(R.id.layoutDeleteNoteContainer)
        );

        builder.setView(view);
        dialogDeleteNote = builder.create();

        if (dialogDeleteNote.getWindow() != null) {
            dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        view.findViewById(R.id.textDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                @SuppressLint("StaticFieldLeak")
                class DeleteNoteTask extends AsyncTask<Void, Void, Void> {

                    @Override
                    protected Void doInBackground(Void... voids) {
                        NotesDatabase.getDatabase(getApplicationContext()).noteDao()
                                .deleteNote(alreadyAvailableNote);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void unused) {
                        Intent intent = new Intent();

                        intent.putExtra("isNoteDeleted", true);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }

                new DeleteNoteTask().execute();
            }
        });

        view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogDeleteNote.dismiss();
            }
        });

        dialogDeleteNote.show();
    }

    private void setSubtitleIndicatorColor() {
        GradientDrawable gradientDrawable = (GradientDrawable) binding.view.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectNoteColor));
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedImageUri = data.getData();

                if (selectedImageUri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);

                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        binding.imageNote.setImageBitmap(bitmap);
                        binding.imageNote.setVisibility(View.VISIBLE);
                        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);

                        selectImagePath = getPathFromUri(selectedImageUri);

                    } catch (Exception ex) {
                        Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    private String getPathFromUri(Uri contenUri) {
        String filePath;
        Cursor cursor = getContentResolver()
                .query(contenUri, null, null, null, null);

        if (cursor == null) {
            filePath = contenUri.getPath();
        } else {
            cursor.moveToFirst();

            int index = cursor.getColumnIndex("_data");

            filePath = cursor.getString(index);
            cursor.close();
        }

        return filePath;
    }

    private void showAddURLDialog() {
        if (dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);

            View view = LayoutInflater.from(this).inflate(R.layout.layout_add_url, findViewById(R.id.layoutAddUrlContainer)
            );

            builder.setView(view);

            dialogAddURL = builder.create();
            if (dialogAddURL.getWindow() != null) {
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputURL);
            inputURL.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(view12 -> {
                if (inputURL.getText().toString().trim().isEmpty()) {
                    Toast.makeText(CreateNoteActivity.this, "Enter URL", Toast.LENGTH_LONG).show();
                } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
                    Toast.makeText(CreateNoteActivity.this, "Ener vaild URL", Toast.LENGTH_LONG).show();
                } else {
                    binding.textWebURL.setText(inputURL.getText().toString());
                    binding.layoutWebURL.setVisibility(View.VISIBLE);
                    dialogAddURL.dismiss();
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(view1 -> dialogAddURL.dismiss());
        }

        dialogAddURL.show();
    }
}