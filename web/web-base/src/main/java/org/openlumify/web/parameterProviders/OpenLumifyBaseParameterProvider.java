package org.openlumify.web.parameterProviders;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.vertexium.FetchHint;
import org.vertexium.SecurityVertexiumException;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.user.User;
import org.openlumify.web.CurrentUser;
import org.openlumify.web.WebApp;
import org.visallo.webster.App;
import org.visallo.webster.parameterProviders.ParameterProvider;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.TimeZone;

public abstract class OpenLumifyBaseParameterProvider<T> extends ParameterProvider<T> {
    public static final String VISALLO_WORKSPACE_ID_HEADER_NAME = "OpenLumify-Workspace-Id";
    public static final String VISALLO_SOURCE_GUID_HEADER_NAME = "OpenLumify-Source-Guid";
    private static final String LOCALE_LANGUAGE_PARAMETER = "localeLanguage";
    private static final String LOCALE_COUNTRY_PARAMETER = "localeCountry";
    private static final String LOCALE_VARIANT_PARAMETER = "localeVariant";
    private static final String VISALLO_TIME_ZONE_HEADER_NAME = "OpenLumify-TimeZone";
    private static final String TIME_ZONE_ATTRIBUTE_NAME = "timeZone";
    private static final String TIME_ZONE_PARAMETER_NAME = "timeZone";
    public static final String WORKSPACE_ID_ATTRIBUTE_NAME = "workspaceId";
    private final Configuration configuration;

    public OpenLumifyBaseParameterProvider(Configuration configuration) {
        this.configuration = configuration;
    }

    public static String getActiveWorkspaceIdOrDefault(
            final HttpServletRequest request,
            final WorkspaceRepository workspaceRepository
    ) {
        String workspaceId = (String) request.getAttribute(WORKSPACE_ID_ATTRIBUTE_NAME);
        if (workspaceId == null || workspaceId.trim().length() == 0) {
            workspaceId = request.getHeader(VISALLO_WORKSPACE_ID_HEADER_NAME);
            if (workspaceId == null || workspaceId.trim().length() == 0) {
                workspaceId = getOptionalParameter(request, WORKSPACE_ID_ATTRIBUTE_NAME);
                if (workspaceId == null || workspaceId.trim().length() == 0) {
                    return null;
                }
            }
        }

        User user = CurrentUser.get(request);
        try {
            if (!workspaceRepository.hasReadPermissions(workspaceId, user)) {
                throw new OpenLumifyAccessDeniedException(
                        "You do not have access to workspace: " + workspaceId,
                        user,
                        workspaceId
                );
            }
        } catch (SecurityVertexiumException e) {
            throw new OpenLumifyAccessDeniedException(
                    "Error getting access to requested workspace: " + workspaceId,
                    user,
                    workspaceId
            );
        }

        return workspaceId;
    }

    protected static String getActiveWorkspaceId(
            final HttpServletRequest request,
            final WorkspaceRepository workspaceRepository
    ) {
        String workspaceId = getActiveWorkspaceIdOrDefault(request, workspaceRepository);
        if (workspaceId == null || workspaceId.trim().length() == 0) {
            throw new OpenLumifyException(VISALLO_WORKSPACE_ID_HEADER_NAME + " is a required header.");
        }
        return workspaceId;
    }

    protected static String getSourceGuid(final HttpServletRequest request) {
        return request.getHeader(VISALLO_SOURCE_GUID_HEADER_NAME);
    }

    public static String getOptionalParameter(final HttpServletRequest request, final String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");
        return getParameter(request, parameterName, true);
    }

    public static String[] getOptionalParameterArray(HttpServletRequest request, String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");

        return getParameterValues(request, parameterName, true);
    }

    public static EnumSet<FetchHint> getOptionalParameterFetchHints(
            HttpServletRequest request,
            String parameterName,
            EnumSet<FetchHint> defaultFetchHints
    ) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null) {
            return defaultFetchHints;
        }
        return EnumSet.copyOf(Lists.transform(Arrays.asList(val.split(",")), new Function<String, FetchHint>() {
            @Override
            public FetchHint apply(String input) {
                return FetchHint.valueOf(input);
            }
        }));
    }

    public static Integer getOptionalParameterInt(
            final HttpServletRequest request,
            final String parameterName,
            Integer defaultValue
    ) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null || val.length() == 0) {
            return defaultValue;
        }
        return Integer.parseInt(val);
    }

    public static String[] getOptionalParameterAsStringArray(
            final HttpServletRequest request,
            final String parameterName
    ) {
        Preconditions.checkNotNull(request, "The provided request was invalid");
        return getParameterValues(request, parameterName, true);
    }

    public static Float getOptionalParameterFloat(
            final HttpServletRequest request,
            final String parameterName,
            Float defaultValue
    ) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null || val.length() == 0) {
            return defaultValue;
        }
        return Float.parseFloat(val);
    }

    public static Double getOptionalParameterDouble(
            final HttpServletRequest request,
            final String parameterName,
            Double defaultValue
    ) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null || val.length() == 0) {
            return defaultValue;
        }
        return Double.parseDouble(val);
    }

    protected static String[] getParameterValues(
            final HttpServletRequest request,
            final String parameterName,
            final boolean optional
    ) {
        String[] paramValues = request.getParameterValues(parameterName);

        if (paramValues == null) {
            Object value = request.getAttribute(parameterName);
            if (value instanceof String[]) {
                paramValues = (String[]) value;
            }
        }

        if (paramValues == null) {
            if (!optional) {
                throw new RuntimeException(String.format("Parameter: '%s' is required in the request", parameterName));
            }
            return null;
        }

        return paramValues;
    }

    public static String[] getRequiredParameterArray(HttpServletRequest request, String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");
        return getParameterValues(request, parameterName, false);
    }

    public static String getRequiredParameter(final HttpServletRequest request, final String parameterName) {
        String result = getOptionalParameter(request, parameterName);
        if (result == null) {
            throw new OpenLumifyException("parameter " + parameterName + " is required");
        }
        return result;
    }

    protected static String getParameter(
            final HttpServletRequest request,
            final String parameterName,
            final boolean optional
    ) {
        String paramValue = request.getParameter(parameterName);
        if (paramValue == null) {
            Object paramValueObject = request.getAttribute(parameterName);
            if (paramValueObject != null) {
                paramValue = paramValueObject.toString();
            }
            if (paramValue == null) {
                if (!optional) {
                    throw new OpenLumifyException(String.format(
                            "Parameter: '%s' is required in the request",
                            parameterName
                    ));
                }
                return null;
            }
        }
        return paramValue;
    }

    protected WebApp getWebApp(HttpServletRequest request) {
        return (WebApp) App.getApp(request);
    }

    protected Locale getLocale(HttpServletRequest request) {
        String language = getOptionalParameter(request, LOCALE_LANGUAGE_PARAMETER);
        String country = getOptionalParameter(request, LOCALE_COUNTRY_PARAMETER);
        String variant = getOptionalParameter(request, LOCALE_VARIANT_PARAMETER);

        if (language != null) {
            return WebApp.getLocal(language, country, variant);
        }
        return request.getLocale();
    }

    protected ResourceBundle getBundle(HttpServletRequest request) {
        WebApp webApp = getWebApp(request);
        Locale locale = getLocale(request);
        return webApp.getBundle(locale);
    }

    protected String getTimeZone(final HttpServletRequest request) {
        String timeZone = (String) request.getAttribute(TIME_ZONE_ATTRIBUTE_NAME);
        if (timeZone == null || timeZone.trim().length() == 0) {
            timeZone = request.getHeader(VISALLO_TIME_ZONE_HEADER_NAME);
            if (timeZone == null || timeZone.trim().length() == 0) {
                timeZone = getOptionalParameter(request, TIME_ZONE_PARAMETER_NAME);
                if (timeZone == null || timeZone.trim().length() == 0) {
                    timeZone = this.configuration.get(
                            Configuration.DEFAULT_TIME_ZONE,
                            TimeZone.getDefault().getDisplayName()
                    );
                }
            }
        }
        return timeZone;
    }
}
