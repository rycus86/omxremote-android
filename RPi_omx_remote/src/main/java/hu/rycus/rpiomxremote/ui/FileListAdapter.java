package hu.rycus.rpiomxremote.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import hu.rycus.rpiomxremote.R;
import hu.rycus.rpiomxremote.blocks.FileList;
import hu.rycus.rpiomxremote.util.Constants;

/**
 * Adapter for listing the contents of a FileList object.
 *
 * <br/>
 * Created by Viktor Adam on 10/30/13.
 *
 * @author rycus
 */
public class FileListAdapter extends BaseAdapter {

    /** The items (file and directory names) of the root path. */
    private final List<String> items = new ArrayList<String>();

    /** @see android.widget.BaseAdapter#getCount() */
    @Override
    public int getCount() {
        return items.size();
    }

    /** @see android.widget.BaseAdapter#getItem(int) */
    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    /** @see android.widget.BaseAdapter#getItemId(int) */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /** @see android.widget.BaseAdapter#getView(int, android.view.View, android.view.ViewGroup) */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        ViewHolder holder = null;

        // if there is no list item view, inflate one
        if(view == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.li_file, parent, false);

            ImageView vIcon     = (ImageView) view.findViewById(R.id.li_file_icon);
            TextView  vFilename = (TextView)  view.findViewById(R.id.li_file_name);

            // setup the viewholder
            holder = new ViewHolder();
            holder.imgIcon = vIcon;
            holder.txtFilename = vFilename;

            view.setTag(holder);
        } else {
            // the views were already found
            holder = (ViewHolder) view.getTag();
        }

        String filename = (String) getItem(position);
        boolean isDir = filename.endsWith("/");

        int icon = R.drawable.ic_file_unknown;
        if(isDir) {
            icon = R.drawable.ic_action_collection;
            filename = filename.substring(0, filename.length() - 1);
        } else {
            if(filename.contains(".")) {
                String ext = filename.substring(filename.lastIndexOf('.') + 1);
                if(Constants.Extensions.isVideo(ext)) {
                    icon = R.drawable.ic_file_video;
                } else if(Constants.Extensions.isSubtitle(ext)) {
                    icon = R.drawable.ic_file_subtitle;
                }
            }
        }

        holder.imgIcon.setImageResource(icon);
        holder.txtFilename.setText(filename);

        return view;
    }

    /** Sets the currently diplayed FileList item. */
    public void setItems(FileList list) {
        items.clear();
        items.addAll(list.getFiles());

        notifyDataSetChanged();
    }

    public int indexOf(final String item) {
        return items.indexOf(item);
    }

    /** Class for holding the inflated views of a list item. */
    private class ViewHolder {

        /** The icon in the list item. */
        ImageView imgIcon;
        /** The text view (displaying the file name) in the list item. */
        TextView txtFilename;

    }

}
