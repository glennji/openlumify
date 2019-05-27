package org.openlumify.web.structuredingest.core;

import com.google.inject.Singleton;
import org.openlumify.web.privilegeFilters.EditPrivilegeFilter;
import org.visallo.webster.Handler;
import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.model.IRI;
import org.vertexium.Authorizations;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.core.model.ontology.OntologyRepository;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.web.AuthenticationHandler;
import org.openlumify.web.OpenLumifyCsrfHandler;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;
import org.openlumify.web.privilegeFilters.ReadPrivilegeFilter;
import org.openlumify.web.structuredingest.core.routes.Analyze;
import org.openlumify.web.structuredingest.core.routes.Ingest;
import org.openlumify.web.structuredingest.core.routes.MimeTypes;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import java.io.InputStream;

@Name("Structured File")
@Description("Supports importing structured data")
@Singleton
public class StructuredIngestWebAppPlugin implements WebAppPlugin {
    private final OntologyRepository ontologyRepository;
    private final UserRepository userRepository;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public StructuredIngestWebAppPlugin(
            OntologyRepository ontologyRepository,
            UserRepository userRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.ontologyRepository = ontologyRepository;
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticator = AuthenticationHandler.class;
        Class<? extends Handler> csrfProtector = OpenLumifyCsrfHandler.class;

        ensureOntologyDefined();

        app.registerJavaScript("/org/openlumify/web/structuredingest/core/js/plugin.js");
        app.registerResourceBundle("/org/openlumify/web/structuredingest/core/messages.properties");
        app.registerCss("/org/openlumify/web/structuredingest/core/css/style.css");

        app.registerJavaScript("/org/openlumify/web/structuredingest/core/libs/velocity.min.js", false);
        app.registerJavaScript("/org/openlumify/web/structuredingest/core/libs/velocity.ui.min.js", false);

        app.registerJavaScript("/org/openlumify/web/structuredingest/core/js/form.js", false);
        app.registerJavaScript("/org/openlumify/web/structuredingest/core/js/columnEditor.js", false);
        app.registerJavaScript("/org/openlumify/web/structuredingest/core/js/createdObjectPopover.js", false);
        app.registerJavaScript("/org/openlumify/web/structuredingest/core/js/relationEditor.js", false);
        app.registerJavaScript("/org/openlumify/web/structuredingest/core/js/errorsPopover.js", false);
        app.registerJavaScript("/org/openlumify/web/structuredingest/core/js/structuredFileImportAcivityFinished.js", false);
        app.registerJavaScript("/org/openlumify/web/structuredingest/core/js/util.js", false);
        app.registerJavaScript("/org/openlumify/web/structuredingest/core/js/auxillary/boolean.js", false);
        app.registerJavaScript("/org/openlumify/web/structuredingest/core/js/auxillary/date.js", false);
        app.registerJavaScript("/org/openlumify/web/structuredingest/core/js/auxillary/geoLocation.js", false);

        app.registerJavaScriptComponent("/org/openlumify/web/structuredingest/core/js/TextSection.jsx");

        app.registerWebWorkerJavaScript("/org/openlumify/web/structuredingest/core/js/web-worker/service.js");

        app.registerJavaScriptTemplate("/org/openlumify/web/structuredingest/core/templates/modal.hbs");
        app.registerJavaScriptTemplate("/org/openlumify/web/structuredingest/core/templates/entity.hbs");
        app.registerJavaScriptTemplate("/org/openlumify/web/structuredingest/core/templates/form.hbs");
        app.registerJavaScriptTemplate("/org/openlumify/web/structuredingest/core/templates/table.hbs");
        app.registerJavaScriptTemplate("/org/openlumify/web/structuredingest/core/templates/preview.hbs");
        app.registerJavaScriptTemplate("/org/openlumify/web/structuredingest/core/templates/columnEditor.hbs");
        app.registerJavaScriptTemplate("/org/openlumify/web/structuredingest/core/templates/relationEditor.hbs");
        app.registerJavaScriptTemplate("/org/openlumify/web/structuredingest/core/templates/error.hbs");
        app.registerJavaScriptTemplate("/org/openlumify/web/structuredingest/core/templates/createdObject.hbs");

        app.registerJavaScriptTemplate("/org/openlumify/web/structuredingest/core/templates/auxillary/boolean.hbs");
        app.registerJavaScriptTemplate("/org/openlumify/web/structuredingest/core/templates/auxillary/date.hbs");
        app.registerJavaScriptTemplate("/org/openlumify/web/structuredingest/core/templates/auxillary/geoLocation.hbs");

        app.get(
                "/structured-ingest/mimeTypes",
                authenticator,
                csrfProtector,
                ReadPrivilegeFilter.class,
                MimeTypes.class
        );
        app.get(
                "/structured-ingest/analyze",
                authenticator,
                csrfProtector,
                ReadPrivilegeFilter.class,
                Analyze.class
        );
        app.post(
                "/structured-ingest/ingest",
                authenticator,
                csrfProtector,
                EditPrivilegeFilter.class,
                Ingest.class
        );
    }

    private void ensureOntologyDefined() {
        try (InputStream structuredFileOwl = StructuredIngestWebAppPlugin.class.getResourceAsStream("structured-file.owl")) {
            byte[] inFileData = IOUtils.toByteArray(structuredFileOwl);
            IRI tagIRI = IRI.create(StructuredIngestOntology.IRI);
            Authorizations authorizations = authorizationRepository.getGraphAuthorizations(this.userRepository.getSystemUser());
            ontologyRepository.importFileData(inFileData, tagIRI, null, authorizations);
        } catch (Exception e) {
            throw new OpenLumifyException("Could not read structured-file.owl file", e);
        }
    }
}
