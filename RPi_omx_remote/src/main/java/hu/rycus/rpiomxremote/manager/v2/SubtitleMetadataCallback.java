package hu.rycus.rpiomxremote.manager.v2;

import hu.rycus.rpiomxremote.blocks.SubtitleMetadata;

public interface SubtitleMetadataCallback {

    void onMetadataReceived(SubtitleMetadata metadata);

}
