package org.openlumify.tools.ontology.ingest.codegen;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.google.common.base.Strings;
import org.openlumify.core.cmdline.CommandLineTool;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.web.clientapi.JsonUtil;
import org.openlumify.web.clientapi.UserNameAndPasswordOpenLumifyApi;
import org.openlumify.web.clientapi.UserNameOnlyOpenLumifyApi;
import org.openlumify.web.clientapi.OpenLumifyApi;
import org.openlumify.web.clientapi.model.ClientApiOntology;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

@Parameters(commandDescription = "Generate model classes based on a OpenLumify ontology.")
public class ModelCodeGen extends CommandLineTool {

    @Parameter(names = {"--inputJsonFile", "-f"}, arity = 1, converter = FileConverter.class, description = "The path to a local json file containing the OpenLumify ontology.")
    private File inputJsonFile;

    @Parameter(names = {"--openlumifyUrl", "-url"}, arity = 1, description = "The root URL of the OpenLumify instance from which to download the ontology.")
    private String openlumifyUrl;

    @Parameter(names = {"--openlumifyUsername", "-u"}, arity = 1, description = "The username to authenticate as when downloading the ontology from the OpenLumify instance.")
    private String openlumifyUsername;

    @Parameter(names = {"--openlumifyPassword", "-p"}, arity = 1, description = "The password to authenticate with when downloading the ontology from the OpenLumify instance.")
    private String openlumifyPassword;

    @Parameter(names = {"--outputDirectory", "-o"}, arity = 1, required = true, description = "The path to the output directory for the class files. If it does not exist, it will be created.")
    private String outputDirectory;

    @Parameter(names = {"--includeOpenLumifyClasses"}, description = "By default, the core OpenLumify concepts and relationships are skipped during code generation. Include this flag to include them.")
    private boolean includeOpenLumifyClasses;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new ModelCodeGen(), args, false);
    }

    @Override
    protected int run() throws Exception {
        String ontologyJsonString;
        if (inputJsonFile != null) {
            ontologyJsonString = new String(Files.readAllBytes(inputJsonFile.toPath()), Charset.forName("UTF-8"));
        } else if (!Strings.isNullOrEmpty(openlumifyUrl) && !Strings.isNullOrEmpty(openlumifyUsername) && !Strings.isNullOrEmpty(openlumifyPassword)) {
            OpenLumifyApi openlumifyApi = new UserNameAndPasswordOpenLumifyApi(openlumifyUrl, openlumifyUsername, openlumifyPassword, true);
            ontologyJsonString = openlumifyApi.invokeAPI("/ontology", "GET", null, null, null, null, "application/json");
            openlumifyApi.logout();
        } else if (!Strings.isNullOrEmpty(openlumifyUrl) && !Strings.isNullOrEmpty(openlumifyUsername)) {
            OpenLumifyApi openlumifyApi = new UserNameOnlyOpenLumifyApi(openlumifyUrl, openlumifyUsername, true);
            ontologyJsonString = openlumifyApi.invokeAPI("/ontology", "GET", null, null, null, null, "application/json");
            openlumifyApi.logout();
        } else {
            throw new OpenLumifyException("inputJsonFile or openlumifyUrl, openlumifyUsername, and openlumifyPassword parameters are required");
        }

        ClientApiOntology ontology = JsonUtil.getJsonMapper().readValue(ontologyJsonString, ClientApiOntology.class);
        ConceptWriter conceptWriter = new ConceptWriter(outputDirectory, ontology, includeOpenLumifyClasses);
        RelationshipWriter relationshipWriter = new RelationshipWriter(outputDirectory, ontology, includeOpenLumifyClasses);

        ontology.getConcepts().forEach(conceptWriter::writeClass);
        ontology.getRelationships().forEach(relationshipWriter::writeClass);

        return 0;
    }
}
