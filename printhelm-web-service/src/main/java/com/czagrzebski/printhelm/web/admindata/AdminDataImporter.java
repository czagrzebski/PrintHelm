package com.czagrzebski.printhelm.web.admindata;

import com.czagrzebski.printhelm.web.exception.AdminDataImportException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

public final class AdminDataImporter {

    private static final Logger logger = LogManager.getLogger(AdminDataImporter.class);
    private static final String RESOURCE_PATH_PATTERN = "classpath:admindata/*.xml";

    private AdminDataImporter() {
        throw new UnsupportedOperationException("Cannot instantiate AdminDataImporter!");
    }

    public static HashMap<String, NodeList> getImportedAdminDataFromXML() throws AdminDataImportException {
        HashMap<String, NodeList> adminDataNodes = new HashMap<>();

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(RESOURCE_PATH_PATTERN);

            if (resources.length == 0) {
                throw new AdminDataImportException("No admin XML files found under /admindata");
            }

            // Sort XMLs by filename
            Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            for (Resource res : resources) {
                String filename = res.getFilename(); // e.g., roles_1.xml
                logger.info("Loading admin data from {}", filename);

                try (InputStream inputStream = res.getInputStream()) {
                    Document doc = dBuilder.parse(inputStream);
                    doc.getDocumentElement().normalize();

                    // Extract name from filename: e.g., "roles_1.xml" -> "roles"
                    String name = filename.split("_")[1].split("\\.")[0];
                    NodeList nodes = doc.getElementsByTagName(name);

                    adminDataNodes.put(name.toUpperCase(Locale.ROOT), nodes);
                }
            }

        } catch (Exception e) {
            throw new AdminDataImportException("Failed to import admin data from XMLs");
        }

        return adminDataNodes;
    }
}