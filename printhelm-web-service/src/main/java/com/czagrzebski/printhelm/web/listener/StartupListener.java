package com.czagrzebski.printhelm.web.listener;

import com.czagrzebski.printhelm.model.ConnectionType;
import com.czagrzebski.printhelm.web.admindata.AdminDataImporter;
import com.czagrzebski.printhelm.web.connection.MqttConnectionManager;
import com.czagrzebski.printhelm.web.domain.Privilege;
import com.czagrzebski.printhelm.web.domain.Role;
import com.czagrzebski.printhelm.web.domain.User;
import com.czagrzebski.printhelm.web.domain.connection.ConnectionConfig;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import com.czagrzebski.printhelm.web.repository.PrivilegeRepository;
import com.czagrzebski.printhelm.web.repository.RoleRepository;
import com.czagrzebski.printhelm.web.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Component
public class StartupListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LogManager.getLogger(StartupListener.class);
    boolean setupComplete = false;
    private final ApplicationContext appContext;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final PasswordEncoder passwordEncoder;
    private final PrinterRepository printerRepository;
    private final MqttConnectionManager mqttConnectionManager;

    public StartupListener(UserRepository userRepository, RoleRepository roleRepository,
                           PrivilegeRepository privilegeRepository, PasswordEncoder passwordEncoder, ApplicationContext appContext, PrinterRepository printerRepository, MqttConnectionManager mqttConnectionManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
        this.passwordEncoder = passwordEncoder;
        this.appContext = appContext;
        this.printerRepository = printerRepository;
        this.mqttConnectionManager = mqttConnectionManager;
    }

    @Transactional
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if(!setupComplete) {
            loadAdminData();
            connectPrintersOnStartup();
            setupComplete = true;
        }
    }

    private void loadAdminData() {
        try {
            logger.info("Checking for admin data to load on startup...");
            HashMap<String, NodeList> adminDataXML = AdminDataImporter.getImportedAdminDataFromXML();
            importPrivileges(adminDataXML.get("PRIVILEGE"));
            importRoles(adminDataXML.get("ROLE"));
            importUsers(adminDataXML.get("USER"));
            logger.info("Admin data load complete!");
        } catch (Exception e) {
            logger.info("Failed to load admin data on startup. Terminating application! {}", e.toString());
            SpringApplication.exit(appContext, () -> -1);
        }
    }

    private void importPrivileges(NodeList privilegeNodes) throws Exception {
        logger.info("Importing Privileges");
        for(int i=0; i < privilegeNodes.getLength(); i++) {
            var node = privilegeNodes.item(i);
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            var privilegeName = getXPathValue(xpath, node, "@name");
            var privilegeDescription = getXPathValue(xpath, node, "Description");
            createPrivilegeIfNotFoundOrUpdate(privilegeName, privilegeDescription);
        }
    }

    private void importRoles(NodeList roleNodes) throws Exception {
        logger.info("Importing Roles");
        for(int i=0; i < roleNodes.getLength(); i++) {
            var node = roleNodes.item(i);
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            var roleName = getXPathValue(xpath, node, "@name");
            var roleDescription = getXPathValue(xpath, node, "Description");
            NodeList privilegeNodes = getXPathNodeList(node, "Privileges/Privilege");
            Set<Privilege> privileges = new HashSet<>();
            for(int j=0; j < privilegeNodes.getLength(); j++) {
                Element privilegeElement = (Element) privilegeNodes.item(j);
                privileges.add(privilegeRepository.findByPrivilegeName(privilegeElement.getAttribute("name")));
            }
            createRoleIfNotFoundOrUpdate(roleName, roleDescription, privileges);
        }
        // Process inherited roles
        importInheritedRoles(roleNodes);

    }

    // For nested role inheritance, we continuously process each role and its inherited
    // privileges until no more privileges are inherited. This ensures all privileges from
    // the entire inheritance chain are applied, even across multiple generations of roles.
    private void importInheritedRoles(NodeList roleNodes) throws Exception {
        logger.info("Processing role inheritances");
        var allInheritedRolesProcessed = false;
        do {
            allInheritedRolesProcessed = !processRoleInheritance(roleNodes);
        } while(!allInheritedRolesProcessed);
    }

    private void importUsers(NodeList userNodes) throws Exception {
        logger.info("Importing Users");
        for(int i=0; i < userNodes.getLength(); i++) {
            var node = userNodes.item(i);
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            var username = getXPathValue(xpath, node, "@name");
            var firstName = getXPathValue(xpath, node, "FirstName");
            var lastName = getXPathValue(xpath, node, "LastName");
            var password = getXPathValue(xpath, node, "Credential");
            NodeList roleNodes = getXPathNodeList(node, "Roles/Role");
            Set<Role> roles = new HashSet<>();
            for(int j=0; j < roleNodes.getLength(); j++) {
                Element roleElement = (Element) roleNodes.item(j);
                roles.add(roleRepository.findByRoleName(roleElement.getAttribute("name")));
            }
            createUserIfNotFoundOrUpdate(username, password, firstName, lastName, roles);
        }
    }

    private boolean processRoleInheritance(NodeList roleNodes) throws Exception {
        var rolesUpdatedFromInheritance = false;
        for(int i=0; i < roleNodes.getLength(); i++) {
            var node = roleNodes.item(i);
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            var role = roleRepository.findByRoleName(getXPathValue(xpath, node, "@name"));
            var rolePrivileges = role.getPrivileges();
            NodeList inheritanceNodes = getXPathNodeList(node, "Inheritances/Inherits");
            for(int j=0; j < inheritanceNodes.getLength(); j++){
                var inheritedRoleNode = inheritanceNodes.item(j);
                var inheritedRole = roleRepository.findByRoleName(getXPathValue(xpath, inheritedRoleNode, "@role"));
                var setChanged = rolePrivileges.addAll(inheritedRole.getPrivileges());
                if(setChanged) {
                    rolesUpdatedFromInheritance = true;
                }
            }
            roleRepository.save(role);
        }
        return rolesUpdatedFromInheritance;
    }

    private Privilege createPrivilegeIfNotFoundOrUpdate(String privilegeName, String privilegeDescription) {
        Privilege privilege = privilegeRepository.findByPrivilegeName(privilegeName);
        if (privilege == null) {
            logger.info("Database upgrade: creating privilege {}", privilegeName);
            privilege = new Privilege(privilegeName, privilegeDescription);
        } else {
            privilege.setPrivilegeDescription(privilegeDescription);
        }
        privilegeRepository.save(privilege);
        return privilege;
    }

    private Role createRoleIfNotFoundOrUpdate(
            String roleName, String roleDescription, Set<Privilege> privileges) {

        Role role = roleRepository.findByRoleName(roleName);
        if (role == null) {
            logger.info("Database upgrade: creating role {}", roleName);
            role = new Role(roleName, roleDescription, privileges);
        } else {
            role.setRoleDescription(roleDescription);
            role.setPrivileges(privileges);
        }
        roleRepository.save(role);
        return role;
    }

    private User createUserIfNotFoundOrUpdate(
            String username, String password, String firstName, String lastName, Set<Role> roles) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.info("Database upgrade: creating user {}", username);
            user = new User(username, passwordEncoder.encode(password), firstName, lastName);
            user.setUserRoles(roles);
            user.setActive(true);
        } else {
            var updatedRoles = user.getUserRoles();
            // modifying roles is only additive
            updatedRoles.addAll(roles);
            user.setUserRoles(updatedRoles);
            user.setFirstname(firstName);
            user.setLastname(lastName);
        }
        userRepository.save(user);
        return user;
    }

    // Helper method to evaluate XPath expression on a given node
    private static String getXPathValue(XPath xpath, Node node, String childNodeName) throws Exception {
        XPathExpression expr = xpath.compile(childNodeName);
        return (String) expr.evaluate(node, XPathConstants.STRING);
    }

    // Helper method to evaluate XPath expression and return NodeList
    private static NodeList getXPathNodeList(Node node, String expression) throws Exception {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        XPathExpression expr = xpath.compile(expression);
        return (NodeList) expr.evaluate(node, XPathConstants.NODESET);
    }

    private void connectPrintersOnStartup() {
        logger.info("Connecting to printers");
        printerRepository.findAll().forEach(printer -> {
            try {
                if (printer.getConnectionConfig() != null && printer.getConnectionConfig().getConnectionType() == ConnectionType.MQTT) {
                    mqttConnectionManager.connect(printer);
                    logger.info("Connected to printer [ID={}]", printer.getPrinterId());
                } else {
                    logger.warn("Printer [ID={}] is not configured for MQTT connection", printer.getPrinterId());
                }
            } catch (Exception e) {
                logger.error("Failed to connect to printer [ID={}]: {}", printer.getPrinterId(), e.getMessage());
            }
        });

        logger.info("Printer connection process completed.");
    }
}
