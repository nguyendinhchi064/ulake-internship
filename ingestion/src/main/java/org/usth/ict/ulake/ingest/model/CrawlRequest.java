package org.usth.ict.ulake.ingest.model;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.usth.ict.ulake.ingest.utils.PolicyToStringConverter;

@Entity
public class CrawlRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Schema(description = "Owner of crawl request")
    public Long ownerId;

    // TODO: missing JavaTypeDescriptor for custom Policy type
    @Lob
    @Convert(converter = PolicyToStringConverter.class)
    @Schema(description = "Execution query")
    public Policy query;

    @Schema(description = "Folder holding crawled file")
    public Long folderId;

    @Schema(description = "Short explanation of request")
    public String description;

    @Schema(description = "Starting time of request")
    public Long creationTime;

    @Schema(description = "Finishing time of request")
    public Long endTime;

    public CrawlRequest() {}
}
