package com.fxdb.db.jdbc;

import org.junit.jupiter.api.Test;

import static com.fxdb.db.jdbc.SqlServerConnectionOptions.Authentication.ACTIVE_DIRECTORY_PASSWORD;
import static com.fxdb.db.jdbc.SqlServerConnectionOptions.Authentication.SQL_SERVER;
import static com.fxdb.db.jdbc.SqlServerConnectionOptions.Authentication.WINDOWS_INTEGRATED;
import static org.assertj.core.api.Assertions.assertThat;

class SqlServerConnectionOptionsTest {

    private static final String BASE_URL = "jdbc:sqlserver://localhost:1433;databaseName=mydb";

    @Test
    void sqlServerAuthenticationAddsNoExtraProperty() {
        var options = new SqlServerConnectionOptions(SQL_SERVER, true, false, null);
        assertThat(options.appendTo(BASE_URL))
                .isEqualTo(BASE_URL + ";encrypt=true;trustServerCertificate=false");
    }

    @Test
    void windowsIntegratedAddsIntegratedSecurity() {
        var options = new SqlServerConnectionOptions(WINDOWS_INTEGRATED, true, false, null);
        assertThat(options.appendTo(BASE_URL))
                .isEqualTo(BASE_URL + ";encrypt=true;trustServerCertificate=false;integratedSecurity=true");
    }

    @Test
    void activeDirectoryPasswordAddsAuthenticationProperty() {
        var options = new SqlServerConnectionOptions(ACTIVE_DIRECTORY_PASSWORD, false, true, null);
        assertThat(options.appendTo(BASE_URL))
                .isEqualTo(BASE_URL + ";encrypt=false;trustServerCertificate=true;authentication=ActiveDirectoryPassword");
    }

    @Test
    void additionalPropertiesAreAppendedVerbatim() {
        var options = new SqlServerConnectionOptions(SQL_SERVER, true, true, "instanceName=SQLEXPRESS;loginTimeout=30");
        assertThat(options.appendTo(BASE_URL))
                .isEqualTo(BASE_URL + ";encrypt=true;trustServerCertificate=true;instanceName=SQLEXPRESS;loginTimeout=30");
    }

    @Test
    void blankAdditionalPropertiesAreIgnored() {
        var options = new SqlServerConnectionOptions(SQL_SERVER, true, false, "   ");
        assertThat(options.appendTo(BASE_URL))
                .isEqualTo(BASE_URL + ";encrypt=true;trustServerCertificate=false");
    }
}
