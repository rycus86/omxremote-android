package hu.rycus.rpiomxremote.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import hu.rycus.rpiomxremote.R;
import hu.rycus.rpiomxremote.RemoteService;
import hu.rycus.rpiomxremote.RemoteServiceCreator;
import hu.rycus.rpiomxremote.blocks.SubtitleItem;
import hu.rycus.rpiomxremote.blocks.SubtitleMetadata;
import hu.rycus.rpiomxremote.manager.v2.SubtitleDownloadCallback;
import hu.rycus.rpiomxremote.manager.v2.SubtitleMetadataCallback;
import hu.rycus.rpiomxremote.manager.v2.SubtitleQueryCallback;

public class SubtitleDialogFragment extends DialogFragment {

    public static final int REQUEST_SUBTITLES = 0x12;

    public static final String RESULT_DIRECTORY = "directory";

    private static final String EXTRA_FILENAME = "filename";
    private static final String EXTRA_DIRECTORY = "directory";

    /** Helper object to bind/unbind the remote service. */
    private final RemoteServiceCreator rsc = new RemoteServiceCreator() {
        @Override
        protected void onServiceInstanceReceived(final RemoteService service) {
            startMetadataLoading(service);
        }
    };

    private TextView txtInfo;
    private EditText editQuery;
    private ImageButton btnSearch;
    private ProgressBar progressBar;
    private ViewPager viewPager;

    private SubtitlePagerAdapter pagerAdapter;

    private final Map<String, ItemListAdapter> listAdapters = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        getDialog().setTitle(R.string.sub_title);

        final View view = inflater.inflate(R.layout.fragment_subtitle_dialog, null);

        txtInfo = (TextView) view.findViewById(R.id.sub_txt_info);
        editQuery = (EditText) view.findViewById(R.id.sub_edit_query);
        btnSearch = (ImageButton) view.findViewById(R.id.sub_btn_search);
        progressBar = (ProgressBar) view.findViewById(R.id.sub_progress);
        viewPager = (ViewPager) view.findViewById(R.id.sub_pager);

        editQuery.setText(getFilename());

        btnSearch.setEnabled(false);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (pagerAdapter != null) {
                    final String query = getQuery();

                    for (final String provider : listAdapters.keySet()) {
                        final ItemListAdapter listAdapter = listAdapters.get(provider);

                        listAdapter.clear();
                        startQuery(provider, query, listAdapter);
                    }
                }
            }
        });

        return view;
    }

    private String getFilename() {
        return getArguments().getString(EXTRA_FILENAME);
    }

    private String getDirectory() {
        return getArguments().getString(EXTRA_DIRECTORY);
    }

    private String getQuery() {
        return editQuery.getText().toString().trim();
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

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

    private void startMetadataLoading(final RemoteService service) {
        if (pagerAdapter == null) {
            service.loadSubtitleMetadata(getFilename(), new SubtitleMetadataCallback() {
                @Override
                public void onMetadataReceived(final SubtitleMetadata metadata) {
                    if (metadata != null) {
                        if (metadata.getShow() != null) {
                            txtInfo.setText(String.format("%s %02dx%02d",
                                    metadata.getShow(), metadata.getSeason(), metadata.getEpisode()));
                            txtInfo.setVisibility(View.VISIBLE);
                        }

                        for (final String provider : metadata.getProviders()) {
                            final ItemListAdapter adapter = new ItemListAdapter();
                            listAdapters.put(provider, adapter);

                            startQuery(provider, getFilename(), adapter);
                        }

                        pagerAdapter = new SubtitlePagerAdapter(metadata.getProviders());
                        viewPager.setAdapter(pagerAdapter);

                        btnSearch.setEnabled(true);
                    } else {
                        dismiss();
                    }
                }
            });
        }
    }

    private void startQuery(final String provider, final String query, final ItemListAdapter adapter) {
        if (rsc.isServiceBound()) {
            rsc.getService().querySubtitles(provider, query, new SubtitleQueryCallback() {
                @Override
                public void onItemReceived(final SubtitleItem item) {
                    adapter.add(item);
                }
            });
        }
    }

    private void startDownload(final SubtitleItem item) {
        if (rsc.isServiceBound()) {
            editQuery.setVisibility(View.GONE);
            btnSearch.setVisibility(View.GONE);
            viewPager.setVisibility(View.GONE);

            progressBar.setVisibility(View.VISIBLE);

            rsc.getService().downloadSubtitle(item.getProvider(), item.getId(), getDirectory(),
                    new SubtitleDownloadCallback() {
                        @Override
                        public void onDownloaded(final String filename) {
                            Toast.makeText(
                                    getActivity(),
                                    String.format("Subtitle downloaded: %s", filename),
                                    Toast.LENGTH_SHORT).show();

                            final Intent data = new Intent();
                            data.putExtra(RESULT_DIRECTORY, getDirectory());

                            getTargetFragment().onActivityResult(
                                    getTargetRequestCode(), Activity.RESULT_OK, data);

                            dismiss();
                        }

                        @Override
                        public void onFailed() {
                            Toast.makeText(
                                    getActivity(),
                                    "Failed to download subtitle",
                                    Toast.LENGTH_SHORT).show();

                            dismiss();
                        }
                    });
        }
    }

    public static SubtitleDialogFragment create(final String filename, final String directory) {
        final Bundle args = new Bundle();
        args.putString(EXTRA_FILENAME, filename);
        args.putString(EXTRA_DIRECTORY, directory);

        final SubtitleDialogFragment fragment = new SubtitleDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private class SubtitlePagerAdapter extends PagerAdapter {

        private final String[] providers;

        private SubtitlePagerAdapter(final String[] providers) {
            this.providers = providers;
        }

        @Override
        public int getCount() {
            return providers.length;
        }

        @Override
        public Object instantiateItem(final ViewGroup container, final int position) {
            final String provider = providers[position];

            final ListView listView = new ListView(getActivity());
            listView.setTag(provider);
            container.addView(listView);

            final ItemListAdapter adapter = listAdapters.get(provider);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(final AdapterView<?> parent,
                                        final View view, final int position, final long id) {
                    final SubtitleItem item = (SubtitleItem) parent.getAdapter().getItem(position);
                    startDownload(item);
                }
            });

            return listView;
        }

        @Override
        public void destroyItem(final ViewGroup container, final int position, final Object object) {
            if (object instanceof ListView) {
                container.removeView((ListView) object);
            }
        }

        @Override
        public CharSequence getPageTitle(final int position) {
            return providers[position];
        }

        @Override
        public boolean isViewFromObject(final View view, final Object object) {
            return view == object;
        }

    }

    private class ItemListAdapter extends BaseAdapter {

        private final ArrayList<SubtitleItem> items = new ArrayList<>(10);

        public void add(final SubtitleItem item) {
            items.add(item);
            notifyDataSetChanged();
        }

        public void clear() {
            items.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public SubtitleItem getItem(final int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(final int position) {
            return position;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = View.inflate(parent.getContext(), R.layout.li_subtitle, null);
            }

            ViewHolder holder = (ViewHolder) view.getTag();
            if (holder == null) {
                holder = new ViewHolder(view);
            }

            final SubtitleItem item = getItem(position);

            holder.txtTitle.setText(item.getTitle());
            holder.txtLanguage.setText(item.getLanguage());
            holder.txtExtras.setText(item.getExtras());

            return view;
        }

        private class ViewHolder {

            private final TextView txtTitle;
            private final TextView txtLanguage;
            private final TextView txtExtras;

            public ViewHolder(final View parent) {
                txtTitle = (TextView) parent.findViewById(R.id.txt_title);
                txtLanguage = (TextView) parent.findViewById(R.id.txt_language);
                txtExtras = (TextView) parent.findViewById(R.id.txt_extras);
            }

        }

    }

}
