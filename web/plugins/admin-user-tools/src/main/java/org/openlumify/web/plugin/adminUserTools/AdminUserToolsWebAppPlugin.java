package org.openlumify.web.plugin.adminUserTools;

import org.visallo.webster.Handler;
import org.openlumify.core.model.Description;
import org.openlumify.core.model.Name;
import org.openlumify.web.OpenLumifyCsrfHandler;
import org.openlumify.web.WebApp;
import org.openlumify.web.WebAppPlugin;
import org.openlumify.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Admin User Tools")
@Description("Admin tools to add/update/delete users")
public class AdminUserToolsWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = OpenLumifyCsrfHandler.class;

        app.registerJavaScript("/org/openlumify/web/adminUserTools/plugin.js");

        app.registerJavaScriptComponent("/org/openlumify/web/adminUserTools/UserAdminPlugin.jsx");
        app.registerJavaScriptComponent("/org/openlumify/web/adminUserTools/WorkspaceList.jsx");
        app.registerJavaScriptComponent("/org/openlumify/web/adminUserTools/LoadUser.jsx");
        app.registerJavaScriptComponent("/org/openlumify/web/adminUserTools/UserTypeaheadInput.jsx");
        app.registerJavaScriptComponent("/org/openlumify/web/adminUserTools/ActiveUserList.jsx");
        app.registerLess("/org/openlumify/web/adminUserTools/userAdmin.less");
        app.registerCss("/org/openlumify/web/adminUserTools/workspaceList.css");

        app.registerResourceBundle("/org/openlumify/web/adminUserTools/messages.properties");

        app.post("/user/delete", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserDelete.class);
        app.post("/workspace/shareWithMe", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, WorkspaceShareWithMe.class);
    }
}
