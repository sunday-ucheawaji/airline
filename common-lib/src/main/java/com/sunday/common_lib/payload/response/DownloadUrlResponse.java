package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;

/** A short-lived signed link to one document; the file is never public and never streamed through the gateway. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DownloadUrlResponse {

    private String url;
    private Instant expiresAt;
    private String fileName;
    private String contentType;
}
