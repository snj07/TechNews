package com.snj.technews;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.snj.technews.database.AppDatabase;
import com.snj.technews.database.AppDatabaseSingleton;
import com.snj.technews.model.User;
import com.snj.technews.utils.CheckNet;
import com.snj.technews.utils.SharedPrefConfigUtils;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;
    private ProgressDialog mProgressDialog;


    @BindView(R.id.gmail_signin_button2)
    Button btnGmailSignIn;
    @BindView(R.id.facebook_button)
    Button facebookBtn;
    private AppDatabase appDatabase;
    private Context context;
    private FirebaseAuth mAuth;
    private SharedPreferences sp;
    private static final int RC_SIGN_IN = 9001;

    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        context = this;
        sp = getSharedPreferences(SharedPrefConfigUtils.MY_PREFERENCES, Context.MODE_PRIVATE);


        if (!sp.getString(SharedPrefConfigUtils.USER_NAME, "").equals("")) {
            startActivity(new Intent(getApplicationContext(), MainArticlesListActivity.class));
            finish();
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        mGoogleApiClient.connect();
        mAuth = FirebaseAuth.getInstance();
        facebookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CheckNet.isOnline(getApplicationContext())) {
                    fbLogin();
                } else {

                    showSnackbar(getResources().getString(R.string.no_internet));

                }

            }
        });
        btnGmailSignIn.setOnClickListener(new View.OnClickListener() {
                                              @Override
                                              public void onClick(View view) {
                                                  submit();
                                              }
                                          }
        );

    }


    void submit() {
        if (CheckNet.isOnline(getApplicationContext())) {
            signIn();
        } else {
            showSnackbar(getResources().getString(R.string.no_internet));

        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
        mGoogleApiClient.connect();
    }


    private void signOutGoogle() {

        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {

                    }
                });
    }

    public void fbLogin() {

        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        //  LoginManager.getInstance().logInWithPublishPermissions(this, Arrays.asList("publish_actions"));
        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {

                        Log.d(TAG, "facebook:onSuccess:" + loginResult);
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "facebook:onCancel");
                        // ...
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.d(TAG, "facebook:onError", error);
                        showSnackbar(getResources().getString(R.string.auth_failed));
                    }
                });
    }

    private void showSnackbar(String msg){
        View parentLayout = findViewById(android.R.id.content);
        Snackbar.make(parentLayout, msg, Snackbar.LENGTH_LONG)
                .setAction(getResources().getText(R.string.close), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                })
                .setActionTextColor(getResources().getColor(android.R.color.holo_red_light ))
                .show();
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            handleSignInResult(user.getDisplayName(), user.getEmail(), user.getPhotoUrl().toString());

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.d(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, getResources().getText(R.string.auth_failed),
                                    Toast.LENGTH_SHORT).show();
                            //  updateUI(null);*/
                        }


                    }
                });
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed: --- " + connectionResult);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
            if (requestCode == RC_SIGN_IN) {

                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleGoogle(result);
                Log.d(TAG, "onConnectionFailed: --- " + result.getStatus().toString());
            } else {
                mCallbackManager.onActivityResult(requestCode, resultCode, data);
            }
        } catch (Exception e) {

            showSnackbar("Error in login!");

            Log.d("error", e.getLocalizedMessage());
        }
    }


    private void handleGoogle(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();

            Log.e(TAG, "display name: " + acct.getDisplayName());

            String name = acct.getDisplayName();
            String email = acct.getEmail();
            String personPhotoUrl = "";
            if (acct.getPhotoUrl() != null)
                personPhotoUrl = acct.getPhotoUrl().toString();
            handleSignInResult(name, email, personPhotoUrl);


        } else {
            // Signed out, show unauthenticated UI.
            showSnackbar(getResources().getString(R.string.login_failed));
        }
    }

    void handleSignInResult(final String personName, final String email, final String personPhotoUrl) {

        Log.e(TAG, "Name: " + personName + ", email: " + email
                + ", Image: " + personPhotoUrl);
        final User user = new User();
        user.setUsername(personName);
        user.setEmail(email);
        user.setImage_url(personPhotoUrl);
        user.setPassword("");

        appDatabase = AppDatabaseSingleton.getInstance(this);


        new Thread(new Runnable() {
            @Override
            public void run() {
                User user = new User();
                user.setUsername(personName);
                user.setEmail(email);
                user.setImage_url(personPhotoUrl);
                user.setPassword("");
                appDatabase.userDao().insertAll(user);
            }
        }).start();
        SharedPreferences.Editor et = sp.edit();
        final User[] user1 = {null};
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                user1[0] = appDatabase.userDao().findByEmailId(email);
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (user1[0] != null) {
            et.putLong(SharedPrefConfigUtils.USER_ID, user1[0].getUid());
            et.putString(SharedPrefConfigUtils.USER_NAME, user1[0].getUsername());
            et.putString(SharedPrefConfigUtils.USER_EMAIL, user1[0].getEmail());
            et.putString(SharedPrefConfigUtils.USER_IMAGE, user1[0].getImage_url());
            Log.d(TAG, user1[0].getEmail() + "");
            et.commit();
            startActivity(new Intent(context, MainArticlesListActivity.class));
            finish();
        }
    }


    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getResources().getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

}
