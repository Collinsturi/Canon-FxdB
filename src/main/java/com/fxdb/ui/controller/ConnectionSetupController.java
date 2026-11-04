package com.fxdb.ui.controller;

import com.fxdb.core.SchemaProvider;
import com.fxdb.db.jdbc.DatabaseProvisioner;
import com.fxdb.db.jdbc.DriverRegistry;
import com.fxdb.db.jdbc.JdbcConnectionConfig;
import com.fxdb.db.jdbc.SqlServerConnectionOptions;
import com.fxdb.file.FileConnectionConfig;
import com.fxdb.file.FileFormat;
import com.fxdb.ui.BackgroundTasks;
import com.fxdb.ui.MigrationWizardContext;
import com.fxdb.ui.Navigator;
import com.fxdb.ui.WizardScreen;
import com.fxdb.ui.onboarding.OnboardingTour;
import com.fxdb.ui.onboarding.TourStep;
import com.fxdb.ui.support.ConnectionSelection;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.nio.file.Path;
import java.util.List;

public class ConnectionSetupController implements WizardScreen {

    @FXML private ComboBox<String> sourceKind;
    @FXML private VBox sourceDbBox;
    @FXML private VBox sourceFileBox;
    @FXML private ComboBox<DriverRegistry> sourceProvider;
    @FXML private VBox sourceServerBox;
    @FXML private TextField sourceHost;
    @FXML private TextField sourcePort;
    @FXML private TextField sourceDatabase;
    @FXML private HBox sourceSqliteBox;
    @FXML private TextField sourceSqlitePath;
    @FXML private VBox sourceSqlServerBox;
    @FXML private ComboBox<SqlServerConnectionOptions.Authentication> sourceSqlServerAuth;
    @FXML private CheckBox sourceSqlServerEncrypt;
    @FXML private CheckBox sourceSqlServerTrustCert;
    @FXML private TextField sourceSqlServerExtraProps;
    @FXML private CheckBox sourceAdvancedUrl;
    @FXML private TextField sourceJdbcUrl;
    @FXML private TextField sourceUsername;
    @FXML private PasswordField sourcePassword;
    @FXML private TextField sourceFilePath;
    @FXML private ComboBox<FileFormat> sourceFormat;
    @FXML private TextField sourceDelimiter;
    @FXML private CheckBox sourceHasHeader;
    @FXML private TextField sourceSheetName;
    @FXML private Label sourceStatus;
    @FXML private ScrollPane sourceTableListScroll;
    @FXML private FlowPane sourceTableList;

    @FXML private ComboBox<String> targetKind;
    @FXML private VBox targetDbBox;
    @FXML private VBox targetFileBox;
    @FXML private ComboBox<DriverRegistry> targetProvider;
    @FXML private VBox targetServerBox;
    @FXML private TextField targetHost;
    @FXML private TextField targetPort;
    @FXML private TextField targetDatabase;
    @FXML private HBox targetSqliteBox;
    @FXML private TextField targetSqlitePath;
    @FXML private VBox targetSqlServerBox;
    @FXML private ComboBox<SqlServerConnectionOptions.Authentication> targetSqlServerAuth;
    @FXML private CheckBox targetSqlServerEncrypt;
    @FXML private CheckBox targetSqlServerTrustCert;
    @FXML private TextField targetSqlServerExtraProps;
    @FXML private CheckBox targetAdvancedUrl;
    @FXML private TextField targetJdbcUrl;
    @FXML private TextField targetUsername;
    @FXML private PasswordField targetPassword;
    @FXML private TextField targetFilePath;
    @FXML private ComboBox<FileFormat> targetFormat;
    @FXML private TextField targetDelimiter;
    @FXML private CheckBox targetHasHeader;
    @FXML private TextField targetSheetName;
    @FXML private Label targetStatus;
    @FXML private Button targetCreateDatabaseButton;
    @FXML private ScrollPane targetTableListScroll;
    @FXML private FlowPane targetTableList;

    private Navigator navigator;
    private boolean sourcePortTouchedByUser;
    private boolean targetPortTouchedByUser;
    private boolean settingPortProgrammatically;

    @Override
    public void init(MigrationWizardContext context, Navigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        sourceKind.setItems(javafx.collections.FXCollections.observableArrayList("Database", "File"));
        targetKind.setItems(javafx.collections.FXCollections.observableArrayList("Database", "File"));
        sourceFormat.setItems(javafx.collections.FXCollections.observableArrayList(FileFormat.values()));
        targetFormat.setItems(javafx.collections.FXCollections.observableArrayList(FileFormat.values()));

        sourceKind.getSelectionModel().select("Database");
        targetKind.getSelectionModel().select("Database");
        sourceFormat.getSelectionModel().select(FileFormat.CSV);
        targetFormat.getSelectionModel().select(FileFormat.CSV);

        bindKindVisibility(sourceKind, sourceDbBox, sourceFileBox);
        bindKindVisibility(targetKind, targetDbBox, targetFileBox);

        sourceProvider.setItems(javafx.collections.FXCollections.observableArrayList(DriverRegistry.values()));
        targetProvider.setItems(javafx.collections.FXCollections.observableArrayList(DriverRegistry.values()));

        sourceSqlServerAuth.setItems(javafx.collections.FXCollections.observableArrayList(
                SqlServerConnectionOptions.Authentication.values()));
        targetSqlServerAuth.setItems(javafx.collections.FXCollections.observableArrayList(
                SqlServerConnectionOptions.Authentication.values()));
        sourceSqlServerAuth.getSelectionModel().select(SqlServerConnectionOptions.Authentication.SQL_SERVER);
        targetSqlServerAuth.getSelectionModel().select(SqlServerConnectionOptions.Authentication.SQL_SERVER);
        sourceSqlServerEncrypt.setSelected(true);
        targetSqlServerEncrypt.setSelected(true);

        bindSimpleUrlBuilder(true);
        bindSimpleUrlBuilder(false);

        sourceProvider.getSelectionModel().select(DriverRegistry.POSTGRES);
        targetProvider.getSelectionModel().select(DriverRegistry.POSTGRES);
    }

    /**
     * Wires the provider dropdown + host/port/database (or SQLite path) fields to
     * recompute the JDBC URL field live, and shows/hides those fields based on the
     * selected provider and whether "enter full JDBC URL manually" is checked.
     */
    private void bindSimpleUrlBuilder(boolean isSource) {
        ComboBox<DriverRegistry> provider = isSource ? sourceProvider : targetProvider;
        TextField port = isSource ? sourcePort : targetPort;
        CheckBox advancedUrl = isSource ? sourceAdvancedUrl : targetAdvancedUrl;

        provider.valueProperty().addListener((obs, old, value) -> {
            boolean portTouched = isSource ? sourcePortTouchedByUser : targetPortTouchedByUser;
            if (value != null && !value.isFileBased() && !portTouched) {
                settingPortProgrammatically = true;
                port.setText(String.valueOf(value.defaultPort()));
                settingPortProgrammatically = false;
            }
            updateDbFieldVisibility(isSource);
        });
        (isSource ? sourceHost : targetHost).textProperty().addListener((obs, old, val) -> recomputeUrl(isSource));
        port.textProperty().addListener((obs, old, val) -> {
            if (!settingPortProgrammatically) {
                if (isSource) {
                    sourcePortTouchedByUser = true;
                } else {
                    targetPortTouchedByUser = true;
                }
            }
            recomputeUrl(isSource);
        });
        (isSource ? sourceDatabase : targetDatabase).textProperty().addListener((obs, old, val) -> recomputeUrl(isSource));
        (isSource ? sourceSqlitePath : targetSqlitePath).textProperty().addListener((obs, old, val) -> recomputeUrl(isSource));
        (isSource ? sourceSqlServerAuth : targetSqlServerAuth).valueProperty().addListener((obs, old, val) -> recomputeUrl(isSource));
        (isSource ? sourceSqlServerEncrypt : targetSqlServerEncrypt).selectedProperty().addListener((obs, old, val) -> recomputeUrl(isSource));
        (isSource ? sourceSqlServerTrustCert : targetSqlServerTrustCert).selectedProperty().addListener((obs, old, val) -> recomputeUrl(isSource));
        (isSource ? sourceSqlServerExtraProps : targetSqlServerExtraProps).textProperty().addListener((obs, old, val) -> recomputeUrl(isSource));
        advancedUrl.selectedProperty().addListener((obs, old, val) -> updateDbFieldVisibility(isSource));
    }

    private void updateDbFieldVisibility(boolean isSource) {
        ComboBox<DriverRegistry> provider = isSource ? sourceProvider : targetProvider;
        VBox serverBox = isSource ? sourceServerBox : targetServerBox;
        HBox sqliteBox = isSource ? sourceSqliteBox : targetSqliteBox;
        VBox sqlServerBox = isSource ? sourceSqlServerBox : targetSqlServerBox;
        CheckBox advancedUrl = isSource ? sourceAdvancedUrl : targetAdvancedUrl;
        TextField jdbcUrl = isSource ? sourceJdbcUrl : targetJdbcUrl;

        boolean advanced = advancedUrl.isSelected();
        boolean fileBased = provider.getValue() != null && provider.getValue().isFileBased();
        boolean showServer = !advanced && !fileBased;
        boolean showSqlite = !advanced && fileBased;
        boolean showSqlServerOptions = showServer && provider.getValue() == DriverRegistry.SQLSERVER;

        serverBox.setVisible(showServer);
        serverBox.setManaged(showServer);
        sqliteBox.setVisible(showSqlite);
        sqliteBox.setManaged(showSqlite);
        sqlServerBox.setVisible(showSqlServerOptions);
        sqlServerBox.setManaged(showSqlServerOptions);
        jdbcUrl.setEditable(advanced);

        if (!isSource) {
            boolean showCreateDatabase = showServer && provider.getValue() != null
                    && provider.getValue().dialect().supportsDatabaseCreation();
            targetCreateDatabaseButton.setVisible(showCreateDatabase);
            targetCreateDatabaseButton.setManaged(showCreateDatabase);
        }

        recomputeUrl(isSource);
    }

    /**
     * Rebuilds the JDBC URL field from the simple fields - unless "enter full JDBC URL
     * manually" is checked, in which case the user's own typed URL is left untouched.
     */
    private void recomputeUrl(boolean isSource) {
        CheckBox advancedUrl = isSource ? sourceAdvancedUrl : targetAdvancedUrl;
        if (advancedUrl.isSelected()) {
            return;
        }

        DriverRegistry selected = (isSource ? sourceProvider : targetProvider).getValue();
        TextField jdbcUrl = isSource ? sourceJdbcUrl : targetJdbcUrl;
        if (selected == null) {
            return;
        }

        if (selected.isFileBased()) {
            String path = (isSource ? sourceSqlitePath : targetSqlitePath).getText();
            jdbcUrl.setText(path == null || path.isBlank() ? "" : DriverRegistry.buildSqliteUrl(path));
            return;
        }

        String host = (isSource ? sourceHost : targetHost).getText();
        String database = (isSource ? sourceDatabase : targetDatabase).getText();
        if (host == null || host.isBlank() || database == null || database.isBlank()) {
            jdbcUrl.setText("");
            return;
        }

        String portText = (isSource ? sourcePort : targetPort).getText();
        int port;
        try {
            port = Integer.parseInt(portText.trim());
        } catch (NumberFormatException | NullPointerException e) {
            port = selected.defaultPort();
        }

        String url = selected.buildUrl(host, port, database);
        if (selected == DriverRegistry.SQLSERVER) {
            url = buildSqlServerOptions(isSource).appendTo(url);
        }
        jdbcUrl.setText(url);
    }

    private SqlServerConnectionOptions buildSqlServerOptions(boolean isSource) {
        var authentication = (isSource ? sourceSqlServerAuth : targetSqlServerAuth).getValue();
        boolean encrypt = (isSource ? sourceSqlServerEncrypt : targetSqlServerEncrypt).isSelected();
        boolean trustServerCertificate = (isSource ? sourceSqlServerTrustCert : targetSqlServerTrustCert).isSelected();
        String extraProps = blankToNull((isSource ? sourceSqlServerExtraProps : targetSqlServerExtraProps).getText());
        return new SqlServerConnectionOptions(
                authentication == null ? SqlServerConnectionOptions.Authentication.SQL_SERVER : authentication,
                encrypt, trustServerCertificate, extraProps);
    }

    private void bindKindVisibility(ComboBox<String> kind, VBox dbBox, VBox fileBox) {
        kind.valueProperty().addListener((obs, old, value) -> {
            boolean isDb = "Database".equals(value);
            dbBox.setVisible(isDb);
            dbBox.setManaged(isDb);
            fileBox.setVisible(!isDb);
            fileBox.setManaged(!isDb);
        });
        dbBox.setVisible(true);
        dbBox.setManaged(true);
        fileBox.setVisible(false);
        fileBox.setManaged(false);
    }

    @FXML
    private void onShowTour() {
        javafx.stage.Stage stage = (javafx.stage.Stage) sourceStatus.getScene().getWindow();
        OnboardingTour.show(stage, TourStep.wizardOverview(), () -> {
        });
    }

    @FXML
    private void onBrowseSourceFile() {
        browseFile(sourceFilePath, sourceFormat);
    }

    @FXML
    private void onBrowseTargetFile() {
        browseFile(targetFilePath, targetFormat);
    }

    private void browseFile(TextField pathField, ComboBox<FileFormat> formatBox) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select file");
        var file = chooser.showOpenDialog(pathField.getScene().getWindow());
        if (file == null) {
            return;
        }
        pathField.setText(file.getAbsolutePath());
        String name = file.getName().toLowerCase();
        if (name.endsWith(".csv")) {
            formatBox.getSelectionModel().select(FileFormat.CSV);
        } else if (name.endsWith(".json")) {
            formatBox.getSelectionModel().select(FileFormat.JSON);
        } else if (name.endsWith(".xlsx")) {
            formatBox.getSelectionModel().select(FileFormat.EXCEL);
        }
    }

    @FXML
    private void onBrowseSourceSqlite() {
        browseSqliteFile(sourceSqlitePath);
    }

    @FXML
    private void onBrowseTargetSqlite() {
        browseSqliteFile(targetSqlitePath);
    }

    private void browseSqliteFile(TextField pathField) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select SQLite database file");
        var file = chooser.showOpenDialog(pathField.getScene().getWindow());
        if (file == null) {
            return;
        }
        pathField.setText(file.getAbsolutePath());
    }

    @FXML
    private void onTestSource() {
        testConnection(true);
    }

    @FXML
    private void onTestTarget() {
        testConnection(false);
    }

    private void testConnection(boolean isSource) {
        Label status = isSource ? sourceStatus : targetStatus;
        ScrollPane tableListScroll = isSource ? sourceTableListScroll : targetTableListScroll;
        FlowPane tableList = isSource ? sourceTableList : targetTableList;
        tableList.getChildren().clear();
        setTableListVisible(tableListScroll, false);

        ConnectionSelection selection;
        try {
            selection = buildSelection(isSource);
        } catch (Exception e) {
            status.setText("Failed: " + e.getMessage());
            status.setStyle("-fx-text-fill: red;");
            return;
        }

        BackgroundTasks.run(navigator, "Testing connection...",
                () -> {
                    try (SchemaProvider provider = selection.createSchemaProvider()) {
                        return provider.listTables();
                    }
                },
                tables -> {
                    status.setText("Connected successfully - " + tables.size() + " table(s) found");
                    status.setStyle("-fx-text-fill: green;");
                    tables.stream().sorted().forEach(table -> tableList.getChildren().add(tableChip(table)));
                    setTableListVisible(tableListScroll, !tables.isEmpty());
                },
                ex -> {
                    status.setText("Failed: " + ex.getMessage());
                    status.setStyle("-fx-text-fill: red;");
                });
    }

    @FXML
    private void onCreateTargetDatabase() {
        DriverRegistry provider = targetProvider.getValue();
        String host = targetHost.getText();
        String database = targetDatabase.getText();
        String username = blankToNull(targetUsername.getText());
        String password = blankToNull(targetPassword.getText());

        if (provider == null || host == null || host.isBlank() || database == null || database.isBlank()) {
            targetStatus.setText("Failed: host and database are required");
            targetStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        int parsedPort;
        try {
            parsedPort = Integer.parseInt(targetPort.getText().trim());
        } catch (NumberFormatException | NullPointerException e) {
            parsedPort = provider.defaultPort();
        }

        // Same pattern recomputeUrl uses for the real target URL - vendor-specific extras
        // (SQL Server's encrypt/trustServerCertificate/authentication) have to carry over to
        // this admin connection too, or it can fail even when the real target connection works.
        String adminUrl = provider.buildUrl(host, parsedPort, provider.dialect().adminDatabase());
        if (provider == DriverRegistry.SQLSERVER) {
            adminUrl = buildSqlServerOptions(false).appendTo(adminUrl);
        }
        String finalAdminUrl = adminUrl;

        BackgroundTasks.run(navigator, "Creating database...",
                () -> DatabaseProvisioner.ensureDatabaseExists(provider, finalAdminUrl, database, username, password),
                created -> {
                    targetStatus.setText("Database '" + database + "' "
                            + (created ? "created" : "already exists"));
                    targetStatus.setStyle("-fx-text-fill: green;");
                },
                ex -> {
                    targetStatus.setText("Failed: " + ex.getMessage());
                    targetStatus.setStyle("-fx-text-fill: red;");
                });
    }

    private void setTableListVisible(ScrollPane scrollPane, boolean visible) {
        scrollPane.setVisible(visible);
        scrollPane.setManaged(visible);
    }

    private Label tableChip(String tableName) {
        Label chip = new Label(tableName);
        chip.setStyle("-fx-background-color: #e8e8e8; -fx-background-radius: 10; "
                + "-fx-padding: 2 8 2 8; -fx-font-size: 11px;");
        return chip;
    }

    @FXML
    private void onNext() {
        try {
            ConnectionSelection source = buildSelection(true);
            ConnectionSelection target = buildSelection(false);
            navigator.context().setSource(source);
            navigator.context().setTarget(target);
            navigator.showTableSelection();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not build connection: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private ConnectionSelection buildSelection(boolean isSource) {
        String kind = isSource ? sourceKind.getValue() : targetKind.getValue();
        if ("Database".equals(kind)) {
            String url = isSource ? sourceJdbcUrl.getText() : targetJdbcUrl.getText();
            String username = isSource ? sourceUsername.getText() : targetUsername.getText();
            String password = isSource ? sourcePassword.getText() : targetPassword.getText();
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("JDBC URL is required");
            }
            return new ConnectionSelection.Jdbc(new JdbcConnectionConfig(url,
                    blankToNull(username), blankToNull(password)));
        }

        String pathText = isSource ? sourceFilePath.getText() : targetFilePath.getText();
        if (pathText == null || pathText.isBlank()) {
            throw new IllegalArgumentException("File path is required");
        }
        FileFormat format = (isSource ? sourceFormat : targetFormat).getValue();
        char delimiter = parseDelimiter(isSource ? sourceDelimiter.getText() : targetDelimiter.getText());
        boolean hasHeader = (isSource ? sourceHasHeader : targetHasHeader).isSelected();
        String sheetName = blankToNull(isSource ? sourceSheetName.getText() : targetSheetName.getText());

        return new ConnectionSelection.File(new FileConnectionConfig(Path.of(pathText), format, delimiter, hasHeader, sheetName));
    }

    private char parseDelimiter(String text) {
        return (text == null || text.isBlank()) ? ',' : text.charAt(0);
    }

    private String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text;
    }
}
