package com.voiceai.ui;

import com.voiceai.constant.UIConstants;
import com.voiceai.service.ChangelogService;
import com.voiceai.service.ChangelogService.ChangelogVersion;
import com.voiceai.service.ChangelogService.ChangelogUpdate;
import com.voiceai.util.AppVersion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

/**
 * About dialog displaying application information, author details, and links
 */
public class AboutDialog {

    private static final Logger logger = Logger.getLogger(AboutDialog.class.getName());

    private static final String APP_DESCRIPTION =
            "StorieS Maker è una applicazione di trascrizione vocale e chat AI. " +
                    "Registra l'audio dal tuo microfono o dall'audio di sistema, ottieni trascrizioni in tempo reale " +
                    "utilizzando OpenAI Whisper e interagisci con la chat integrata per elaborare le tue trascrizioni.";

    private static final String AUTHOR_NAME = "Ruben";
    private static final String AUTHOR_SURNAME = "Garruto";
    private static final String GITHUB_URL = "https://github.com/rubenGarrutoDeveloper/stories-maker-javafx";

    private static final String AUTHOR_PHOTO_PATH = "/author-photo.jpg";
    private static final int PHOTO_SIZE = 150;
    private static final int DIALOG_WIDTH = 700;
    private static final double DIALOG_MAX_HEIGHT_RATIO = 0.85; // 85% of screen height
    private static final int WHATS_NEW_MAX_VERSIONS = 1;

    private final Stage ownerStage;
    private Dialog<Void> dialog;
    private final ChangelogService changelogService;

    /**
     * Creates a new AboutDialog
     *
     * @param ownerStage the parent stage
     */
    public AboutDialog(Stage ownerStage) {
        this.ownerStage = ownerStage;
        this.changelogService = new ChangelogService();
    }

    /**
     * Shows the About dialog
     */
    public void show() {
        dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ownerStage);
        dialog.setTitle("About " + UIConstants.APP_TITLE);
        dialog.setResizable(false);

        // Get screen bounds to constrain dialog size
        javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
        javafx.geometry.Rectangle2D screenBounds = screen.getVisualBounds();
        double maxHeight = screenBounds.getHeight() * DIALOG_MAX_HEIGHT_RATIO;

        // Create dialog content
        VBox content = createDialogContent();

        // Wrap content in ScrollPane to handle overflow
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(maxHeight);
        scrollPane.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background: white;"
        );
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefWidth(DIALOG_WIDTH);
        dialog.getDialogPane().setMaxHeight(maxHeight);

        // Style the dialog pane
        dialog.getDialogPane().setStyle(
                "-fx-background-color: white; " +
                        "-fx-padding: 0;"
        );

        // Add close button
        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);

        // Style the close button
        Button closeButton = (Button) dialog.getDialogPane().lookupButton(closeButtonType);
        if (closeButton != null) {
            closeButton.setStyle(UIConstants.PRIMARY_BUTTON_STYLE);
        }

        logger.info("Showing About dialog");
        dialog.showAndWait();
    }

    /**
     * Creates the main dialog content
     */
    private VBox createDialogContent() {
        VBox mainContainer = new VBox();
        mainContainer.setSpacing(0);

        // Header section with gradient background
        VBox header = createHeader();

        // Content section with author and app info
        HBox contentSection = createContentSection();

        // What's New section
        VBox whatsNewSection = createWhatsNewSection();

        // Footer with GitHub link
        VBox footer = createFooter();

        mainContainer.getChildren().addAll(header, contentSection, whatsNewSection, footer);

        return mainContainer;
    }

    /**
     * Creates the header section with app title and version
     */
    private VBox createHeader() {
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(25, 20, 25, 20));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, " +
                        UIConstants.PRIMARY_COLOR + ", " +
                        UIConstants.SECONDARY_COLOR + ");"
        );

        Label titleLabel = new Label(UIConstants.APP_TITLE);
        titleLabel.setStyle(
                "-fx-font-size: 28px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white;"
        );

        Label versionLabel = new Label("Version " + AppVersion.VERSION);
        versionLabel.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-text-fill: white; " +
                        "-fx-opacity: 0.9;"
        );

        header.getChildren().addAll(titleLabel, versionLabel);

        return header;
    }

    /**
     * Creates the main content section with author photo and information
     */
    private HBox createContentSection() {
        HBox contentBox = new HBox(30);
        contentBox.setPadding(new Insets(25, 30, 15, 30));
        contentBox.setAlignment(Pos.TOP_LEFT);

        // Left side - Author photo
        VBox photoSection = createPhotoSection();

        // Right side - Information
        VBox infoSection = createInfoSection();

        contentBox.getChildren().addAll(photoSection, infoSection);

        return contentBox;
    }

    /**
     * Creates the photo section with author photo
     */
    private VBox createPhotoSection() {
        VBox photoBox = new VBox(10);
        photoBox.setAlignment(Pos.TOP_CENTER);
        photoBox.setPrefWidth(PHOTO_SIZE + 20);

        try {
            // Load the author photo at original size
            Image authorImage = new Image(
                    getClass().getResourceAsStream(AUTHOR_PHOTO_PATH)
            );

            // Calculate the crop dimensions to make it square (centered)
            double imgWidth = authorImage.getWidth();
            double imgHeight = authorImage.getHeight();
            double cropSize = Math.min(imgWidth, imgHeight);
            double xOffset = (imgWidth - cropSize) / 2.0;
            double yOffset = (imgHeight - cropSize) / 2.0;

            ImageView photoView = new ImageView(authorImage);

            // Set viewport to crop the center square of the image
            photoView.setViewport(new javafx.geometry.Rectangle2D(
                    xOffset,
                    yOffset,
                    cropSize,
                    cropSize
            ));

            // Now scale to desired size
            photoView.setFitWidth(PHOTO_SIZE);
            photoView.setFitHeight(PHOTO_SIZE);
            photoView.setPreserveRatio(true);
            photoView.setSmooth(true);

            // Container for the image
            StackPane imageContainer = new StackPane(photoView);
            imageContainer.setMaxSize(PHOTO_SIZE, PHOTO_SIZE);
            imageContainer.setMinSize(PHOTO_SIZE, PHOTO_SIZE);
            imageContainer.setPrefSize(PHOTO_SIZE, PHOTO_SIZE);

            // Create circular clip
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(
                    PHOTO_SIZE / 2.0,
                    PHOTO_SIZE / 2.0,
                    PHOTO_SIZE / 2.0
            );
            imageContainer.setClip(clip);

            // Create outer container with circular border and shadow
            StackPane photoContainer = new StackPane(imageContainer);
            photoContainer.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-background-radius: " + (PHOTO_SIZE / 2.0) + "px; " +
                            "-fx-border-radius: " + (PHOTO_SIZE / 2.0) + "px; " +
                            "-fx-border-color: " + UIConstants.BORDER_COLOR + "; " +
                            "-fx-border-width: 3px; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);"
            );
            photoContainer.setMaxSize(PHOTO_SIZE, PHOTO_SIZE);
            photoContainer.setMinSize(PHOTO_SIZE, PHOTO_SIZE);

            photoBox.getChildren().add(photoContainer);

        } catch (Exception e) {
            logger.warning("Could not load author photo: " + e.getMessage());

            // Fallback: Show placeholder with initials
            StackPane placeholder = createPhotoPlaceholder();
            photoBox.getChildren().add(placeholder);
        }

        return photoBox;
    }

    /**
     * Creates a placeholder for the author photo
     */
    private StackPane createPhotoPlaceholder() {
        StackPane placeholder = new StackPane();
        placeholder.setPrefSize(PHOTO_SIZE, PHOTO_SIZE);
        placeholder.setMaxSize(PHOTO_SIZE, PHOTO_SIZE);
        placeholder.setMinSize(PHOTO_SIZE, PHOTO_SIZE);
        placeholder.setStyle(
                "-fx-background-color: " + UIConstants.PRIMARY_COLOR + "; " +
                        "-fx-background-radius: 75px; " +
                        "-fx-border-radius: 75px; " +
                        "-fx-border-color: " + UIConstants.BORDER_COLOR + "; " +
                        "-fx-border-width: 3px;"
        );

        // Get initials
        String initials = "";
        if (!AUTHOR_NAME.isEmpty()) {
            initials += AUTHOR_NAME.charAt(0);
        }
        if (!AUTHOR_SURNAME.isEmpty()) {
            initials += AUTHOR_SURNAME.charAt(0);
        }

        Label initialsLabel = new Label(initials.toUpperCase());
        initialsLabel.setStyle(
                "-fx-font-size: 48px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white;"
        );

        placeholder.getChildren().add(initialsLabel);

        return placeholder;
    }

    /**
     * Creates the information section with author details and description
     */
    private VBox createInfoSection() {
        VBox infoBox = new VBox(20);
        infoBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Author name section
        VBox authorSection = new VBox(5);

        Label authorLabel = new Label("Developed by");
        authorLabel.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: " + UIConstants.GRAY_COLOR + "; " +
                        "-fx-font-weight: bold;"
        );

        Label authorNameLabel = new Label(AUTHOR_NAME + " " + AUTHOR_SURNAME);
        authorNameLabel.setStyle(
                "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: " + UIConstants.PRIMARY_COLOR + ";"
        );

        authorSection.getChildren().addAll(authorLabel, authorNameLabel);

        // Description section
        VBox descriptionSection = new VBox(5);

        Label descriptionLabel = new Label("About");
        descriptionLabel.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: " + UIConstants.GRAY_COLOR + "; " +
                        "-fx-font-weight: bold;"
        );

        TextFlow descriptionText = new TextFlow();
        Text description = new Text(APP_DESCRIPTION);
        description.setStyle(
                "-fx-font-size: 13px; " +
                        "-fx-fill: #333;"
        );
        descriptionText.getChildren().add(description);
        descriptionText.setTextAlignment(TextAlignment.JUSTIFY);
        descriptionText.setMaxWidth(400);

        descriptionSection.getChildren().addAll(descriptionLabel, descriptionText);

        // Features list
        VBox featuresSection = createFeaturesSection();

        infoBox.getChildren().addAll(authorSection, descriptionSection, featuresSection);

        return infoBox;
    }

    /**
     * Creates the features section
     */
    private VBox createFeaturesSection() {
        VBox featuresBox = new VBox(5);

        Label featuresLabel = new Label("Key Features");
        featuresLabel.setStyle(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: " + UIConstants.GRAY_COLOR + "; " +
                        "-fx-font-weight: bold;"
        );

        VBox featuresList = new VBox(3);

        String[] features = {
                "✓ Trascrizione audio in tempo reale",
                "✓ Registrazione audio di sistema e microfono",
                "✓ Integrazione ChatGPT",
                "✓ Supporto per sorgenti audio multiple"
        };

        for (String feature : features) {
            Label featureLabel = new Label(feature);
            featureLabel.setStyle(
                    "-fx-font-size: 12px; " +
                            "-fx-text-fill: #555;"
            );
            featuresList.getChildren().add(featureLabel);
        }

        featuresBox.getChildren().addAll(featuresLabel, featuresList);

        return featuresBox;
    }

    /**
     * Creates the What's New section with recent updates
     */
    private VBox createWhatsNewSection() {
        VBox whatsNewBox = new VBox(15);
        whatsNewBox.setPadding(new Insets(15, 30, 15, 30));
        whatsNewBox.setStyle(
                "-fx-background-color: #f9fafb; " +
                        "-fx-border-color: " + UIConstants.BORDER_COLOR + "; " +
                        "-fx-border-width: 1 0 1 0;"
        );

        // Section header
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label headerLabel = new Label("🎉 Novità");
        headerLabel.setStyle(
                "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: " + UIConstants.PRIMARY_COLOR + ";"
        );

        headerBox.getChildren().add(headerLabel);

        // Check if changelog is available
        if (!changelogService.isChangelogAvailable()) {
            Label noChangelogLabel = new Label("Nessuna informazione disponibile sul changelog.");
            noChangelogLabel.setStyle(
                    "-fx-font-size: 12px; " +
                            "-fx-text-fill: " + UIConstants.GRAY_COLOR + "; " +
                            "-fx-font-style: italic;"
            );
            whatsNewBox.getChildren().addAll(headerBox, noChangelogLabel);
            return whatsNewBox;
        }

        // Load latest versions
        List<ChangelogVersion> latestVersions = changelogService.getLatestVersions(WHATS_NEW_MAX_VERSIONS);

        if (latestVersions.isEmpty()) {
            Label noUpdatesLabel = new Label("Nessun aggiornamento disponibile.");
            noUpdatesLabel.setStyle(
                    "-fx-font-size: 12px; " +
                            "-fx-text-fill: " + UIConstants.GRAY_COLOR + "; " +
                            "-fx-font-style: italic;"
            );
            whatsNewBox.getChildren().addAll(headerBox, noUpdatesLabel);
            return whatsNewBox;
        }

        // Create container for the latest version only
        VBox versionsContainer = new VBox(15);

        // Show only the first (latest) version
        if (!latestVersions.isEmpty()) {
            VBox versionBox = createVersionBox(latestVersions.get(0));
            versionsContainer.getChildren().add(versionBox);
        }

        whatsNewBox.getChildren().addAll(headerBox, versionsContainer);

        return whatsNewBox;
    }

    /**
     * Creates a box for a single version with its updates
     */
    private VBox createVersionBox(ChangelogVersion version) {
        VBox versionBox = new VBox(8);
        versionBox.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: " + UIConstants.BORDER_COLOR + "; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-padding: 12px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 1);"
        );

        // Version header
        HBox versionHeader = new HBox(10);
        versionHeader.setAlignment(Pos.CENTER_LEFT);

        Label versionLabel = new Label("v" + version.getVersion());
        versionLabel.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: " + UIConstants.SECONDARY_COLOR + ";"
        );

        Label dateLabel = new Label(version.getFormattedDate());
        dateLabel.setStyle(
                "-fx-font-size: 11px; " +
                        "-fx-text-fill: " + UIConstants.GRAY_COLOR + "; " +
                        "-fx-padding: 2px 8px; " +
                        "-fx-background-color: " + UIConstants.LIGHT_GRAY_COLOR + "; " +
                        "-fx-background-radius: 10px;"
        );

        versionHeader.getChildren().addAll(versionLabel, dateLabel);

        // Updates list
        VBox updatesList = new VBox(5);

        for (ChangelogUpdate update : version.getUpdates()) {
            HBox updateBox = new HBox(8);
            updateBox.setAlignment(Pos.TOP_LEFT);

            Label iconLabel = new Label(update.getIcon());
            iconLabel.setStyle("-fx-font-size: 14px;");
            iconLabel.setMinWidth(20);

            Label updateLabel = new Label(update.getDescription());
            updateLabel.setStyle(
                    "-fx-font-size: 12px; " +
                            "-fx-text-fill: #333; " +
                            "-fx-wrap-text: true;"
            );
            updateLabel.setWrapText(true);
            updateLabel.setMaxWidth(550);

            updateBox.getChildren().addAll(iconLabel, updateLabel);
            updatesList.getChildren().add(updateBox);
        }

        versionBox.getChildren().addAll(versionHeader, updatesList);

        return versionBox;
    }

    /**
     * Creates the footer section with GitHub link
     */
    private VBox createFooter() {
        VBox footer = new VBox(10);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(15, 20, 15, 20));
        footer.setStyle(
                "-fx-background-color: " + UIConstants.BACKGROUND_COLOR + "; " +
                        "-fx-border-color: " + UIConstants.BORDER_COLOR + "; " +
                        "-fx-border-width: 1 0 0 0;"
        );

        // GitHub link
        Hyperlink githubLink = new Hyperlink("🔗 View on GitHub");
        githubLink.setStyle(
                "-fx-font-size: 13px; " +
                        "-fx-text-fill: " + UIConstants.INFO_COLOR + "; " +
                        "-fx-underline: true;"
        );

        githubLink.setOnAction(e -> openWebpage(GITHUB_URL));

        // Copyright
        Label copyrightLabel = new Label("© " + java.time.Year.now().getValue() + " " +
                AUTHOR_NAME + " " + AUTHOR_SURNAME + ". All rights reserved.");
        copyrightLabel.setStyle(
                "-fx-font-size: 11px; " +
                        "-fx-text-fill: " + UIConstants.GRAY_COLOR + ";"
        );

        footer.getChildren().addAll(githubLink, copyrightLabel);

        return footer;
    }

    /**
     * Opens a webpage in the default browser
     *
     * @param url the URL to open
     */
    private void openWebpage(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                logger.info("Opened URL: " + url);
            } else {
                logger.warning("Desktop browsing not supported");
                showUrlCopyDialog(url);
            }
        } catch (Exception e) {
            logger.warning("Failed to open URL: " + e.getMessage());
            showUrlCopyDialog(url);
        }
    }

    /**
     * Shows a dialog with the URL for manual copying if browser opening fails
     *
     * @param url the URL to display
     */
    private void showUrlCopyDialog(String url) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(dialog.getOwner());
        alert.setTitle("GitHub Repository");
        alert.setHeaderText("Visit the project on GitHub");
        alert.setContentText("Copy this URL:\n" + url);
        alert.showAndWait();
    }

    /**
     * Closes the dialog
     */
    public void close() {
        if (dialog != null) {
            dialog.close();
        }
    }
}