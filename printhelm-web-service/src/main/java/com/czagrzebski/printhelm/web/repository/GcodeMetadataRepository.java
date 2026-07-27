package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.domain.GcodeMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GcodeMetadataRepository extends MongoRepository<GcodeMetadata, String> {
}
