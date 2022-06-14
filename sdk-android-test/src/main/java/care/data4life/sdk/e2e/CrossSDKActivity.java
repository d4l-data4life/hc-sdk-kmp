/*
 * Copyright (c) 2020 D4L data4life gGmbH / All rights reserved.
 *
 * D4L owns all legal rights, title and interest in and to the Software Development Kit ("SDK"),
 * including any intellectual property rights that subsist in the SDK.
 *
 * The SDK and its documentation may be accessed and used for viewing/review purposes only.
 * Any usage of the SDK for other purposes, including usage for the development of
 * applications/third-party applications shall require the conclusion of a license agreement
 * between you and D4L.
 *
 * If you are interested in licensing the SDK for your own applications/third-party
 * applications and/or if you’d like to contribute to the development of the SDK, please
 * contact D4L by email to help@data4life.care.
 */

package care.data4life.sdk.e2e;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.snackbar.Snackbar;

import care.data4life.sdk.Data4LifeClient;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.listener.Callback;

public class CrossSDKActivity extends AppCompatActivity {
    public enum ScreenState {LoginVisible, LogoutVisible}

    private static final String UNEXPECTED_CASE = "Unexpected case!";

    private ConstraintLayout rootCL;
    private Button loginLogoutBTN;
    private ScreenState screenState = ScreenState.LoginVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.d4l_sdk_activity_cross_sdk);
        rootCL = findViewById(R.id.rootCL);
        loginLogoutBTN = findViewById(R.id.loginLogoutBtn);
        loginLogoutBTN.setOnClickListener(loginLogoutListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Data4LifeClient.D4L_AUTH) {
            if (resultCode == Activity.RESULT_OK) {
                showSnackMsg(R.string.d4l_sdk_successful_login);
                renderScreen(ScreenState.LogoutVisible);
            } else {
                showSnackMsg(R.string.d4l_sdk_failed_login);
            }
        }
    }

    @Override
    public void finish() {
        //ignore
    }

    public void explicitFinish() {
        super.finish();
    }

    private View.OnClickListener loginLogoutListener = view -> {
        switch (screenState) {
            case LoginVisible:
                loginUser();
                break;
            case LogoutVisible:
                logoutUser();
                break;
            default:
                throw new RuntimeException(UNEXPECTED_CASE);
        }
    };

    private void loginUser() {
        Intent loginIntent = Data4LifeClient.getInstance().getLoginIntent(null);
        startActivityForResult(loginIntent, Data4LifeClient.D4L_AUTH);
    }

    private void logoutUser() {
        Data4LifeClient.getInstance().logout(new Callback() {
            @Override
            public void onSuccess() {
                showSnackMsg(R.string.d4l_sdk_successful_logout);
                renderScreen(ScreenState.LoginVisible);
            }

            @Override
            public void onError(D4LException exception) {
                exception.printStackTrace();
                showSnackMsg(R.string.d4l_sdk_failed_logout);
            }
        });
    }

    private void renderScreen(ScreenState state) {
        this.screenState = state;

        runOnUiThread(() -> {
            switch (state) {
                case LoginVisible:
                    loginLogoutBTN.setText(R.string.d4l_sdk_login);
                    break;
                case LogoutVisible:
                    loginLogoutBTN.setText(R.string.d4l_sdk_logout);
                    break;
                default:
                    throw new RuntimeException(UNEXPECTED_CASE);
            }
        });
    }

    private void showSnackMsg(int resourceId) {
        Snackbar.make(rootCL, resourceId, Snackbar.LENGTH_SHORT).show();
    }
}
