package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.domain.JobOrderFileType;
import com.czagrzebski.printhelm.web.domain.JobOrderFileVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobOrderFileVersionRepository extends JpaRepository<JobOrderFileVersion, Long> {

    List<JobOrderFileVersion> findByOrderIdAndFileTypeOrderByVersionNumberDesc(Long orderId, JobOrderFileType fileType);

    List<JobOrderFileVersion> findByOrderId(Long orderId);

    Optional<JobOrderFileVersion> findByVersionIdAndOrderId(Long versionId, Long orderId);

    Optional<JobOrderFileVersion> findFirstByOrderIdAndFileTypeOrderByVersionNumberDesc(Long orderId, JobOrderFileType fileType);
}
