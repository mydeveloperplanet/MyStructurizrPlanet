package com.mydeveloperplanet.mystructurizrplanet;

import java.io.File;
import java.util.List;

import com.structurizr.Workspace;
import com.structurizr.api.AdminApiClient;
import com.structurizr.api.WorkspaceApiClient;
import com.structurizr.api.WorkspaceMetadata;
import com.structurizr.configuration.WorkspaceScope;
import com.structurizr.dsl.StructurizrDslParser;
import com.structurizr.model.Element;
import com.structurizr.model.Model;
import com.structurizr.model.Person;
import com.structurizr.model.Relationship;
import com.structurizr.model.SoftwareSystem;
import com.structurizr.view.SystemLandscapeView;

public class CreateSystemLandscape {

    private static final String STRUCTURIZR_ONPREMISES_URL = "http://localhost:8080";
    private static final String ADMIN_API_KEY_PLAINTEXT = "password";
    private static WorkspaceMetadata SYSTEM_LANDSCAPE_WORKSPACE_METADATA;

    public static void main(String[] args) throws Exception {

        // create workspace 1 (a placeholder for the landscape workspace)
        SYSTEM_LANDSCAPE_WORKSPACE_METADATA = createAdminApiClient().createWorkspace();

        loadServicesWorkspaces();
        generateSystemLandscapeWorkspace();

    }

    private static AdminApiClient createAdminApiClient() {
        return new AdminApiClient(STRUCTURIZR_ONPREMISES_URL + "/api", null, ADMIN_API_KEY_PLAINTEXT);
    }

    private static WorkspaceApiClient createWorkspaceApiClient(WorkspaceMetadata workspaceMetadata) {
        WorkspaceApiClient workspaceApiClient = new WorkspaceApiClient(STRUCTURIZR_ONPREMISES_URL + "/api", workspaceMetadata.getApiKey(), workspaceMetadata.getApiSecret());
        workspaceApiClient.setWorkspaceArchiveLocation(null); // this prevents the local file system from being cluttered with JSON files

        return workspaceApiClient;
    }

    private static void loadServicesWorkspaces() throws Exception {
        // create workspace 2 (customer service)
        WorkspaceMetadata workspaceMetadataCustomerService = createAdminApiClient().createWorkspace();
        StructurizrDslParser parser = new StructurizrDslParser();
        parser.parse(new File("src/main/resources/customer-service/workspace.dsl"));
        Workspace workspaceCustomerService = parser.getWorkspace();
        workspaceCustomerService.trim();
        WorkspaceApiClient workspaceApiClient = createWorkspaceApiClient(workspaceMetadataCustomerService);
        workspaceApiClient.setWorkspaceArchiveLocation(null);
        workspaceApiClient.putWorkspace(workspaceMetadataCustomerService.getId(), workspaceCustomerService);

        // create workspace 3 (invoice service)
        WorkspaceMetadata workspaceMetadataInvoiceService = createAdminApiClient().createWorkspace();
        parser = new StructurizrDslParser();
        parser.parse(new File("src/main/resources/invoice-service/workspace.dsl"));
        Workspace workspaceInvoiceService = parser.getWorkspace();
        workspaceInvoiceService.trim();
        workspaceApiClient = createWorkspaceApiClient(workspaceMetadataInvoiceService);
        workspaceApiClient.setWorkspaceArchiveLocation(null);
        workspaceApiClient.putWorkspace(workspaceMetadataInvoiceService.getId(), workspaceInvoiceService);

        // create workspace 4 (order service)
        WorkspaceMetadata workspaceMetadataOrderService = createAdminApiClient().createWorkspace();
        parser = new StructurizrDslParser();
        parser.parse(new File("src/main/resources/order-service/workspace.dsl"));
        Workspace workspaceOrderService = parser.getWorkspace();
        workspaceOrderService.trim();
        workspaceApiClient = createWorkspaceApiClient(workspaceMetadataOrderService);
        workspaceApiClient.setWorkspaceArchiveLocation(null);
        workspaceApiClient.putWorkspace(workspaceMetadataOrderService.getId(), workspaceOrderService);
    }

    private static void generateSystemLandscapeWorkspace() throws Exception {
        // create a workspace based upon the system catalog ... this has people and software systems, but no relationships
        StructurizrDslParser parser = new StructurizrDslParser();
        parser.parse(new File("src/main/resources/system-catalog.dsl"));

        Workspace systemLandscapeWorkspace = parser.getWorkspace();
        systemLandscapeWorkspace.setName("Landscape");
        enrichSystemLandscape(systemLandscapeWorkspace);
    }

    protected static void enrichSystemLandscape(Workspace systemLandscapeWorkspace) throws Exception {
        // extract all relationships between people/software systems from all software system scoped workspaces
        // so they can be added to the system landscape workspace
        List<WorkspaceMetadata> workspaces = createAdminApiClient().getWorkspaces();
        for (WorkspaceMetadata workspaceMetadata : workspaces) {
            WorkspaceApiClient workspaceApiClient = createWorkspaceApiClient(workspaceMetadata);
            workspaceApiClient.setWorkspaceArchiveLocation(null);
            Workspace workspace = workspaceApiClient.getWorkspace(workspaceMetadata.getId());
            if (workspace.getConfiguration().getScope() == WorkspaceScope.SoftwareSystem) {
                SoftwareSystem softwareSystem = findScopedSoftwareSystem(workspace);
                if (softwareSystem != null) {
                    systemLandscapeWorkspace.getModel().getSoftwareSystemWithName(softwareSystem.getName()).setUrl("{workspace:" + workspaceMetadata.getId() + "}/diagrams#SystemContext");
                }

                findAndCloneRelationships(workspace, systemLandscapeWorkspace);
            }
        }

        // create a system landscape view
        SystemLandscapeView view = systemLandscapeWorkspace.getViews().createSystemLandscapeView("Landscape", "An automatically generated system landscape view.");
        view.addAllElements();
        view.enableAutomaticLayout();

        // and push the landscape workspace to the on-premises installation
        WorkspaceApiClient workspaceApiClient = createWorkspaceApiClient(SYSTEM_LANDSCAPE_WORKSPACE_METADATA);
        workspaceApiClient.putWorkspace(SYSTEM_LANDSCAPE_WORKSPACE_METADATA.getId(), systemLandscapeWorkspace);
    }

    private static SoftwareSystem findScopedSoftwareSystem(Workspace workspace) {
        return workspace.getModel().getSoftwareSystems().stream().filter(ss -> !ss.getContainers().isEmpty()).findFirst().orElse(null);
    }

    private static void findAndCloneRelationships(Workspace source, Workspace destination) {
        for (Relationship relationship : source.getModel().getRelationships()) {
            if (isPersonOrSoftwareSystem(relationship.getSource()) && isPersonOrSoftwareSystem(relationship.getDestination())) {
                cloneRelationshipIfItDoesNotExist(relationship, destination.getModel());
            }
        }
    }

    private static boolean isPersonOrSoftwareSystem(Element element) {
        return element instanceof Person || element instanceof SoftwareSystem;
    }

    private static void cloneRelationshipIfItDoesNotExist(Relationship relationship, Model model) {
        Relationship clonedRelationship = null;

        if (relationship.getSource() instanceof SoftwareSystem && relationship.getDestination() instanceof SoftwareSystem) {
            SoftwareSystem source = model.getSoftwareSystemWithName(relationship.getSource().getName());
            SoftwareSystem destination = model.getSoftwareSystemWithName(relationship.getDestination().getName());

            if (source != null && destination != null && !source.hasEfferentRelationshipWith(destination)) {
                clonedRelationship = source.uses(destination, relationship.getDescription());
            }
        } else if (relationship.getSource() instanceof Person && relationship.getDestination() instanceof SoftwareSystem) {
            Person source = model.getPersonWithName(relationship.getSource().getName());
            SoftwareSystem destination = model.getSoftwareSystemWithName(relationship.getDestination().getName());

            if (source != null && destination != null && !source.hasEfferentRelationshipWith(destination)) {
                clonedRelationship = source.uses(destination, relationship.getDescription());
            }
        } else if (relationship.getSource() instanceof SoftwareSystem && relationship.getDestination() instanceof Person) {
            SoftwareSystem source = model.getSoftwareSystemWithName(relationship.getSource().getName());
            Person destination = model.getPersonWithName(relationship.getDestination().getName());

            if (source != null && destination != null && !source.hasEfferentRelationshipWith(destination)) {
                clonedRelationship = source.delivers(destination, relationship.getDescription());
            }
        } else if (relationship.getSource() instanceof Person && relationship.getDestination() instanceof Person) {
            Person source = model.getPersonWithName(relationship.getSource().getName());
            Person destination = model.getPersonWithName(relationship.getDestination().getName());

            if (source != null && destination != null && !source.hasEfferentRelationshipWith(destination)) {
                clonedRelationship = source.delivers(destination, relationship.getDescription());
            }
        }

        if (clonedRelationship != null) {
            clonedRelationship.addTags(relationship.getTags());
        }
    }

}