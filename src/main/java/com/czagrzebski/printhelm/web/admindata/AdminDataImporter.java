package com.czagrzebski.printhelm.web.admindata;

import com.czagrzebski.printhelm.web.listener.StartupDataLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public final class AdminDataImporter {
    private static final String RESOURCE_ADMIN_DATA = "admindata";
    private static final Logger logger = LogManager.getLogger(AdminDataImporter.class);

    private AdminDataImporter() {
        throw new UnsupportedOperationException("Cannot instantiate AdminDataImporter class!");
    }

    public static HashMap<String, NodeList> getImportedAdminDataFromXML() throws Exception {
        HashMap<String, NodeList> adminDataNodes = new HashMap<>();
        URL resource = AdminDataImporter.class.getClassLoader().getResource(RESOURCE_ADMIN_DATA);
        if(resource != null) {
            File adminDataFolder = new File(resource.getFile());
            File[] xmlFiles = adminDataFolder.listFiles((dir, name) -> name.endsWith(".xml"));
            if(xmlFiles != null) {
                List<File> sortedXmlFiles = Arrays.stream(xmlFiles).sorted(Comparator.comparing(File::getName)).toList();
                for(File xml : sortedXmlFiles) {
                    try {
                        logger.info("Loading admin data from {}", xml.getName());
                        String name = xml.getName().split("_")[1].replaceFirst("\\.xml$", "");
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(xml);
                        doc.getDocumentElement().normalize();
                        NodeList nodeList = doc.getElementsByTagName(name);
                        adminDataNodes.put(name.toUpperCase(Locale.ROOT), nodeList);

                    } catch (Exception e) {
                        throw new Exception("Failed to import admin data from XML!");
                    }
                }
            }
        }
        return adminDataNodes;
    }


}
