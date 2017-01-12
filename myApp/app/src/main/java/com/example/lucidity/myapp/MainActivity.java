package com.example.lucidity.myapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.view.View;
import android.widget.Spinner;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import android.util.Log;
import java.io.File;
import java.io.IOException;

import android.support.v4.content.FileProvider;
import android.os.Environment;
import com.soundcloud.android.crop.Crop;

public class MainActivity extends AppCompatActivity {
    public static final String PREFER_NAME = "myPref";
    public static final String MY_NAME = "myName";
    public static final String MY_EMAIL = "myEmail";
    public static final String MY_PHONE = "myPhone";
    public static final String MY_GENDER = "myGender";
    public static final String MY_MAJOR = "myMajor";
    public static final String MY_PHOTO = "myPhoto";
    public static final String URI_KEY = "uri_record";

    public static final int CAPTURE_PHOTO = 1;
    private Uri UriPath = null;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.view_photo);

        boolean isRestarted = false;
        if (savedInstanceState != null) {
            UriPath = savedInstanceState.getParcelable(URI_KEY);
            isRestarted = true;
        }

        loadProfile(isRestarted);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the settings before the activity goes into background
        outState.putParcelable(URI_KEY, UriPath);
    }

    // load the saved settings if there are ones
    protected void loadProfile(boolean isRestarted) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFER_NAME, MODE_PRIVATE);
        String name = sharedPreferences.getString(MY_NAME, "");
        String email = sharedPreferences.getString(MY_EMAIL, "");
        String phone = sharedPreferences.getString(MY_PHONE, "");
        String photo = sharedPreferences.getString(MY_PHOTO, "");
        int gender = sharedPreferences.getInt(MY_GENDER, -1);

        ((EditText)findViewById(R.id.edit_name)).setText(name);
        ((EditText)findViewById(R.id.edit_email)).setText(email);
        ((EditText)findViewById(R.id.edit_phone)).setText(phone);
        if (gender >= 0) {
            RadioButton radioButton = (RadioButton) ((RadioGroup)findViewById(R.id.radio_gender))
                    .getChildAt(gender);
            radioButton.setChecked(true);
        }
        ((Spinner)findViewById(R.id.spinner_major)).setSelection(sharedPreferences.getInt(MY_MAJOR, 0));
        if (isRestarted == true && UriPath != null) {
            imageView.setImageURI(UriPath);
        } else if (photo.length() != 0) {
            UriPath = Uri.parse(photo);
            imageView.setImageURI(Uri.parse(photo));
        } else {
            imageView.setImageResource(R.drawable.default_photo);
        }

    }

    private void getPermission() {
        if (Build.VERSION.SDK_INT >= 23
                && (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 0);
        }
    }

    // take a picture and crop it
    public void onChangeClick(View view) {
        getPermission();
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File newFile = File.createTempFile("newphoto", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
                UriPath = FileProvider.getUriForFile(this, "com.example.lucidity.myapp.fileprovider", newFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, UriPath);
                startActivityForResult(cameraIntent, CAPTURE_PHOTO);
            } catch (ActivityNotFoundException e) {
                (Toast.makeText(this, "Crop is not supported", Toast.LENGTH_SHORT)).show();
            } catch (IOException e) {
                Log.d("Output", "IOException");
            }
        }
    }

    protected void onActivityResult(int request_code, int result_code, Intent data) {
        if (result_code == RESULT_OK) {
            try {
                switch (request_code) {
                    case CAPTURE_PHOTO:
                        // crop the photo and save in the same uri
                        Crop.of(UriPath, UriPath).asSquare().start(this);
                        break;
                    case Crop.REQUEST_CROP:
                        imageView.setImageURI(Crop.getOutput(data));
                        break;
                    default:
                        return;
                }
            } catch (ActivityNotFoundException e) {
                (Toast.makeText(this, "Crop is not supported", Toast.LENGTH_SHORT)).show();
            }
        }
    }

    // save all the current settings
    public void onSaveClick(View view) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFER_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();

        String name = ((EditText)findViewById(R.id.edit_name)).getText().toString();
        String email = ((EditText)findViewById(R.id.edit_email)).getText().toString();
        String phone = ((EditText)findViewById(R.id.edit_phone)).getText().toString();
        RadioGroup genderGroup = (RadioGroup)findViewById(R.id.radio_gender);
        int gender = genderGroup.indexOfChild(findViewById(genderGroup.getCheckedRadioButtonId()));
        int major = ((Spinner)findViewById(R.id.spinner_major)).getSelectedItemPosition();

        editor.putString(MY_NAME, name);
        editor.putString(MY_EMAIL, email);
        editor.putString(MY_PHONE, phone);
        editor.putInt(MY_GENDER, gender);
        editor.putInt(MY_MAJOR, major);
        if (UriPath != null) {
            editor.putString(MY_PHOTO, UriPath.toString());
        }
        editor.commit();
    }

    // kill the program
    public void onCloseClick(View view) {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
}
