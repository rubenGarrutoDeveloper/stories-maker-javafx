package com.voiceai.constant;

public class UIConstants {

    // Colors
    public static final String PRIMARY_COLOR = "#6b46c1";
    public static final String SUCCESS_COLOR = "#10b981";
    public static final String ERROR_COLOR = "#ef4444";
    public static final String WARNING_COLOR = "#f59e0b";
    public static final String SECONDARY_COLOR = "#8b5cf6";
    public static final String INFO_COLOR = "#06b6d4";
    public static final String DANGER_COLOR = "#dc2626";
    public static final String GRAY_COLOR = "#9ca3af";
    public static final String LIGHT_GRAY_COLOR = "#e5e7eb";
    public static final String BORDER_COLOR = "#d1d5db";
    public static final String BACKGROUND_COLOR = "#f5f5f5";
    public static final String WHITE_COLOR = "white";

    // UI Text
    public static final String APP_TITLE = "StorieS Maker";
    public static final String API_KEY_PROMPT = "OpenAI API Key";
    public static final String TEST_CONNECTION_TEXT = "Test Connection";
    public static final String TESTING_TEXT = "Testing...";
    public static final String DISCONNECTED_STATUS = "Disconnected";
    public static final String TRANSCRIPT_HEADER = "TRANSCRIPT";
    public static final String CHAT_HEADER = "💬 CHAT AI";
    public static final String AUDIO_SOURCE_LABEL = "Audio Source:";
    public static final String REFRESH_SOURCES_TEXT = "🔄";

    // Button Text
    public static final String REC_BUTTON_TEXT = "🔴 REC";
    public static final String STOP_BUTTON_TEXT = "⏹ STOP";
    public static final String STOPPING_BUTTON_TEXT = "⏹ STOPPING...";
    public static final String SAVE_BUTTON_TEXT = "💾 SAVE";
    public static final String SELECT_ALL_BUTTON_TEXT = "📋 SELECT ALL";
    public static final String LOAD_BUTTON_TEXT = "📂 LOAD";
    public static final String SEND_BUTTON_TEXT = "➤";
    public static final String INSERT_TRANSCRIPT_BUTTON_TEXT = "📎 Insert Transcript";
    public static final String CLEAR_BUTTON_TEXT = "\uD83D\uDDD1 Clear";

    // Placeholder Text
    public static final String TRANSCRIPT_PLACEHOLDER_IDLE = "Premi REC per iniziare la trascrizione...";
    public static final String TRANSCRIPT_PLACEHOLDER_RECORDING = "🎤 Recording in progress...";
    public static final String CHAT_PLACEHOLDER = "Scrivi un messaggio per iniziare...";
    public static final String MESSAGE_PLACEHOLDER = "Scrivi un messaggio...";
    public static final String AUDIO_SOURCE_PROMPT = "Select audio input...";

    // Dimensions
    public static final int MAIN_WINDOW_WIDTH = 1200;
    public static final int MAIN_WINDOW_HEIGHT = 700;
    public static final int PANEL_WIDTH = 580;
    public static final int TRANSCRIPT_AREA_HEIGHT = 400;
    public static final int CHAT_AREA_HEIGHT = 300;
    public static final int API_KEY_FIELD_WIDTH = 300;
    public static final int MESSAGE_FIELD_WIDTH = 450;
    public static final int AUDIO_SOURCE_COMBO_WIDTH = 250;

    // Spacing
    public static final int ROOT_SPACING = 15;
    public static final int MAIN_CONTENT_SPACING = 20;
    public static final int SECTION_SPACING = 10;
    public static final int CONTROL_SPACING = 10;
    public static final int PANEL_SPACING = 15;
    public static final int ROOT_PADDING = 20;
    public static final int PANEL_PADDING = 20;

    // Font Sizes
    public static final String TITLE_FONT_SIZE = "24px";
    public static final String HEADER_FONT_SIZE = "16px";
    public static final String LABEL_FONT_SIZE = "12px";

    // CSS Styles
    public static final String TITLE_STYLE =
            "-fx-font-size: " + TITLE_FONT_SIZE + "; " +
                    "-fx-font-weight: bold; " +
                    "-fx-text-fill: " + PRIMARY_COLOR + ";";

    public static final String HEADER_STYLE =
            "-fx-font-size: " + HEADER_FONT_SIZE + "; " +
                    "-fx-font-weight: bold;";

    public static final String LABEL_STYLE =
            "-fx-font-size: " + LABEL_FONT_SIZE + "; " +
                    "-fx-text-fill: #666;";

    public static final String ROOT_STYLE =
            "-fx-background-color: " + BACKGROUND_COLOR + ";";

    public static final String PANEL_STYLE =
            "-fx-background-color: " + WHITE_COLOR + "; " +
                    "-fx-border-radius: 10px; " +
                    "-fx-padding: " + PANEL_PADDING + "px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);";

    public static final String INPUT_FIELD_STYLE =
            "-fx-padding: 8px; " +
                    "-fx-border-radius: 5px;";

    public static final String COMBO_BOX_STYLE =
            "-fx-padding: 6px; " +
                    "-fx-border-radius: 5px;";

    public static final String TEXT_AREA_STYLE =
            "-fx-control-inner-background: " + WHITE_COLOR + "; " +
                    "-fx-border-color: " + BORDER_COLOR + "; " +
                    "-fx-border-radius: 5px;";

    // Button Styles
    public static final String BASE_BUTTON_STYLE =
            "-fx-border-radius: 5px; " +
                    "-fx-padding: 8px 15px;";

    public static final String SMALL_BUTTON_STYLE =
            "-fx-border-radius: 5px; " +
                    "-fx-padding: 6px 10px; " +
                    "-fx-font-size: 14px;";

    public static final String PRIMARY_BUTTON_STYLE =
            "-fx-background-color: " + ERROR_COLOR + "; " +
                    "-fx-text-fill: " + WHITE_COLOR + "; " +
                    BASE_BUTTON_STYLE;

    public static final String SUCCESS_BUTTON_STYLE =
            "-fx-background-color: " + SUCCESS_COLOR + "; " +
                    "-fx-text-fill: " + WHITE_COLOR + "; " +
                    BASE_BUTTON_STYLE;

    public static final String SECONDARY_BUTTON_STYLE =
            "-fx-background-color: " + SECONDARY_COLOR + "; " +
                    "-fx-text-fill: " + WHITE_COLOR + "; " +
                    BASE_BUTTON_STYLE;

    public static final String INFO_BUTTON_STYLE =
            "-fx-background-color: " + INFO_COLOR + "; " +
                    "-fx-text-fill: " + WHITE_COLOR + "; " +
                    BASE_BUTTON_STYLE;

    public static final String LIGHT_BUTTON_STYLE =
            "-fx-background-color: " + LIGHT_GRAY_COLOR + "; " +
                    "-fx-text-fill: black; " +
                    BASE_BUTTON_STYLE;

    public static final String DISABLED_BUTTON_STYLE =
            "-fx-background-color: " + GRAY_COLOR + "; " +
                    "-fx-text-fill: " + WHITE_COLOR + "; " +
                    BASE_BUTTON_STYLE;

    public static final String DANGER_BUTTON_STYLE =
            "-fx-background-color: " + DANGER_COLOR + "; " +
                    "-fx-text-fill: " + WHITE_COLOR + "; " +
                    BASE_BUTTON_STYLE;

    public static final String SEND_BUTTON_STYLE =
            "-fx-background-color: " + SUCCESS_COLOR + "; " +
                    "-fx-text-fill: " + WHITE_COLOR + "; " +
                    "-fx-border-radius: 5px; " +
                    "-fx-padding: 8px 12px;";

    public static final String REFRESH_BUTTON_STYLE =
            "-fx-background-color: " + INFO_COLOR + "; " +
                    "-fx-text-fill: " + WHITE_COLOR + "; " +
                    SMALL_BUTTON_STYLE;

    // Status Styles
    public static final String SUCCESS_STATUS_STYLE =
            "-fx-background-color: " + SUCCESS_COLOR + "; " +
                    "-fx-text-fill: " + WHITE_COLOR + "; " +
                    "-fx-padding: 5px 10px; " +
                    "-fx-border-radius: 15px;";

    public static final String ERROR_STATUS_STYLE =
            "-fx-background-color: " + ERROR_COLOR + "; " +
                    "-fx-text-fill: " + WHITE_COLOR + "; " +
                    "-fx-padding: 5px 10px; " +
                    "-fx-border-radius: 15px;";

    public static final String WARNING_STATUS_STYLE =
            "-fx-background-color: " + WARNING_COLOR + "; " +
                    "-fx-text-fill: " + WHITE_COLOR + "; " +
                    "-fx-padding: 5px 10px; " +
                    "-fx-border-radius: 15px;";

    // Utility methods for creating custom status styles
    public static String createStatusStyle(String backgroundColor) {
        return "-fx-background-color: " + backgroundColor + "; " +
                "-fx-text-fill: " + WHITE_COLOR + "; " +
                "-fx-padding: 5px 10px; " +
                "-fx-border-radius: 15px;";
    }

    public static String createButtonStyle(String backgroundColor) {
        return "-fx-background-color: " + backgroundColor + "; " +
                "-fx-text-fill: " + WHITE_COLOR + "; " +
                BASE_BUTTON_STYLE;
    }

    // Private constructor to prevent instantiation
    private UIConstants() {}
}