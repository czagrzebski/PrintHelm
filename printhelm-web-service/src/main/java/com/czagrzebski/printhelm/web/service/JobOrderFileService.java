package com.czagrzebski.printhelm.web.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class JobOrderFileService {

    private final GridFsTemplate gridFsTemplate;

    public JobOrderFileService(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

    public String storeFile(long orderId, String fileType, MultipartFile file) throws IOException {
        Document metadata = new Document()
                .append("orderId", orderId)
                .append("fileType", fileType);
        ObjectId id = gridFsTemplate.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                metadata
        );
        return id.toHexString();
    }

    public GridFsResource getFileResource(String fileId) {
        GridFSFile file = gridFsTemplate.findOne(queryById(fileId));
        if (file == null) throw new IllegalArgumentException("File not found: " + fileId);
        return gridFsTemplate.getResource(file);
    }

    public String getFilename(String fileId) {
        GridFSFile file = gridFsTemplate.findOne(queryById(fileId));
        if (file == null) throw new IllegalArgumentException("File not found: " + fileId);
        return file.getFilename();
    }

    public void deleteFile(String fileId) {
        gridFsTemplate.delete(queryById(fileId));
    }

    private Query queryById(String fileId) {
        return new Query(Criteria.where("_id").is(new ObjectId(fileId)));
    }
}
