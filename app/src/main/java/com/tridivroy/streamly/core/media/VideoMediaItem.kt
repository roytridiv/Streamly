package com.tridivroy.streamly.core.media

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.tridivroy.streamly.domain.model.Video

/** Builds a playable item for [Video]; the media ID is the video ID so players can tell items apart. */
fun Video.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setUri(videoUrl)
        // Extension sniffing misses HLS URLs with query strings or no extension, so tag them explicitly.
        .apply { if (".m3u8" in videoUrl) setMimeType(MimeTypes.APPLICATION_M3U8) }
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
        .build()
