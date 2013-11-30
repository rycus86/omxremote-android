package hu.rycus.rpiomxremote.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;

import hu.rycus.rpiomxremote.MainActivity;
import hu.rycus.rpiomxremote.R;
import hu.rycus.rpiomxremote.RemoteServiceCreator;
import hu.rycus.rpiomxremote.blocks.FileList;
import hu.rycus.rpiomxremote.util.Constants;
import hu.rycus.rpiomxremote.util.Intents;

/**
 * Fragment displaying a remote directory and its contents.
 * Also the user can select the files to start here.
 *
 * <br/>
 * Created by Viktor Adam on 11/7/13.
 *
 * @author rycus
 */
public class FileListFragment extends Fragment {

    /** Intent extra key for a FileList object. */
    private static final String EXTRA_FILES             = "x$files";
    /** Intent extra key for a video file path. */
    private static final String EXTRA_SELECTED_VIDEO    = "x$sel_video";
    /** Intent extra key for a subtitle file path. */
    private static final String EXTRA_SELECTED_SUBTITLE = "x$sel_subtitle";

    /** The list adapter instance displaying the files. */
    private final FileListAdapter fileListAdapter = new FileListAdapter();

    /** The root view of the fragment. */
    private View            rootView;
    /** Text view displaying the current path. */
    private TextView        txtCurrentPath;
    /** List view displaying the contents of the current directory. */
    private ListView        fileList;
    /** View containing views displaying the selected files. */
    private View            selectionContainer;
    /** Text view displaying a header on the selection panel. */
    private TextView        selectionHeader;
    /** View group for the selected video. */
    private LinearLayout    selectedVideoContainer;
    /** Text view displaying the selected video. */
    private TextView        txtSelectedVideo;
    /** Button to remove the selected video. */
    private ImageButton     btnRemoveSelectedVideo;
    /** View group for the selected subtitle. */
    private LinearLayout    selectedSubtitleContainer;
    /** Text view displaying the selected subtitle. */
    private TextView        txtSelectedSubtitle;
    /** Button to remove the selected subtitle. */
    private ImageButton     btnRemoveSelectedSubtitle;
    /** Text view informing that playback is starting. */
    private TextView        txtSelectionStarting;
    /** Progress bar informing that playback is starting. */
    private ProgressBar     prgSelectionStarting;
    /** Button to start playback with the selected files. */
    private Button          btnSelectionStart;

    /** The FileList object containing the current path and the contents of it. */
    private FileList    current             = null;
    /** The currently selected video file. */
    private String      selectedVideo       = null;
    /** The currently selected subtitle file. */
    private String      selectedSubtitle    = null;

    /** Helper object to bind/unbind the remote service. */
    private final RemoteServiceCreator rsc = new RemoteServiceCreator();

    /** @see android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle) */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(current          != null) outState.putParcelable(EXTRA_FILES, current);
        if(selectedVideo    != null) outState.putString(EXTRA_SELECTED_VIDEO, selectedVideo);
        if(selectedSubtitle != null) outState.putString(EXTRA_SELECTED_SUBTITLE, selectedSubtitle);
    }

    /** @see android.support.v4.app.Fragment#onStart() */
    @Override
    public void onStart() {
        super.onStart();
        rsc.bind(getActivity());
    }

    /** @see android.support.v4.app.Fragment#onStop() */
    @Override
    public void onStop() {
        super.onStop();
        rsc.unbind(getActivity());
    }

    /** @see android.support.v4.app.Fragment#onPause() */
    @Override
    public void onPause() {
        super.onPause();

        selectionHeader.setVisibility(View.VISIBLE);
        txtSelectionStarting.setVisibility(View.GONE);
        prgSelectionStarting.setVisibility(View.GONE);
    }

    /** @see android.support.v4.app.Fragment#onResume() */
    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /** Convert density independent pixels to actual pixels. */
    private float pxFromDp(float dp)
    {
        return dp * getActivity().getResources().getDisplayMetrics().density;
    }

    /** Change the current selection and update the panel height with animation. */
    private void selectAnimated(Runnable selection) {
        selectionContainer.measure(0, 0);
        int startHeight = selectionContainer.getMeasuredHeight();

        selection.run();

        selectionContainer.measure(0, 0);

        int height  = selectionContainer.getMeasuredHeight();

        final RelativeLayout.LayoutParams lpSelection = (RelativeLayout.LayoutParams) selectionContainer.getLayoutParams();

        int diff = startHeight - height;
        if(diff != 0) {
            lpSelection.setMargins(
                    lpSelection.leftMargin,
                    lpSelection.topMargin,
                    lpSelection.rightMargin,
                    lpSelection.bottomMargin + diff);
            rootView.requestLayout();
        }

        final int startMargin = lpSelection.bottomMargin;

        int bottomTarget = 0; // (int) (selectedVideo != null || selectedSubtitle != null ? pxFromDp(5) : pxFromDp(-8));
        final int change      = -startMargin + bottomTarget;

        Animation animation = new Animation() {
            @Override protected void applyTransformation(float interpolatedTime, Transformation t) {
                int current = (int) ( change * interpolatedTime);
                lpSelection.setMargins(
                        lpSelection.leftMargin,
                        lpSelection.topMargin,
                        lpSelection.rightMargin,
                        startMargin + current);
                rootView.requestLayout();
            }
        };
        animation.setDuration(500);
        animation.setInterpolator(new DecelerateInterpolator());
        rootView.startAnimation(animation);
    }

    /** Change root path to the given directory. */
    private void changeToDir(String directory) {
        String parent = current != null ? current.getPath() : ".";
        String path = new File(parent, directory).getPath();
        txtCurrentPath.setText(path);

        if(rsc.isServiceBound()) {
            rsc.getService().requestFileList(path);
        }
    }

    /** Processes user selection of a list item. */
    private void selectItem(final String item) {
        selectionHeader.setVisibility(View.VISIBLE);
        txtSelectionStarting.setVisibility(View.GONE);
        prgSelectionStarting.setVisibility(View.GONE);

        if(!item.endsWith("/")) {
            if(item.contains(".")) { // probably a file
                selectAnimated(new Runnable() {
                    @Override
                    public void run() {
                        selectionContainer.measure(0, 0);
                        int startHeight = selectionContainer.getMeasuredHeight();

                        String extension = item.substring(item.lastIndexOf('.') + 1);
                        if(Constants.Extensions.isVideo(extension)) {
                            selectVideo(item, true);
                        } else if(Constants.Extensions.isSubtitle(extension)) {
                            selectSubtitle(item, true);
                        }
                    }
                });
            }
        } else {
            // directory
            changeToDir(item);
        }
    }

    /** Process video file selection. */
    private void selectVideo(String item, boolean updateSelection) {
        if(updateSelection) {
            selectedVideo = new File(current.getPath(), item).getPath();
        }

        selectionHeader.setText(getResources().getText(R.string.fl_selection_header));
        txtSelectedVideo.setText(item);
        selectedVideoContainer.setVisibility(View.VISIBLE);
        btnSelectionStart.setVisibility(View.VISIBLE);
    }

    /** Process subtitle file selection. */
    private void selectSubtitle(String item, boolean updateSelection) {
        if(updateSelection) {
            selectedSubtitle = new File(current.getPath(), item).getPath();
        }

        selectionHeader.setText(getResources().getText(R.string.fl_selection_header));
        txtSelectedSubtitle.setText(item);
        selectedSubtitleContainer.setVisibility(View.VISIBLE);
    }

    /** Removes video file selection. */
    private void removeVideoSelection() {
        selectedVideo = null;

        selectAnimated(new Runnable() {
            @Override public void run() {
                txtSelectedVideo.setText(getResources().getText(R.string.fl_selection_no_video));
                selectedVideoContainer.setVisibility(View.GONE);
                btnSelectionStart.setVisibility(View.GONE);

                if(selectedSubtitle == null) {
                    selectionHeader.setText(getResources().getText(R.string.fl_selection_hint));
                }
            }
        });
    }

    /** Removes subtitle file selection. */
    private void removeSubtitleSelection() {
        selectedSubtitle = null;

        selectAnimated(new Runnable() {
            @Override public void run() {
                txtSelectedSubtitle.setText(getResources().getText(R.string.fl_selection_no_subtitle));
                selectedSubtitleContainer.setVisibility(View.GONE);

                if(selectedVideo == null) {
                    selectionHeader.setText(getResources().getText(R.string.fl_selection_hint));
                }
            }
        });
    }

    /** This executes when the user requests to start a video. */
    private void onStartVideo() {
        removeVideoSelection();
        removeSubtitleSelection();

        selectAnimated(new Runnable() {
            @Override
            public void run() {
                selectionHeader.setVisibility(View.GONE);
                txtSelectionStarting.setVisibility(View.VISIBLE);
                prgSelectionStarting.setVisibility(View.VISIBLE);
            }
        });
    }

    /** Find a View in the selection container with the given ID. */
    private <T> T findSC(int id) {
        return (T) selectionContainer.findViewById(id);
    }

    /**
     * @see android.support.v4.app.Fragment
     *      #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_filelist, container, false);

        fileList = (ListView) rootView.findViewById(R.id.test_file_list);
        fileList.setAdapter(fileListAdapter);
        txtCurrentPath          = (TextView) rootView.findViewById(R.id.txt_current_path);

        selectionContainer      = rootView.findViewById(R.id.selection_container);

        selectionHeader             = findSC(R.id.txt_selection_header);
        selectedVideoContainer      = findSC(R.id.selected_video_container);
        txtSelectedVideo            = findSC(R.id.txt_selected_video);
        btnRemoveSelectedVideo      = findSC(R.id.btn_remove_selected_video);
        selectedSubtitleContainer   = findSC(R.id.selected_subtitle_container);
        txtSelectedSubtitle         = findSC(R.id.txt_selected_subtitle);
        btnRemoveSelectedSubtitle   = findSC(R.id.btn_remove_selected_subtitle);
        btnSelectionStart           = findSC(R.id.btn_selection_start);
        txtSelectionStarting        = findSC(R.id.txt_selection_starting);
        prgSelectionStarting        = findSC(R.id.selection_starting_progress);

        fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem((String) parent.getItemAtPosition(position));
            }
        });
        btnRemoveSelectedVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeVideoSelection();
            }
        });
        btnRemoveSelectedSubtitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeSubtitleSelection();
            }
        });

        btnSelectionStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rsc.isServiceBound()) {
                    rsc.getService().startPlayer(selectedVideo, selectedSubtitle);

                    onStartVideo();
                }
            }
        });

        if(savedInstanceState != null) {
            if(savedInstanceState.containsKey(EXTRA_FILES)) {
                FileList files = (FileList) savedInstanceState.getParcelable(EXTRA_FILES);
                setFiles(files);
            }

            selectedVideo = savedInstanceState.getString(EXTRA_SELECTED_VIDEO);
            if(selectedVideo != null) {
                String fname = selectedVideo;
                if(fname.contains("/")) {
                    fname = fname.substring(fname.lastIndexOf('/') + 1);
                }
                selectVideo(fname, false);
            }

            selectedSubtitle = savedInstanceState.getString(EXTRA_SELECTED_SUBTITLE);
            if(selectedSubtitle != null) {
                String fname = selectedSubtitle;
                if(fname.contains("/")) {
                    fname = fname.substring(fname.lastIndexOf('/') + 1);
                }
                selectSubtitle(fname, false);
            }
        }

        if(selectedVideo != null || selectedSubtitle != null) {
            RelativeLayout.LayoutParams lpSelection = (RelativeLayout.LayoutParams) selectionContainer.getLayoutParams();
            lpSelection.setMargins(
                    lpSelection.leftMargin,
                    lpSelection.topMargin,
                    lpSelection.rightMargin,
                    (int) pxFromDp(5));
        }

        Bundle args = getArguments();
        if(args != null) {
            FileList files = args.getParcelable(Intents.EXTRA_FILE_LIST);
            if(files != null) {
                setFiles(files);
            }
        }

        return rootView;
    }

    /** Sets the file list to display. */
    public void setFiles(FileList files) {
        current = files;

        txtCurrentPath.setText(files.getPath());
        txtCurrentPath.setVisibility(View.VISIBLE);

        fileList.scrollTo(0, 0);
        fileListAdapter.setItems(files);
    }

}