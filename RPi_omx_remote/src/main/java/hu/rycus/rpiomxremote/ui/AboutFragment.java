package hu.rycus.rpiomxremote.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import hu.rycus.rpiomxremote.MainActivity;
import hu.rycus.rpiomxremote.R;

/**
 * Fragment displaying general info about the project
 * and acknowlegdments to the services used in it.
 *
 * <br/>
 * Created by Viktor Adam on 11/24/13.
 *
 * @author rycus
 */
public class AboutFragment extends Fragment {

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }
}