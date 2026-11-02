package com.fxdb.ui.controller;

import com.fxdb.core.SchemaProvider;
import com.fxdb.db.jdbc.JdbcConnectionConfig;
import com.fxdb.file.FileConnectionConfig;
import com.fxdb.file.FileFormat;
import com.fxdb.ui.MigrationWizardContext;
import com.fxdb.ui.Navigator;
import com.fxdb.ui.WizardScreen;
import com.fxdb.ui.onboarding.OnboardingTour;
import com.fxdb.ui.onboarding.TourStep;
import com.fxdb.ui.support.ConnectionSelection;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.nio.file.Path;
import java.util.List;

public class ConnectionSetupController implements WizardScreen {

    @FXML private ComboBox<String> sourceKind;
    @FXML private VBox sourceDbBox;
    @FXML private VBox sourceFileBox;
    @FXML private TextField sourceJdbcUrl;
    @FXML private TextField sourceUsername;
    @FXML private PasswordField sourcePassword;
    @FXML private TextField sourceFilePath;
    @FXML private ComboBox<FileFormat> sourceFormat;
    @FXML private TextField sourceDelimiter;
    @FXML private CheckBox sourceHasHeader;
    @FXML private TextField sourceSheetName;
    @FXML private Label sourceStatus;

    @FXML private ComboBox<String> targetKind;
    @FXML private VBox targetDbBox;
    @FXML private VBox targetFileBox;
    @FXML private TextField targetJdbcUrl;
    @FXML private TextField targetUsername;
    @FXML private PasswordField targetPassword;
    @FXML private TextField targetFilePath;
    @FXML private ComboBox<FileFormat> targetFormat;
    @FXML private TextField targetDelimiter;
    @FXML private CheckBox targetHasHeader;
    @FXML private TextField targetSheetName;
    @FXML private Label targetStatus;

    private Navigator navigator;

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
    private void onTestSource() {
        testConnection(true);
    }

    @FXML
    private void onTestTarget() {
        testConnection(false);
    }

    private void testConnection(boolean isSource) {
        Label status = isSource ? sourceStatus : targetStatus;
        try {
            ConnectionSelection selection = buildSelection(isSource);
            try (SchemaProvider provider = selection.createSchemaProvider()) {
                List<String> tables = provider.listTables();
                status.setText("OK - found " + tables.size() + " table(s): " + String.join(", ", tables));
                status.setStyle("-fx-text-fill: green;");
            }
        } catch (Exception e) {
            status.setText("Failed: " + e.getMessage());
            status.setStyle("-fx-text-fill: red;");
        }
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
