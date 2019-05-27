#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.worker;

import org.openlumify.core.model.properties.types.StringSingleValueOpenLumifyProperty;

public interface OntologyConstants {
    String ONTOLOGY_BASE_IRI = "${ontologyBaseIri}${symbol_pound}";

    String PERSON_CONCEPT_TYPE = ONTOLOGY_BASE_IRI + "person";

    String CONTACTS_CSV_FILE_CONCEPT_TYPE = ONTOLOGY_BASE_IRI + "contactsCsvFile";

    String HAS_ENTITY_EDGE_LABEL = ONTOLOGY_BASE_IRI + "hasEntity";
    String KNOWS_EDGE_LABEL = ONTOLOGY_BASE_IRI + "knows";

    StringSingleValueOpenLumifyProperty PERSON_FULL_NAME_PROPERTY =
            new StringSingleValueOpenLumifyProperty(ONTOLOGY_BASE_IRI + "fullName");

    StringSingleValueOpenLumifyProperty PERSON_PHONE_NUMBER_PROPERTY =
            new StringSingleValueOpenLumifyProperty(ONTOLOGY_BASE_IRI + "phoneNumber");

    StringSingleValueOpenLumifyProperty PERSON_EMAIL_ADDRESS_PROPERTY =
            new StringSingleValueOpenLumifyProperty(ONTOLOGY_BASE_IRI + "emailAddress");
}
