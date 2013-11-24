package hu.rycus.rpiomxremote.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import hu.rycus.rpiomxremote.MainActivity;
import hu.rycus.rpiomxremote.R;
import hu.rycus.rpiomxremote.RemoteServiceCreator;
import hu.rycus.rpiomxremote.blocks.Setting;
import hu.rycus.rpiomxremote.util.Intents;

/**
 * Created by rycus on 11/21/13.
 */
public class SettingsFragment extends Fragment {

    private final RemoteServiceCreator rsc = new RemoteServiceCreator();

    @Override
    public void onStart() {
        super.onStart();
        rsc.bind(getActivity());
    }

    @Override
    public void onStop() {
        rsc.unbind(getActivity());
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_settings, container, false);
        ViewGroup root = (ViewGroup) layout.findViewById(R.id.settings_container);

        Setting[] settings = (Setting[]) getArguments().getParcelableArray(Intents.EXTRA_SETTINGS_LIST);
        for(int index = 0; index < settings.length; index++) {
            if(index > 0) {
                inflater.inflate(R.layout.setting_divider, root);
            }

            Setting setting = settings[index];
            addSetting(setting, root);
        }

        return layout;
    }

    private void addSetting(Setting setting, ViewGroup root) {
        switch (setting.getType()) {
            case TEXT:
            {
                View view = View.inflate(root.getContext(), R.layout.setting_text_or_enum, null);
                setupText(view, setting);
                root.addView(view);
                break;
            }
            case ENUM:
            {
                View view = View.inflate(root.getContext(), R.layout.setting_text_or_enum, null);
                setupEnum(view, setting);
                root.addView(view);
                break;
            }
            case NUMBER:
            {
                View view = View.inflate(root.getContext(), R.layout.setting_number, null);
                setupNumber(view, setting);
                root.addView(view);
                break;
            }
            case SWITCH:
            {
                View view = View.inflate(root.getContext(), R.layout.setting_switch, null);
                setupSwitch(view, setting);
                root.addView(view);
                break;
            }
        }
    }

    private void setupText(View view, final Setting setting) {
        final TextView txtTitle = (TextView) view.findViewById(R.id.setting_title);
        final TextView txtValue = (TextView) view.findViewById(R.id.setting_value);

        final String key = setting.getKey();
        final String value = setting.getValue();
        final String description = setting.getDescription();

        txtTitle.setText(description);
        txtValue.setText(value);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle(description);

                final EditText input = new EditText(v.getContext());
                input.setText(setting.getValue());
                input.selectAll();
                input.requestFocus();
                builder.setView(input);

                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newValue = input.getText().toString().trim();
                        if(rsc.isServiceBound()) {
                            rsc.getService().setSetting(key, newValue);
                            txtValue.setText(newValue);
                            setting.setValue(newValue);
                        }
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        InputMethodManager manager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        manager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
                dialog.show();
            }
        });
    }

    private void setupEnum(View view, final Setting setting) {
        final TextView txtTitle = (TextView) view.findViewById(R.id.setting_title);
        final TextView txtValue = (TextView) view.findViewById(R.id.setting_value);

        final String key = setting.getKey();
        final String value = setting.getValue();
        final String[] values = setting.getPossibleValues().split(",");
        final String description = setting.getDescription();

        txtTitle.setText(description);
        txtValue.setText(value);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle(description);

                int selectedItem = 0;
                String currentValue = setting.getValue();

                for(int idx = 0; idx < values.length; idx++) {
                    if(values[idx].equals(currentValue)) {
                        selectedItem = idx;
                        break;
                    }
                }

                builder.setSingleChoiceItems(values, selectedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newValue = values[which];
                        if (rsc.isServiceBound()) {
                            rsc.getService().setSetting(key, newValue);
                            txtValue.setText(newValue);
                            setting.setValue(newValue);
                        }
                        dialog.dismiss();
                    }
                });

                builder.show();
            }
        });
    }

    private void setupNumber(View view, final Setting setting) {
        final TextView txtTitle = (TextView) view.findViewById(R.id.setting_title);
        final TextView txtValue = (TextView) view.findViewById(R.id.setting_value);

        final String key = setting.getKey();
        final String value = setting.getValue();
        final String description = setting.getDescription();

        txtTitle.setText(description);
        txtValue.setText(value);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle(description);

                final EditText input = new EditText(v.getContext());
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setText(setting.getValue());
                input.selectAll();
                builder.setView(input);

                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newValue = input.getText().toString().trim();
                        if(rsc.isServiceBound()) {
                            rsc.getService().setSetting(key, newValue);
                            txtValue.setText(newValue);
                            setting.setValue(newValue);
                        }
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        InputMethodManager manager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        manager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
                dialog.show();
            }
        });
    }

    private void setupSwitch(View view, final Setting setting) {
        final TextView txtTitle = (TextView) view.findViewById(R.id.setting_title);
        final CheckBox ckBoxValue = (CheckBox) view.findViewById(R.id.setting_value_ckbox);

        if(ckBoxValue.getId() == R.id.setting_value_ckbox) {
            ckBoxValue.setId(ckBoxValue.getId() + setting.getKey().hashCode());
        }

        txtTitle.setText(setting.getDescription());
        ckBoxValue.setChecked("1".equals(setting.getValue()));

        ckBoxValue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String newValue = isChecked ? "1" : "0";
                if(rsc.isServiceBound()) {
                    rsc.getService().setSetting(setting.getKey(), newValue);
                    setting.setValue(newValue);
                }
            }
        });
    }

}