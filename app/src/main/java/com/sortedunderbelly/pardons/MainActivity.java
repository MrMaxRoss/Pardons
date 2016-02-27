package com.sortedunderbelly.pardons;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.common.collect.Sets;
import com.sortedunderbelly.pardons.storage.FirebasePardonStorage;
import com.sortedunderbelly.pardons.storage.PardonStorage;
import com.sortedunderbelly.pardons.storage.PardonStorage.StorageSignInResult;

import java.io.IOException;
import java.util.Date;
import java.util.List;


public class MainActivity extends FragmentActivity implements PardonsUIListener,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String GOOGLE_PROVIDER = "google";
    private static final int RC_SIGN_IN = 9001;
    private static final int AUTH_REQUEST_CODE = 9002;

    private static boolean USE_AUTH = false;

    /* Begin Static State - does not change when we get a new instance of the MainActivity */
    private static PardonStorage storage;

    /* Data from the authenticated user */
    private static GoogleSignInAccount mGoogleSignInAccount;

    /* Client used to interact with Google APIs. */
    private static GoogleApiClient mGoogleApiClient;

    private static final PardonsUIListenerProvider pardonsUIListenerProvider = new PardonsUIListenerProvider() {
        @Override
        public PardonsUIListener get() {
            return uiListener;
        }
    };
    private static PardonsUIListener uiListener;
    /* End Static State */

    private TextView receivedPardonsText;
    private TextView sentPardonsText;
    SlidingTabsBasicFragment tabsFragment;

    /* A dialog that is presented until storage authentication is finished. */
    private ProgressDialog mAuthProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update the listener so that the new view objects receive updates
        uiListener = this;
        setContentView(R.layout.pardons_home);

        /* Load the Google login button */
        SignInButton mGoogleLoginButton = (SignInButton) findViewById(R.id.login_with_google);
        mGoogleLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSignInIntent();
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        if (storage == null) {
            // this seems like a bad idea, but how do I keep from reiniitializing Firebase
            // every time a new activity is created?
            storage = new FirebasePardonStorage(this, pardonsUIListenerProvider, USE_AUTH);
        }

        receivedPardonsText = (TextView) findViewById(R.id.receivedPardonsValTextView);
        sentPardonsText = (TextView) findViewById(R.id.sentPardonsValTextView);
        Button sendPardonsButton = (Button) findViewById(R.id.sendPardonButton);
        sendPardonsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new SendPardonDialogFragment();
                newFragment.show(getSupportFragmentManager(), "What is this?");
            }
        });

        Button requestPardonsButton = (Button) findViewById(R.id.requestPardonButton);
        requestPardonsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new RequestPardonDialogFragment();
                newFragment.show(getSupportFragmentManager(), "What is this?");
            }
        });

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            tabsFragment = new SlidingTabsBasicFragment();
            transaction.replace(R.id.tabbed_fragment, tabsFragment);
            transaction.commit();
        }
        updateUI(mGoogleSignInAccount);
    }

    private void startSignInIntent() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mGoogleSignInAccount != null) {
            return;
        }
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result.isSuccess(), result.getSignInAccount());
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    hideProgressDialog();
                    handleSignInResult(googleSignInResult.isSuccess(), googleSignInResult.getSignInAccount());
                }
            });
        }
    }

    private void showProgressDialog() {
        /* Setup the progress dialog that is displayed later when authenticating with storage */
        if (mAuthProgressDialog == null) {
            mAuthProgressDialog = new ProgressDialog(this);
            mAuthProgressDialog.setTitle("Loading");
            mAuthProgressDialog.setMessage("Authenticating ...");
            mAuthProgressDialog.setCancelable(false);
        }
        mAuthProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mAuthProgressDialog != null && mAuthProgressDialog.isShowing()) {
            mAuthProgressDialog.hide();
        }
    }

    /**
     * This method fires when any startActivityForResult finishes. The requestCode maps to
     * the value passed into startActivityForResult.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(result.isSuccess(), result.getSignInAccount());
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Login Result Canceled");
                if (!USE_AUTH) {
                    GoogleSignInAccount account = GoogleSignInAccount.zza("id", "token id", "max@sortedunderbelly.com", "display name", null, Long.MAX_VALUE, "obfuscated id", Sets.<Scope>newHashSet()).zzbI("serverauthcode");
                    handleSignInResult(true, account);
                }
            }
        } else if (requestCode == AUTH_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startSignInIntent();
            }
        }
    }

    private void handleSignInResult(boolean isSuccess, GoogleSignInAccount account) {
        Log.d(TAG, "handleSignInResult: " + isSuccess);
        if (isSuccess && account != null) {
            // Signed into Google, now sign into the storage service
            doStorageSignIn(account);
        } else {
            // Signed out, show unauthenticated UI.
            signOut();
        }
        onAuthStateChanged(account);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* If a user is currently authenticated, display a signOut menu */
        if (this.mGoogleSignInAccount != null) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            MenuItem item = menu.findItem(R.id.sign_out);
            item.setTitle(String.format("%s (%s)", getString(R.string.action_logout),
                    mGoogleSignInAccount.getEmail()));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sign_out) {
            signOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Unauthenticate from the storage system and from providers where necessary.
     */
    private void signOut() {
        if (mGoogleSignInAccount != null) {
            // Grab this now because when we signOut, mAuthData will become null
            storage.signOut();
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            updateUI(null);
                        }
                    });
            onAuthStateChanged(null);
        }
    }

    /**
     * Show errors to users
     */
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void refreshStats() {
        int totalReceivedPardons = calcPardonSum(storage.getReceivedPardons());
        receivedPardonsText.setText(String.format("%d", totalReceivedPardons));

        int totalSentPardons = calcPardonSum(storage.getSentPardons());
        sentPardonsText.setText(String.format("%d", totalSentPardons));
    }

    private int calcPardonSum(List<Pardons> pardons) {
        int total = 0;
        for (Pardons p : pardons) {
            total += p.getQuantity();
        }
        return total;
    }

    private void updateUI(GoogleSignInAccount acct) {
        if (acct != null) {
            findViewById(R.id.login_with_google).setVisibility(View.GONE);
            findViewById(R.id.gridLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.horz_line).setVisibility(View.VISIBLE);
            findViewById(R.id.tabbed_fragment).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.login_with_google).setVisibility(View.VISIBLE);
            findViewById(R.id.gridLayout).setVisibility(View.GONE);
            findViewById(R.id.horz_line).setVisibility(View.GONE);
            findViewById(R.id.tabbed_fragment).setVisibility(View.GONE);
        }
        refreshStats();
    }

    private void doStorageSignIn(final GoogleSignInAccount account) {
        storage.start(account);

        /* Get OAuth token in Background */
        AsyncTask<Void, Void, StorageSignInResult> task = new AsyncTask<Void, Void, StorageSignInResult>() {
            String errorMessage = null;

            @Override
            protected StorageSignInResult doInBackground(Void... params) {
                String token = null;

                try {
                    String scope = String.format("oauth2:%s", Scopes.PLUS_LOGIN);
                    token = GoogleAuthUtil.getToken(MainActivity.this, account.getEmail(), scope);
                } catch (IOException transientEx) {
                    /* Network or server error */
                    Log.e(TAG, "Error authenticating with Google: " + transientEx);
                    errorMessage = "Network error: " + transientEx.getMessage();
                } catch (UserRecoverableAuthException e) {
                    Log.w(TAG, "Recoverable Google OAuth error: " + e.toString());
                    /* We probably need to ask for permissions, so start the intent if there is none pending */
                    startActivityForResult(e.getIntent(), AUTH_REQUEST_CODE);
                } catch (GoogleAuthException authEx) {
                    /* The call is not ever expected to succeed assuming you have already verified that
                     * Google Play services is installed. */
                    Log.e(TAG, "Error authenticating with Google: " + authEx.getMessage(), authEx);
                    errorMessage = "Error authenticating with Google: " + authEx.getMessage();
                }
                return new StorageSignInResult(account, token);
            }

            @Override
            protected void onPostExecute(StorageSignInResult result) {
                if (result != null && result.getToken() != null) {
                    storage.authWithOAuthToken(GOOGLE_PROVIDER, result);
                }
            }
        };
        task.execute();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e(TAG, result.toString());
    }

    @Override
    public void onAddSentPardons(Pardons pardons) {
        updateStats(pardons.getQuantity(), sentPardonsText, SentPardonsFragment.class);
    }

    @Override
    public void onApprovePardonsRequest(Pardons pardonsRequest) {
        sendFragmentUpdate(PendingInboundRequestsPardonsFragment.class);
        updateStats(pardonsRequest.getQuantity(), sentPardonsText,
                SentPardonsFragment.class);
    }

    public void approvePardons(Pardons pardons) {
        storage.approvePardonsRequest(pardons, this);
        Toast.makeText(getApplicationContext(), R.string.acceptedRequestForPardonsText, Toast.LENGTH_SHORT).show();
    }

    public void sendPardonsToFriend(String recipient, String recipientDisplayName, int quantity, String reason) {
        Pardons pardons = new Pardons(mGoogleSignInAccount.getEmail(), mGoogleSignInAccount.getDisplayName(),
                recipient, recipientDisplayName, new Date(), quantity, reason);
        storage.addSentPardons(pardons, this);
        Toast.makeText(getApplicationContext(), R.string.pardonsSentText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDenyPardonsRequest(Pardons pardonsRequest) {
        sendFragmentUpdate(DeniedInboundRequestsPardonsFragment.class);
    }

    public void denyPardons(Pardons pardons) {
        storage.denyPardonsRequest(pardons, this);
        Toast.makeText(getApplicationContext(), R.string.deniedRequestForPardonsText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAddPardonsRequest(Pardons pardons) {
        sendFragmentUpdate(PendingOutboundRequestsPardonsFragment.class);
    }

    public void requestPardons(String recipient, String recipientDisplayName, int quantity,
                               String reason) {
        Pardons pardons = new Pardons(recipient, recipientDisplayName, mGoogleSignInAccount.getEmail(),
                mGoogleSignInAccount.getDisplayName(), new Date(), quantity, reason);
        // If approved, these pardons will come from your friend.
        storage.addPardonsRequest(pardons, this);
        Toast.makeText(getApplicationContext(), R.string.pardonsRequestedText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemovePardonsRequest(Pardons pardons) {
        updateStats(-pardons.getQuantity(), /* stat not displayed */ null,
                PendingOutboundRequestsPardonsFragment.class);
    }

    public void retractRequestForPardons(Pardons pardons) {
        storage.removePardonsRequest(pardons, this);
        Toast.makeText(getApplicationContext(), R.string.pardonsRetractedText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAddReceivedPardons(Pardons pardons) {
        // not exposed in the user's own UI - only triggered via storage
        updateStats(pardons.getQuantity(), receivedPardonsText,
                ReceivedPardonsFragment.class);
    }

    @Override
    public void onChangePendingOutboundPardonsRequests() {
        sendFragmentUpdate(PendingOutboundRequestsPardonsFragment.class);
    }

    @Override
    public void onChangePendingInboundPardonsRequests() {
        sendFragmentUpdate(PendingInboundRequestsPardonsFragment.class);
    }

    @Override
    public void onChangeDeniedOutboundPardonsRequests() {
        sendFragmentUpdate(DeniedOutboundRequestsPardonsFragment.class);
    }

    @Override
    public void onChangeDeniedInboundPardonsRequests() {
        sendFragmentUpdate(DeniedInboundRequestsPardonsFragment.class);
    }

    private int textToInt(TextView textView) {
        return Integer.parseInt(textView.getText().toString());
    }

    public PardonStorage getStorage() {
        return storage;
    }


    private void updateStats(int pardonsDelta, TextView textView, Class<?> intentActionClass) {
        if (textView != null) {
            int newPardonsTotal = textToInt(textView) + pardonsDelta;
            textView.setText(String.format("%d", newPardonsTotal));
        }
        sendFragmentUpdate(intentActionClass);
    }

    private void sendFragmentUpdate(Class<?> intentActionClass) {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.sendBroadcast(new Intent(intentActionClass.getName()));
        lbm.sendBroadcast(new Intent(SlidingTabLayout.UPDATE_TAB_TITLES_INTENT_ACTION));
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.pardons_home, container, false);
        }
    }

    private void onAuthStateChanged(GoogleSignInAccount account) {
        if (account != null && !account.equals(mGoogleSignInAccount)) {
            supportInvalidateOptionsMenu();
        } else if (mGoogleSignInAccount != null && !mGoogleSignInAccount.equals(account)) {
            supportInvalidateOptionsMenu();
        }

        mGoogleSignInAccount = account;
        updateUI(mGoogleSignInAccount);
    }

    @Override
    public void onStorageAuthenticationError(String errorStr, String token) {
        hideProgressDialog();
        showErrorDialog(errorStr);
        // Try to invalidate the oauth token we received
        // TODO(max.ross) Invalidate the token and try logging in again. Only show the error dialog
        // if we fail a second time.
        GoogleAuthUtil.invalidateToken(this, token);
    }
}
