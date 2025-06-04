package com.kuro.signreader;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.xbh.sdk3.System.SystemHelper;
import com.xbh.sdk3.client.UserAPI;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {
    EditText appPkg;
    Button btnGet, btnSave;
    TextView resultBase64, resultCpp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        UserAPI.getInstance().init(this);

        SystemHelper systemHelper = new SystemHelper();
        Log.d("SystemHelper", "Executing command: whoami");

        Log.d("SystemHelper", "Output: " + systemHelper.executeCommand("whoami"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        appPkg = findViewById(R.id.appPkg);
        resultBase64 = findViewById(R.id.resultBase64);
        resultCpp = findViewById(R.id.resultCpp);

        btnGet = findViewById(R.id.btnGetSign);
        btnGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    PackageManager packageManager = getPackageManager();
                    String pkgName = appPkg.getText() != null ? appPkg.getText().toString().trim() : "";
                    if (pkgName.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Package name required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PackageInfo packageInfo = packageManager.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES);

                    Signature[] signatures = packageInfo.signatures;

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeByte(signatures.length);

                    StringBuilder sb = new StringBuilder();
                    sb.append("std::vector<std::vector<uint8_t>> apk_signatures {");
                    for (Signature value : signatures) {
                        sb.append("{");
                        dos.writeInt(value.toByteArray().length);
                        dos.write(value.toByteArray());
                        for (int j = 0; j < value.toByteArray().length; j++) {
                            sb.append(String.format("0x%02X", value.toByteArray()[j]));
                            if (j != value.toByteArray().length - 1) {
                                sb.append(",");
                            }
                        }
                        sb.append("}");
                    }
                    sb.append("};");

                    dos.close();
                    baos.close();

                    resultBase64.setText("Base64: " + Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT));
                    resultCpp.setText("C++: " + sb.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String pkgName = appPkg.getText() != null ? appPkg.getText().toString().trim() : "";
                    if (pkgName.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Package name required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String path = "/sdcard/" + pkgName + "_signatures.txt";

                    String content = resultBase64.getText().toString() + "\n" + resultCpp.getText().toString();

                    // Escapa i caratteri speciali per la shell (come " e $)
                    String safeContent = content.replace("\"", "\\\"").replace("$", "\\$");

                    SystemHelper helper = new SystemHelper();
                    String command = "echo \"" + safeContent + "\" > \"" + path + "\"";

                    String output = helper.executeCommand(command);
                    Log.d("SystemHelper", "Write output: " + output);

                    Toast.makeText(MainActivity.this, "Saved to " + path, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error saving file", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
