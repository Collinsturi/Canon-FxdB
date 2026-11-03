package com.fxdb.db.jdbc;

/**
 * SQL Server-specific connection properties that go beyond plain host/port/database -
 * authentication mode, TLS behavior, and a free-text escape hatch for anything else
 * {@code mssql-jdbc} supports. Appended onto the base JDBC URL built by
 * {@link DriverRegistry#buildUrl}.
 */
public record SqlServerConnectionOptions(
        Authentication authentication,
        boolean encrypt,
        boolean trustServerCertificate,
        String additionalProperties) {

    public enum Authentication {
        SQL_SERVER("SQL Server Authentication"),
        WINDOWS_INTEGRATED("Windows Authentication (Integrated Security)"),
        ACTIVE_DIRECTORY_PASSWORD("Azure AD - Password");

        private final String displayName;

        Authentication(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public String appendTo(String baseUrl) {
        StringBuilder url = new StringBuilder(baseUrl);
        url.append(";encrypt=").append(encrypt);
        url.append(";trustServerCertificate=").append(trustServerCertificate);
        switch (authentication) {
            case WINDOWS_INTEGRATED -> url.append(";integratedSecurity=true");
            case ACTIVE_DIRECTORY_PASSWORD -> url.append(";authentication=ActiveDirectoryPassword");
            case SQL_SERVER -> {
                // No extra property - the driver's default, username/password used as-is.
            }
        }
        if (additionalProperties != null && !additionalProperties.isBlank()) {
            url.append(";").append(additionalProperties.strip());
        }
        return url.toString();
    }
}
