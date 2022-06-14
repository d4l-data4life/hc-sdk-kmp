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

package care.data4life.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.material.snackbar.Snackbar;

import care.data4life.sdk.Data4LifeClient;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.listener.ResultListener;

import static care.data4life.sdk.Data4LifeClient.D4L_AUTH;

public class MainActivity extends Activity {

    private Data4LifeClient client;
    private Button mLoginBTN;
    private CoordinatorLayout mRootCL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            ProviderInstaller.installIfNeeded(this);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }


        client = Data4LifeClient.getInstance();

        client.isUserLoggedIn(new ResultListener<Boolean>() {
            @Override
            public void onSuccess(Boolean isLoggedIn) {
                if (isLoggedIn) {
                    loggedIn();
                }
            }

            @Override
            public void onError(D4LException exception) {
                exception.printStackTrace();
            }
        });

        mRootCL = findViewById(R.id.rootCL);
        mLoginBTN = findViewById(R.id.hcLoginBTN);
        mLoginBTN.setOnClickListener(view -> {
            Intent loginIntent = Data4LifeClient.getInstance().getLoginIntent(null);
            startActivityForResult(loginIntent, D4L_AUTH);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == D4L_AUTH) {
            if (resultCode == RESULT_OK) {
                loggedIn();
            } else if (data.getExtras() != null) {
                if (data.getExtras().containsKey("error")) {
                    Snackbar.make(mRootCL, "Failed to login with Data4Life", Snackbar.LENGTH_SHORT).show();
                } else if (data.getExtras().containsKey("canceled")) {
                    Snackbar.make(mRootCL, "User canceled auth request", Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void loggedIn() {
        startActivity(new Intent(this, DocumentsActivity.class));
    }
}
