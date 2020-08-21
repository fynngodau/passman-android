/**
 *  Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @license GNU AGPL version 3 or any later version
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package es.wolfi.app.passman;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.gson.GsonBuilder;
import com.koushikdutta.async.future.FutureCallback;
import com.nextcloud.android.sso.AccountImporter;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.exceptions.AccountImportCancelledException;
import com.nextcloud.android.sso.exceptions.AndroidGetAccountsPermissionNotGranted;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;
import com.nextcloud.android.sso.ui.UiExceptionManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.wolfi.passman.API.Core;
import es.wolfi.passman.API.Vault;

public class LoginActivity extends AppCompatActivity {
    public final static String LOG_TAG = "LoginActivity";
    private static final String SSO_LOG_TAG = "SSO@LoginActivity";

    @BindView(R.id.protocol) Spinner input_protocol;
    @BindView(R.id.host) EditText input_host;
    @BindView(R.id.user) EditText input_user;
    @BindView(R.id.pass) EditText input_pass;
    @BindView(R.id.next) Button bt_next;

    SharedPreferences settings;
    SingleTon ton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            AccountImporter.pickNewAccount(this);
            Log.w(SSO_LOG_TAG, "try AccountImporter was successful");
        } catch (NextcloudFilesAppNotInstalledException e1) {
            //UiExceptionManager.showDialogForException(this, e1);
            Log.w(SSO_LOG_TAG, "Nextcloud app is not installed. Cannot choose account");
            //e1.printStackTrace();
            setContentView(R.layout.activity_login);
            ButterKnife.bind(this);

            settings = PreferenceManager.getDefaultSharedPreferences(this);
            ton = SingleTon.getTon();

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
        } catch (AndroidGetAccountsPermissionNotGranted e2) {
            AccountImporter.requestAndroidAccountPermissionsAndPickAccount(this);
        }

        //For testing only
        /*try {
            PackageManager pm = getPackageManager();
            try {
                for (ApplicationInfo p : pm.getInstalledApplications(0)){
                    Log.v(SSO_LOG_TAG, p.packageName);
                }
                pm.getPackageInfo("com.nextcloud.client", PackageManager.GET_ACTIVITIES);
            } catch (PackageManager.NameNotFoundException e) {
                //Log.v(SSO_LOG_TAG, e.getMessage());
            }

            AccountImporter.pickNewAccount(this);
            Log.e(SSO_LOG_TAG, "try AccountImporter was successful");
        } catch (NextcloudFilesAppNotInstalledException e1) {
            Log.e(SSO_LOG_TAG, "Nextcloud app is not installed. Cannot choose account");
            e1.printStackTrace();

        } catch (AndroidGetAccountsPermissionNotGranted e2) {
            AccountImporter.requestAndroidAccountPermissionsAndPickAccount(this);
        }*/
    }

    @OnClick(R.id.next)
    public void onNextClick() {
        Log.e("Login", "begin");
        final String protocol = input_protocol.getSelectedItem().toString().toLowerCase();
        final String host = protocol + "://" + input_host.getText().toString();
        final String user = input_user.getText().toString();
        final String pass = input_pass.getText().toString();

        final Activity c = this;

        ton.addString(SettingValues.HOST.toString(), host);
        ton.addString(SettingValues.USER.toString(), user);
        ton.addString(SettingValues.PASSWORD.toString(), pass);

        Core.checkLogin(this, true, new FutureCallback<Boolean>() {
            @Override
            public void onCompleted(Exception e, Boolean result) {
                if (result) {
                    settings.edit()
                            .putString(SettingValues.HOST.toString(), host)
                            .putString(SettingValues.USER.toString(), user)
                            .putString(SettingValues.PASSWORD.toString(), pass)
                            .apply();

                    ton.getCallback(CallbackNames.LOGIN.toString()).onTaskFinished();
                    c.finish();
                }
                else {
                    ton.removeString(SettingValues.HOST.toString());
                    ton.removeString(SettingValues.USER.toString());
                    ton.removeString(SettingValues.PASSWORD.toString());
                }

            }
        });
    }

    /**
     * Displays this activity
     * @param c
     * @param cb
     */
    public static void launch(Context c, ICallback cb) {
        SingleTon.getTon().addCallback(CallbackNames.LOGIN.toString(), cb);
        Intent i = new Intent(c, LoginActivity.class);
        c.startActivity(i);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            AccountImporter.onActivityResult(requestCode, resultCode, data, this, new AccountImporter.IAccountAccessGranted() {

                NextcloudAPI.ApiConnectedListener callback = new NextcloudAPI.ApiConnectedListener() {
                    @Override
                    public void onConnected() {
                        // ignore this one… see 5)
                    }

                    @Override
                    public void onError(Exception ex) {
                        // TODO handle errors
                    }
                };

                @Override
                public void accountAccessGranted(SingleSignOnAccount account) {
                    Context l_context = getApplicationContext();

                    // As this library supports multiple accounts we created some helper methods if you only want to use one.
                    // The following line stores the selected account as the "default" account which can be queried by using
                    // the SingleAccountHelper.getCurrentSingleSignOnAccount(context) method
                    SingleAccountHelper.setCurrentAccount(l_context, account.name);

                    // Get the "default" account
                    SingleSignOnAccount ssoAccount = null;
                    try {
                        ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(l_context);
                    } catch (NextcloudFilesAppAccountNotFoundException | NoCurrentAccountSelectedException e) {
                        UiExceptionManager.showDialogForException(l_context, e);
                    }

                    NextcloudAPI nextcloudAPI = new NextcloudAPI(l_context, ssoAccount, new GsonBuilder().create(), callback);

                    // TODO ... (see code in section 4 and below)
                }
            });
        } catch (AccountImportCancelledException e) {
            e.printStackTrace();
        }
    }
}
