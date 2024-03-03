package com.mydeveloperplanet.mystructurizrplanet;

import java.io.File;
import java.util.Set;

import com.structurizr.Workspace;
import com.structurizr.dsl.StructurizrDslParser;
import com.structurizr.model.Element;
import com.structurizr.model.Relationship;
import com.structurizr.model.SoftwareSystem;

public class ValidateRelationships {

    public static void main(String[] args) throws Exception {
        validate();
    }

    private static void validate() throws Exception {
        validateRelationshipsWithCustomerService("src/main/resources/order-service/workspace.dsl");
        validateRelationshipsWithCustomerService("src/main/resources/invoice-service/workspace.dsl");
    }

    private static void validateRelationshipsWithCustomerService(String workspaceDsl) throws Exception {
        StructurizrDslParser parser = new StructurizrDslParser();
        parser.parse(new File(workspaceDsl));
        Workspace workspace = parser.getWorkspace();
        Set<Relationship> relationships = workspace.getModel().getRelationships();
        for (Relationship relationship : relationships) {
            if (isSystemLandscapeRelationship(relationship)) {
                if (!isRelationDefinedInCustomerService(relationship)) {
                    System.out.println("missing relation in CustomerService " + relationship);
                }
            }
        }
    }

    private static boolean isRelationDefinedInCustomerService(Relationship validateRelation) throws Exception {
        StructurizrDslParser parser = new StructurizrDslParser();
        parser.parse(new File("src/main/resources/customer-service/workspace.dsl"));
        Workspace workspaceCustomerService = parser.getWorkspace();
        Set<Relationship> relationshipCustomerService = workspaceCustomerService.getModel().getRelationships();
        for (Relationship relationship : relationshipCustomerService) {
            if (isSystemLandscapeRelationship(relationship)) {
                if (validateRelation.getSourceId().equals(relationship.getSourceId()) && validateRelation.getDestinationId().equals(relationship.getDestinationId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSystemLandscapeRelationship(Relationship relationship) {
        return isElementPartOfSystemLandscape(relationship.getSource()) && isElementPartOfSystemLandscape(relationship.getDestination());
    }

    private static boolean isElementPartOfSystemLandscape(Element element) {
        return element instanceof SoftwareSystem;
    }

}
