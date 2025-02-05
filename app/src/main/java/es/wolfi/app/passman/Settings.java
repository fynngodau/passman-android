/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @license GNU AGPL version 3 or any later version
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.wolfi.app.passman;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.koushikdutta.async.future.FutureCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.wolfi.passman.API.Vault;


public class Settings extends Fragment {

    @BindView(R.id.settings_nextcloud_url)
    EditText settings_nextcloud_url;
    @BindView(R.id.settings_nextcloud_user)
    EditText settings_nextcloud_user;
    @BindView(R.id.settings_nextcloud_password)
    EditText settings_nextcloud_password;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    @BindView(R.id.settings_app_start_password_switch)
    Switch settings_app_start_password_switch;

    @BindView(R.id.default_autofill_vault_title)
    TextView default_autofill_vault_title;
    @BindView(R.id.default_autofill_vault)
    Spinner default_autofill_vault;

    SharedPreferences settings;

    public Settings() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment Settings.
     */
    public static Settings newInstance() {
        Settings fragment = new Settings();

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Button settingsSaveButton = (Button) view.findViewById(R.id.settings_save_button);
        settingsSaveButton.setOnClickListener(this.getSaveButtonListener());
        settingsSaveButton.setVisibility(View.VISIBLE);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        settings_nextcloud_url.setText(settings.getString(SettingValues.HOST.toString(), null));
        settings_nextcloud_user.setText(settings.getString(SettingValues.USER.toString(), null));
        settings_nextcloud_password.setText(settings.getString(SettingValues.PASSWORD.toString(), null));
        settings_app_start_password_switch.setChecked(settings.getBoolean(SettingValues.ENABLE_APP_START_DEVICE_PASSWORD.toString(), false));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String last_selected_guid = "";
            if (settings.getString(SettingValues.AUTOFILL_VAULT_GUID.toString(), null) != null) {
                last_selected_guid = settings.getString(SettingValues.AUTOFILL_VAULT_GUID.toString(), null);
            }
            Set<Map.Entry<String, Vault>> vaults = getVaultsEntrySet();
            String[] vault_names = new String[vaults.size() + 1];
            vault_names[0] = getContext().getString(R.string.automatically);
            int i = 1;
            int selection_id = 0;
            for (Map.Entry<String, Vault> vault_entry : vaults) {
                if (last_selected_guid.equals(vault_entry.getValue().guid)) {
                    selection_id = i;
                }
                vault_names[i] = vault_entry.getValue().name;
                i++;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, vault_names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            default_autofill_vault.setAdapter(adapter);
            default_autofill_vault.setSelection(selection_id);
        } else {
            ((ViewManager)default_autofill_vault.getParent()).removeView(default_autofill_vault);
            ((ViewManager)default_autofill_vault_title.getParent()).removeView(default_autofill_vault_title);
        }
    }

    private Set<Map.Entry<String, Vault>> getVaultsEntrySet() {
        HashMap<String, Vault> vaults = (HashMap<String, Vault>) SingleTon.getTon().getExtra(SettingValues.VAULTS.toString());
        return vaults.entrySet();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public View.OnClickListener getSaveButtonListener() {
        return new View.OnClickListener() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onClick(View view) {
                SingleTon ton = SingleTon.getTon();

                settings.edit().putBoolean(SettingValues.ENABLE_APP_START_DEVICE_PASSWORD.toString(), settings_app_start_password_switch.isChecked()).commit();

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    if (default_autofill_vault.getSelectedItem().toString().equals(getContext().getString(R.string.automatically))) {
                        ton.removeExtra(SettingValues.AUTOFILL_VAULT_GUID.toString());
                        settings.edit().putString(SettingValues.AUTOFILL_VAULT_GUID.toString(), "").commit();
                    } else {
                        Set<Map.Entry<String, Vault>> vaults = getVaultsEntrySet();
                        for (Map.Entry<String, Vault> vault_entry : vaults) {
                            if (vault_entry.getValue().name.equals(default_autofill_vault.getSelectedItem().toString())) {
                                ton.addExtra(SettingValues.AUTOFILL_VAULT_GUID.toString(), vault_entry.getValue().guid);
                                settings.edit().putString(SettingValues.AUTOFILL_VAULT_GUID.toString(), vault_entry.getValue().guid).commit();

                                Vault.getVault(getContext(), vault_entry.getValue().guid, new FutureCallback<Vault>() {
                                    @Override
                                    public void onCompleted(Exception e, Vault result) {
                                        if (e != null) {
                                            return;
                                        }
                                        Vault.updateAutofillVault(result, settings);
                                    }
                                });

                                break;
                            }
                        }
                    }
                }

                if (!settings.getString(SettingValues.HOST.toString(), null).equals(settings_nextcloud_url.getText().toString()) ||
                        !settings.getString(SettingValues.USER.toString(), null).equals(settings_nextcloud_user.getText().toString()) ||
                        !settings.getString(SettingValues.PASSWORD.toString(), null).equals(settings_nextcloud_password.getText().toString())) {
                    ton.removeString(SettingValues.HOST.toString());
                    ton.removeString(SettingValues.USER.toString());
                    ton.removeString(SettingValues.PASSWORD.toString());

                    ton.addString(SettingValues.HOST.toString(), settings_nextcloud_url.getText().toString());
                    ton.addString(SettingValues.USER.toString(), settings_nextcloud_user.getText().toString());
                    ton.addString(SettingValues.PASSWORD.toString(), settings_nextcloud_password.getText().toString());

                    settings.edit().putString(SettingValues.HOST.toString(), settings_nextcloud_url.getText().toString()).commit();
                    settings.edit().putString(SettingValues.USER.toString(), settings_nextcloud_user.getText().toString()).commit();
                    settings.edit().putString(SettingValues.PASSWORD.toString(), settings_nextcloud_password.getText().toString()).commit();

                    Objects.requireNonNull(((PasswordListActivity) getActivity())).applyNewSettings(true);
                } else {
                    Objects.requireNonNull(((PasswordListActivity) getActivity())).applyNewSettings(false);
                }
            }
        };
    }
}
