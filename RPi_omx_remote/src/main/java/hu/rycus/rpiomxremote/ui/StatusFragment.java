package hu.rycus.rpiomxremote.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import hu.rycus.rpiomxremote.MainActivity;
import hu.rycus.rpiomxremote.R;
import hu.rycus.rpiomxremote.RemoteService;
import hu.rycus.rpiomxremote.RemoteServiceCreator;

/**
 * Created by rycus on 11/10/13.
 */
public class StatusFragment extends Fragment {

    private ProgressBar loadingProgress;
    private View        statusHeader;
    private TextView    txtStatus;
    private Button      btnBrowse;
    private Button      btnSettings;

    private final RemoteServiceCreator rsc = new RemoteServiceCreator() {
        @Override
        protected void onServiceInstanceReceived(RemoteService service) {
            setConnected(service != null && service.isConnected());
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        rsc.bind(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
        rsc.unbind(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    public void setConnected(boolean connected) {
        if(loadingProgress != null) {
            loadingProgress.setVisibility(connected ? View.GONE : View.VISIBLE);
        }

        statusHeader.setBackgroundColor(
                getResources().getColor(
                        connected ?
                                R.color.status_online :
                                R.color.status_offline));
        txtStatus.setText(
                connected ?
                        R.string.status_online :
                        R.string.status_offline);
        txtStatus.setCompoundDrawablesWithIntrinsicBounds(
                connected ?
                        R.drawable.ic_action_accept :
                        R.drawable.ic_action_navigation_cancel,
                0, 0, 0);
        btnBrowse.setEnabled(connected);
        btnSettings.setEnabled(connected);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_status, container, false);

        loadingProgress = (ProgressBar) root.findViewById(R.id.status_loading_progress);
        statusHeader    =               root.findViewById(R.id.container_status_header);
        txtStatus       = (TextView)    root.findViewById(R.id.txt_status);
        btnBrowse       = (Button)      root.findViewById(R.id.status_btn_browse);
        btnSettings     = (Button)      root.findViewById(R.id.status_btn_settings);

        btnBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rsc.isServiceBound()) {
                    rsc.getService().requestFileList(null);
                }
            }
        });
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rsc.isServiceBound()) {
                    rsc.getService().requestSettings();
                }
            }
        });

        setConnected(rsc.isServiceBound() && rsc.getService().isConnected());

        return root;
    }
}