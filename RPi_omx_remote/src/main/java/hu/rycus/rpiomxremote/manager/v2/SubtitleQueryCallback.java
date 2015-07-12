package hu.rycus.rpiomxremote.manager.v2;

import hu.rycus.rpiomxremote.blocks.SubtitleItem;

public interface SubtitleQueryCallback {

    void onItemReceived(SubtitleItem item);

}
