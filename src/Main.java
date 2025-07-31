// Enhanced Pune Metro Route Planner with Visible Route Nodes
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.shape.*;
import javafx.animation.*;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicInteger;

public class Main extends Application {
    // Static variables and methods
    static final int V = 30;
    static final int MAX = 30;
    static int[] predecessor = new int[30];
    static int[] reversePath = new int[30];
    static int[] correctPath = new int[30];
    static String[] color = new String[30];
    static Stack<Integer> stack = new Stack<>();
    static float[][] adj = new float[MAX][MAX];
    static float[] dist = new float[MAX];

    // GUI components
    private ComboBox<String> sourceComboBox;
    private ComboBox<String> destinationComboBox;
    private TextArea resultArea;
    private List<String> stationNames;
    private VBox routeDisplayBox;
    private List<Label> stepLabels; // Add this line to track step numbers
    
    // Enhanced UI components for better visibility
    private ProgressIndicator progressIndicator;
    private Label statusLabel;
    private ScrollPane mapScrollPane;
    private Pane mapPane;
    private Map<Integer, Circle> stationCircles;
    private Map<Integer, Label> stationLabels;
    private List<Line> connectionLines;
    private VBox mapContainer;
    private List<Line> routeLines; // For highlighting route connections

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize data
        giveColorToStation();
        createGraph();
        initializeStationNames();

        // ===== Root Layout =====
        routeDisplayBox = new VBox();
        routeDisplayBox.setSpacing(10);
        routeDisplayBox.setPadding(new Insets(20));

        // ===== Header =====
        Label header = new Label("PUNE METRO CONNECT");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        header.setTextFill(Color.WHITE);

        // Theme toggle switch
        ToggleButton themeToggle = new ToggleButton("🌞");
        themeToggle.setFont(Font.font(18));

        // Header container (Header + Toggle)
        HBox headerBox = new HBox(20, header, themeToggle);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(themeToggle, Priority.ALWAYS);
        headerBox.setSpacing(10);

        // ===== Input Grid =====
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);
        inputGrid.setPadding(new Insets(10));

        // Source
        Label sourceLabel = new Label("Source Station:");
        sourceLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        sourceComboBox = new ComboBox<>();
        sourceComboBox.setPromptText("Select or type station name");
        sourceComboBox.setEditable(true);
        sourceComboBox.getItems().addAll(stationNames);

        // Destination
        Label destLabel = new Label("Destination Station:");
        destLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        destinationComboBox = new ComboBox<>();
        destinationComboBox.setPromptText("Select or type station name");
        destinationComboBox.setEditable(true);
        destinationComboBox.getItems().addAll(stationNames);

        // Find Route Button - FIXED TO USE ENHANCED VERSION
        Button findRouteBtn = new Button("🔍 Find Route");
        findRouteBtn.setPrefWidth(150);
        findRouteBtn.setPrefHeight(40);
        findRouteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #FF6B6B, #FF8E53); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-font-size: 14;");
        findRouteBtn.setOnAction(e -> findRouteWithAnimation()); // FIXED: Now calls the enhanced version

        // Clear Route Button
        Button clearBtn = new Button("🗑️ Clear");
        clearBtn.setPrefWidth(100);
        clearBtn.setPrefHeight(40);
        clearBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #95A5A6, #7F8C8D); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;");
        clearBtn.setOnAction(e -> clearRoute());

        // Add to Grid
        inputGrid.add(sourceLabel, 0, 0);
        inputGrid.add(sourceComboBox, 1, 0);
        inputGrid.add(destLabel, 0, 1);
        inputGrid.add(destinationComboBox, 1, 1);
        
        HBox buttonBox = new HBox(10, findRouteBtn, clearBtn);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        inputGrid.add(buttonBox, 1, 2);

        // ===== Result Text Area =====
        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPrefHeight(200);
        resultArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12;");

        // Create enhanced components
        VBox progressSection = createProgressSection();
        VBox interactiveMap = createInteractiveMetroMap();
        styleComboBoxes();

        // Add all UI elements to root
        routeDisplayBox.getChildren().addAll(headerBox, inputGrid, progressSection, interactiveMap, resultArea);

        // ===== Scene Setup =====
        ScrollPane mainScrollPane = new ScrollPane(routeDisplayBox);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setStyle("-fx-background-color: #2b2b2b;");
        
        Scene scene = new Scene(mainScrollPane, 1200, 800);
        // Increased from 900, 700
        // Apply basic styling since CSS files might not exist
        routeDisplayBox.setStyle("-fx-background-color: #2b2b2b;");
        header.setStyle("-fx-text-fill: white;");
        sourceLabel.setStyle("-fx-text-fill: white;");
        destLabel.setStyle("-fx-text-fill: white;");
        resultArea.setStyle("-fx-control-inner-background: #3c3c3c; -fx-text-fill: white;");
        inputGrid.setStyle("-fx-background-color: #2b2b2b;");
        
        // Toggle Theme Logic
        themeToggle.setOnAction(e -> {
            if (themeToggle.isSelected()) {
                // Light theme
                routeDisplayBox.setStyle("-fx-background-color: #f0f0f0;");
                mainScrollPane.setStyle("-fx-background-color: #f0f0f0;");
                header.setStyle("-fx-text-fill: black;");
                sourceLabel.setStyle("-fx-text-fill: black;");
                destLabel.setStyle("-fx-text-fill: black;");
                resultArea.setStyle("-fx-control-inner-background: white; -fx-text-fill: black;");
                inputGrid.setStyle("-fx-background-color: #f0f0f0;");
                updateMapTheme(true);
                themeToggle.setText("🌜");
            } else {
                // Dark theme
                routeDisplayBox.setStyle("-fx-background-color: #2b2b2b;");
                mainScrollPane.setStyle("-fx-background-color: #2b2b2b;");
                header.setStyle("-fx-text-fill: white;");
                sourceLabel.setStyle("-fx-text-fill: white;");
                destLabel.setStyle("-fx-text-fill: white;");
                resultArea.setStyle("-fx-control-inner-background: #3c3c3c; -fx-text-fill: white;");
                inputGrid.setStyle("-fx-background-color: #2b2b2b;");
                updateMapTheme(false);
                themeToggle.setText("🌞");
            }
        });

        primaryStage.setTitle("Pune Metro Route Planner - Enhanced");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true); // Start maximized for better visibility
        primaryStage.show();
    }

    // Clear route highlighting and selections
    // Clear route highlighting and selections
private void clearRoute() {
    sourceComboBox.setValue(null);
    destinationComboBox.setValue(null);
    resultArea.clear();
    resetMapHighlighting();
    
    // FIXED: Unbind before setting text
    if (statusLabel.textProperty().isBound()) {
        statusLabel.textProperty().unbind();
    }
    statusLabel.setText("Route cleared");
    statusLabel.setVisible(true);
    
    // Hide status after 2 seconds
    Timeline hideStatus = new Timeline(new KeyFrame(Duration.seconds(2), e -> statusLabel.setVisible(false)));
    hideStatus.play();
}

    // Reset all map highlighting
    // Reset all map highlighting
private void resetMapHighlighting() {
    if (stationCircles != null) {
        for (Circle station : stationCircles.values()) {
            station.setStrokeWidth(2);
            station.setStroke(javafx.scene.paint.Color.WHITE);
            station.setScaleX(1.0);
            station.setScaleY(1.0);
        }
    }
    
    // Remove route lines
    if (routeLines != null) {
        for (Line line : routeLines) {
            mapPane.getChildren().remove(line);
        }
        routeLines.clear();
    }
    
    // FIXED: Remove step labels (route numbers)
    if (stepLabels != null) {
        for (Label stepLabel : stepLabels) {
            mapPane.getChildren().remove(stepLabel);
        }
        stepLabels.clear();
    }
    
    // FIXED: Remove all glow effects and other temporary visual elements
    // This removes any leftover glow circles or temporary elements
    mapPane.getChildren().removeIf(node -> {
        if (node instanceof Circle) {
            Circle circle = (Circle) node;
            // Remove glow circles (they have stroke but no fill and are larger)
            return circle.getFill() == null && circle.getStroke() != null && circle.getRadius() > 15;
        }
        return false;
    });
}

    // Helper to find the route as a list of station names
    private List<String> findRouteBetweenStations(String source, String destination) {
        int sourceIdx = getStationIndex(source);
        int destIdx = getStationIndex(destination);
        
        if (sourceIdx < 0 || destIdx < 0)
            return Collections.emptyList();

        dijkstra(sourceIdx, destIdx);

        List<String> path = new ArrayList<>();
        int current = destIdx;

        while (current != sourceIdx) {
            path.add(getStationName(current));
            if (predecessor[current] == -1) {
                // No path found
                return Collections.emptyList();
            }
            current = predecessor[current];
        }

        path.add(getStationName(sourceIdx));
        Collections.reverse(path);
        return path;
    }

    private void initializeStationNames() {
        stationNames = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            stationNames.add(getStationName(i).trim());
        }
        Collections.sort(stationNames);
    }

    // Helper method to check if the station name is similar to the target (case-insensitive, partial match)
    public static boolean checkStation(String input, String target) {
        if (input == null || target == null)
            return false;
        input = input.trim().toLowerCase();
        target = target.trim().toLowerCase();
        return input.contains(target) || target.contains(input);
    }

    public static int getStationIndex(String name) {
        int idx = station(name);
        return (idx != -1) ? idx : -1;
    }

    // Assign line colors based on index range for 30 stations
    // Assign line colors based on index range for 30 stations
public static void giveColorToStation() {
    // Purple Line: index 0 to 12
    for (int i = 0; i <= 12; i++) {
        color[i] = "Purple";
    }

    // Aqua Line: index 13 to 29
    for (int i = 13; i < 30; i++) {
        color[i] = "Aqua";
    }
    
    // FIXED: Civil Court (22) is junction - belongs to BOTH lines
    color[22] = "Junction"; // Special color for junction station
}

    public static void push(int data) {
        if (stack.size() >= 100) {
            System.out.println("Stack Overflow");
            return;
        }
        stack.push(data);
    }

    private static int getArrivalMinute(int src, boolean forward) {
        // Applies only to stations 0–29
        if (src < 0 || src >= 30)
            return 0;

        if (forward) {
            if (src == 0 || src == 13) // Start of Purple (0) and Aqua (13)
                return 0;
            if ((src >= 1 && src <= 4) || (src >= 14 && src <= 18))
                return 5;
            if ((src >= 5 && src <= 8) || (src >= 19 && src <= 22))
                return 10;
            if ((src >= 9 && src <= 11) || (src >= 23 && src <= 25))
                return 15;
            if (src == 12 || src == 26 || src == 27)
                return 20;
            if (src == 28 || src == 29)
                return 25;
        } else {
            if (src == 12 || src == 29) // End of Purple (12) and Aqua (29)
                return 30;
            if ((src >= 1 && src <= 4) || (src >= 14 && src <= 18))
                return 25;
            if ((src >= 5 && src <= 8) || (src >= 19 && src <= 22))
                return 20;
            if ((src >= 9 && src <= 11) || (src >= 23 && src <= 25))
                return 15;
            if (src == 0 || src == 13)
                return 10;
        }

        return 0; // fallback
    }

    // ===== ENHANCED UI METHODS =====
    
    // Create Interactive Metro Map with BETTER VISIBILITY
    private VBox createInteractiveMetroMap() {
        mapContainer = new VBox(10);
        mapContainer.setPadding(new Insets(10));
        
        Label mapTitle = new Label(" Interactive Metro Network Map");
        mapTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        mapTitle.setStyle("-fx-text-fill: white;");
        
        // Create map pane with LARGER size for better visibility
       // Create map pane with MUCH LARGER size for better visibility and spacing
        mapPane = new Pane();
        mapPane.setPrefSize(1400, 800); // SIGNIFICANTLY INCREASED SIZE
        mapPane.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #666; -fx-border-width: 2; -fx-border-radius: 8;");
        // Initialize collections
        stationCircles = new HashMap<>();
        stationLabels = new HashMap<>();
        connectionLines = new ArrayList<>();
        routeLines = new ArrayList<>(); // Initialize route lines
        stepLabels = new ArrayList<>(); // Add this line
        // Draw metro lines and stations
        drawMetroNetwork();
        
        // Create scroll pane for map with better styling
        mapScrollPane = new ScrollPane(mapPane);
        mapScrollPane.setPrefHeight(500); // INCREASED HEIGHT
        mapScrollPane.setFitToWidth(true);
        mapScrollPane.setFitToHeight(true);
        mapScrollPane.setStyle("-fx-background: #1a1a1a; -fx-border-color: #666; -fx-border-width: 2; -fx-border-radius: 8;");
        
        mapContainer.getChildren().addAll(mapTitle, mapScrollPane);
        return mapContainer;
    }

    // Draw Metro Network with IMPROVED VISIBILITY
    private void drawMetroNetwork() {
       // Clear existing elements
        mapPane.getChildren().clear();
        stationCircles.clear();
        stationLabels.clear();
        connectionLines.clear();
        if (stepLabels != null) {
        stepLabels.clear(); // FIXED: Clear step labels when redrawing map
        }
        
        // Create background grid for better visual appeal
        createBackgroundGrid();
        
        // Purple Line - BETTER SPACED coordinates for visibility
        double[] purpleX = {120, 220, 320, 420, 520, 620, 720, 820, 920, 1020, 1120, 1220, 1320};
        double[] purpleY = {200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200};

        // Draw Purple Line background track with BETTER VISIBILITY
        for (int i = 0; i < purpleX.length - 1; i++) {
            Rectangle track = new Rectangle(purpleX[i] + 15, purpleY[i] - 6, purpleX[i+1] - purpleX[i] - 30, 12);
            track.setFill(javafx.scene.paint.Color.web("#8E4EC6"));
            track.setOpacity(0.6); // INCREASED OPACITY
            track.setStroke(javafx.scene.paint.Color.web("#FFFFFF"));
            track.setStrokeWidth(1);
            mapPane.getChildren().add(track);
        }
        
        // Draw Purple Line stations (0-12) with ENHANCED VISIBILITY
        for (int i = 0; i <= 12; i++) {
            createEnhancedStationNode(i, purpleX[i], purpleY[i], "#8E4EC6", "Purple Line");
        }
        
        // Aqua Line - BETTER CURVED path for visibility
        // Aqua Line - STRAIGHT like Purple Line with proper spacing
double[] aquaX = {60, 120, 200, 280, 360, 440, 520, 600, 680, 760, 840, 920, 1000, 1080, 1160, 1240, 1320};
double[] aquaY = { 350, 350, 350, 350, 350, 350, 350, 350, 350, 350, 350, 350, 350, 350, 350, 350, 350 }; // All same Y - straight line

        // Draw Aqua Line background track (curved) with BETTER VISIBILITY
        // Draw Aqua Line background track (STRAIGHT) with BETTER VISIBILITY
for (int i = 0; i < Math.min(aquaX.length - 1, 16); i++) {
    if (i + 13 <= 29) {
        Rectangle track = new Rectangle(aquaX[i] + 20, aquaY[i] - 8, aquaX[i+1] - aquaX[i] - 40, 16);
        track.setFill(javafx.scene.paint.Color.web("#00CED1"));
        track.setOpacity(0.7);
        track.setStroke(javafx.scene.paint.Color.web("#FFFFFF"));
        track.setStrokeWidth(2);
        mapPane.getChildren().add(track);
    }
}
        
        // Draw Aqua Line stations (13-29) with ENHANCED VISIBILITY
        for (int i = 13; i <= 29; i++) {
            int aquaIndex = i - 13;
            if (aquaIndex < aquaX.length) {
                createEnhancedStationNode(i, aquaX[aquaIndex], aquaY[aquaIndex], "#00CED1", "Aqua Line");
            }
        }
        
        // Draw interchange connection with ENHANCED styling
        createEnhancedInterchangeConnection();
        
        // Add enhanced legend
        // addEnhancedMapLegend();
        
        // Add title and info
        addMapTitleAndInfo();
    }
    
    // Create background grid for better visual appeal
    private void createBackgroundGrid() {
        // Add subtle grid lines with BETTER VISIBILITY
        for (int i = 0; i < 850; i += 50) {
            Line verticalLine = new Line(i, 0, i, 500);
            verticalLine.setStroke(javafx.scene.paint.Color.web("#333333"));
            verticalLine.setStrokeWidth(0.8); // INCREASED WIDTH
            verticalLine.setOpacity(0.3); // INCREASED OPACITY
            mapPane.getChildren().add(verticalLine);
        }
        
        for (int i = 0; i < 500; i += 50) {
            Line horizontalLine = new Line(0, i, 850, i);
            horizontalLine.setStroke(javafx.scene.paint.Color.web("#333333"));
            horizontalLine.setStrokeWidth(0.8); // INCREASED WIDTH
            horizontalLine.setOpacity(0.3); // INCREASED OPACITY
            mapPane.getChildren().add(horizontalLine);
        }
    }
    
    // Create ENHANCED interchange connection
    // Create ENHANCED interchange connection - Civil Court as junction
private void createEnhancedInterchangeConnection() {
    Circle civilCourt = stationCircles.get(22);   // Station 22 - Civil Court
    
    if (civilCourt != null) {
        // Create special junction visual indicator
        Circle junctionRing = new Circle(civilCourt.getCenterX(), civilCourt.getCenterY(), 35);
        junctionRing.setFill(null);
        junctionRing.setStroke(javafx.scene.paint.Color.ORANGE);
        junctionRing.setStrokeWidth(6);
        junctionRing.getStrokeDashArray().addAll(15d, 10d);
        junctionRing.setOpacity(0.8);
        mapPane.getChildren().add(junctionRing);
        
        // Add junction icon
        Label junctionIcon = new Label("🔄");
        junctionIcon.setLayoutX(civilCourt.getCenterX() - 15);
        junctionIcon.setLayoutY(civilCourt.getCenterY() - 60);
        junctionIcon.setStyle("-fx-font-size: 24; -fx-background-color: rgba(255,165,0,0.9); -fx-background-radius: 20; -fx-padding: 8; -fx-border-color: white; -fx-border-width: 3; -fx-border-radius: 20;");
        mapPane.getChildren().add(junctionIcon);
        
        // Add "JUNCTION" label
        Label junctionLabel = new Label("JUNCTION STATION");
        junctionLabel.setLayoutX(civilCourt.getCenterX() - 60);
        junctionLabel.setLayoutY(civilCourt.getCenterY() + 45);
        junctionLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold; -fx-font-size: 12; -fx-background-color: rgba(0,0,0,0.8); -fx-padding: 5; -fx-background-radius: 5;");
        mapPane.getChildren().add(junctionLabel);
    }
}
    
    // Add map title and information with BETTER VISIBILITY
    private void addMapTitleAndInfo() {
        // Main title with ENHANCED styling
        Label mainTitle = new Label("🚇 Pune Metro Network Map");
        mainTitle.setLayoutX(280);
        mainTitle.setLayoutY(20);
        mainTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        mainTitle.setStyle("-fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.8); -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #FFD700; -fx-border-width: 2; -fx-border-radius: 8;");
        mapPane.getChildren().add(mainTitle);
        
        // Instructions with BETTER VISIBILITY
        // Label instructions = new Label("💡 Click stations to select • Hover for details • Watch route animation");
        // instructions.setLayoutX(220);
        // instructions.setLayoutY(55);
        // instructions.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 13));
        // instructions.setStyle("-fx-text-fill: #FFD700; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 6; -fx-background-radius: 5; -fx-border-color: #FFD700; -fx-border-width: 1; -fx-border-radius: 5;");
        // mapPane.getChildren().add(instructions);
    }

    // Create ENHANCED station node with MAXIMUM VISIBILITY
    // private void createEnhancedStationNode(int stationIndex, double x, double y, String color, String lineName) {
        
    private void createEnhancedStationNode(int stationIndex, double x, double y, String color, String lineName) {
    String stationColor = color;
    String stationLineName = lineName;
        if (stationIndex == 22) {
        stationColor = "#FF8C00"; // Orange color for junction
        lineName = "Junction Station (Purple ↔ Aqua)";
    }
        // Station outer glow ring (ENHANCED)
        Circle outerGlow = new Circle(x, y, 20);
        outerGlow.setFill(null);
        outerGlow.setStroke(javafx.scene.paint.Color.web(color));
        outerGlow.setStrokeWidth(3);
        outerGlow.setOpacity(0.4);
        
        // Station outer ring (ENHANCED visibility)
        Circle outerRing = new Circle(x, y, 16);
        outerRing.setFill(null);
        outerRing.setStroke(javafx.scene.paint.Color.web(color));
        outerRing.setStrokeWidth(3);
        outerRing.setOpacity(0.7);
        
        // Station main circle (LARGER for better visibility)
        Circle station = new Circle(x, y, 12);
        station.setFill(javafx.scene.paint.Color.web(color));
        station.setStroke(javafx.scene.paint.Color.WHITE);
        station.setStrokeWidth(3);
        
        // Station inner dot (ENHANCED)
        Circle innerDot = new Circle(x, y, 5);
        innerDot.setFill(javafx.scene.paint.Color.WHITE);
        
        // Station number label (ALWAYS VISIBLE)
        Label numberLabel = new Label(String.valueOf(stationIndex));
        numberLabel.setLayoutX(x - 8);
        numberLabel.setLayoutY(y - 8);
        numberLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        numberLabel.setStyle("-fx-text-fill: black;");
        
        // Station name label (ENHANCED visibility)
        String stationName = getStationName(stationIndex);
        Label stationLabel = new Label(stationName);
        stationLabel.setLayoutX(x - 50);
        stationLabel.setLayoutY(y + 25);
        stationLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        stationLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-background-color: rgba(0,0,0,0.8); -fx-padding: 3; -fx-background-radius: 4; -fx-border-color: " + color + "; -fx-border-width: 1; -fx-border-radius: 4;");
        
       
Label detailLabel = new Label(String.format("🚉 %s\n🚇 %s\n📍 Station #%d\n🎯 Click to select", stationName, lineName, stationIndex));
detailLabel.setLayoutX(x - 100);
detailLabel.setLayoutY(y - 120); // MUCH MORE SPACE above station
detailLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
detailLabel.setStyle("-fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.95); -fx-padding: 12; -fx-background-radius: 12; -fx-border-color: " + color + "; -fx-border-width: 3; -fx-border-radius: 12;");
detailLabel.setVisible(false);
        // ENHANCED hover effects with smooth animations
        station.setOnMouseEntered(e -> {
            // Multiple scale animations for better effect
            ScaleTransition stationScale = new ScaleTransition(Duration.millis(200), station);
            stationScale.setToX(1.8);
            stationScale.setToY(1.8);
            stationScale.play();
            
            ScaleTransition outerScale = new ScaleTransition(Duration.millis(200), outerRing);
            outerScale.setToX(1.5);
            outerScale.setToY(1.5);
            outerScale.play();
            
            ScaleTransition glowScale = new ScaleTransition(Duration.millis(200), outerGlow);
            glowScale.setToX(1.3);
            glowScale.setToY(1.3);
            glowScale.play();
            
            // Show detailed label with animation
            detailLabel.setVisible(true);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), detailLabel);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
            
            // Enhanced label styling
            stationLabel.setStyle("-fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.9); -fx-padding: 4; -fx-background-radius: 6; -fx-font-weight: bold; -fx-border-color: " + color + "; -fx-border-width: 2; -fx-border-radius: 6;");
            
            // Bring to front
            station.toFront();
            outerRing.toFront();
            outerGlow.toFront();
            innerDot.toFront();
            numberLabel.toFront();
            detailLabel.toFront();
        });
        
        // Complete the exit handler
        station.setOnMouseExited(e -> {
            // Scale down animations
            ScaleTransition stationScale = new ScaleTransition(Duration.millis(200), station);
            stationScale.setToX(1.0);
            stationScale.setToY(1.0);
            stationScale.play();
            
            ScaleTransition outerScale = new ScaleTransition(Duration.millis(200), outerRing);
            outerScale.setToX(1.0);
            outerScale.setToY(1.0);
            outerScale.play();
            
            ScaleTransition glowScale = new ScaleTransition(Duration.millis(200), outerGlow);
            glowScale.setToX(1.0);
            glowScale.setToY(1.0);
            glowScale.play();
            
            // Hide detailed label with animation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), detailLabel);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> detailLabel.setVisible(false));
            fadeOut.play();
            
            // Reset label styling
            stationLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-background-color: rgba(0,0,0,0.8); -fx-padding: 3; -fx-background-radius: 4; -fx-border-color: " + color + "; -fx-border-width: 1; -fx-border-radius: 4;");
        });
        
        // ENHANCED click to select station with visual feedback
        station.setOnMouseClicked(e -> {
            // Enhanced flash animation
            FillTransition flash = new FillTransition(Duration.millis(400), station);
            flash.setFromValue(javafx.scene.paint.Color.web(color));
            flash.setToValue(javafx.scene.paint.Color.YELLOW);
            flash.setCycleCount(3);
            flash.setAutoReverse(true);
            flash.play();
            
            // Pulse animation
            ScaleTransition pulse = new ScaleTransition(Duration.millis(300), station);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(2.0);
            pulse.setToY(2.0);
            pulse.setCycleCount(2);
            pulse.setAutoReverse(true);
            pulse.play();
            
            // Set station in combo boxes with smart logic
            if (sourceComboBox.getValue() == null) {
                sourceComboBox.setValue(stationName);
                showEnhancedTemporaryMessage("✅ Source: " + stationName, x, y - 90, "#00FF00");
            } else if (destinationComboBox.getValue() == null) {
                destinationComboBox.setValue(stationName);
                showEnhancedTemporaryMessage("✅ Destination: " + stationName, x, y - 90, "#00FF00");
                // Auto-trigger route finding after both stations are selected
                Timeline autoFind = new Timeline(new KeyFrame(Duration.millis(1000), ev -> findRouteWithAnimation()));
                autoFind.play();
            } else {
                sourceComboBox.setValue(stationName);
                destinationComboBox.setValue(null);
                showEnhancedTemporaryMessage("🔄 Source reset: " + stationName, x, y - 90, "#FFA500");
            }
        });
        
        stationCircles.put(stationIndex, station);
        stationLabels.put(stationIndex, stationLabel);
        
        mapPane.getChildren().addAll(outerGlow, outerRing, station, innerDot, numberLabel, stationLabel, detailLabel);
    }
    
    // Show ENHANCED temporary message on station click
    private void showEnhancedTemporaryMessage(String message, double x, double y, String textColor) {
        Label tempMessage = new Label(message);
        tempMessage.setLayoutX(x - 80);
        tempMessage.setLayoutY(y);
        tempMessage.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        tempMessage.setStyle("-fx-text-fill: " + textColor + "; -fx-background-color: rgba(0,0,0,0.9); -fx-padding: 8; -fx-background-radius: 8; -fx-border-color: " + textColor + "; -fx-border-width: 2; -fx-border-radius: 8;");
        
        // Entry animation
        tempMessage.setScaleX(0);
        tempMessage.setScaleY(0);
        mapPane.getChildren().add(tempMessage);
        
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), tempMessage);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        scaleIn.play();
        
        // Exit animation after delay
        Timeline exit = new Timeline(new KeyFrame(Duration.millis(2500), e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), tempMessage);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> mapPane.getChildren().remove(tempMessage));
            fadeOut.play();
        }));
        exit.play();
    }

    // Add ENHANCED map legend with better styling
    private void addEnhancedMapLegend() {
        VBox legend = new VBox(10);
        legend.setLayoutX(15);
        legend.setLayoutY(80);
        legend.setStyle("-fx-background-color: rgba(0,0,0,0.95); -fx-padding: 20; -fx-background-radius: 15; -fx-border-color: #FFD700; -fx-border-width: 2; -fx-border-radius: 15;");
        
        Label legendTitle = new Label("🗺️ Legend & Guide");
        legendTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        legendTitle.setStyle("-fx-text-fill: #FFD700;");
        
        // Purple Line legend with ENHANCED visibility
        HBox purpleLegend = new HBox(10);
        purpleLegend.setAlignment(Pos.CENTER_LEFT);
        Circle purpleCircle = new Circle(10);
        purpleCircle.setFill(javafx.scene.paint.Color.web("#8E4EC6"));
        purpleCircle.setStroke(javafx.scene.paint.Color.WHITE);
        purpleCircle.setStrokeWidth(2);
        Rectangle purpleLine = new Rectangle(25, 6);
        purpleLine.setFill(javafx.scene.paint.Color.web("#8E4EC6"));
        purpleLine.setStroke(javafx.scene.paint.Color.WHITE);
        purpleLine.setStrokeWidth(1);
        Label purpleLabel = new Label("Purple Line (PCMC ↔ Swargate)");
        purpleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
        purpleLegend.getChildren().addAll(purpleCircle, purpleLine, purpleLabel);
        
        // Aqua Line legend with ENHANCED visibility
        HBox aquaLegend = new HBox(10);
        aquaLegend.setAlignment(Pos.CENTER_LEFT);
        Circle aquaCircle = new Circle(10);
        aquaCircle.setFill(javafx.scene.paint.Color.web("#00CED1"));
        aquaCircle.setStroke(javafx.scene.paint.Color.WHITE);
        aquaCircle.setStrokeWidth(2);
        Rectangle aquaLine = new Rectangle(25, 6);
        aquaLine.setFill(javafx.scene.paint.Color.web("#00CED1"));
        aquaLine.setStroke(javafx.scene.paint.Color.WHITE);
        aquaLine.setStrokeWidth(1);
        Label aquaLabel = new Label("Aqua Line (Chandani Chowk ↔ Ramwadi)");
        aquaLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
        aquaLegend.getChildren().addAll(aquaCircle, aquaLine, aquaLabel);
        
        
        // Interchange legend with ENHANCED visibility
        // HBox interchangeLegend = new HBox(10);
        // interchangeLegend.setAlignment(Pos.CENTER_LEFT);
        // Line interchangeLine = new Line(0, 0, 30, 0);
        // interchangeLine.setStroke(javafx.scene.paint.Color.ORANGE);
        // interchangeLine.setStrokeWidth(4);
        // interchangeLine.getStrokeDashArray().addAll(8d, 4d);
        // Label walkIcon = new Label("🚶‍♂️");
        // walkIcon.setStyle("-fx-font-size: 16;");
        // Label interchangeLabel = new Label("Interchange (Walking Connection)");
        // interchangeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
        // interchangeLegend.getChildren().addAll(interchangeLine, walkIcon, interchangeLabel);
      
        // Junction Station legend
HBox interchangeLegend = new HBox(10);
interchangeLegend.setAlignment(Pos.CENTER_LEFT);
Circle junctionCircle = new Circle(10);
junctionCircle.setFill(javafx.scene.paint.Color.web("#FF8C00"));
junctionCircle.setStroke(javafx.scene.paint.Color.WHITE);
junctionCircle.setStrokeWidth(2);
Label junctionIcon = new Label("🔄");
junctionIcon.setStyle("-fx-font-size: 16;");
Label junctionLabel = new Label("Junction Station (Civil Court)");
junctionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
interchangeLegend.getChildren().addAll(junctionCircle, junctionIcon, junctionLabel);

        // Route highlighting legend
        HBox routeLegend = new HBox(10);
        routeLegend.setAlignment(Pos.CENTER_LEFT);
        Circle routeCircle = new Circle(10);
        routeCircle.setFill(javafx.scene.paint.Color.YELLOW);
        routeCircle.setStroke(javafx.scene.paint.Color.WHITE);
        routeCircle.setStrokeWidth(2);
        Line routeLine = new Line(0, 0, 30, 0);
        routeLine.setStroke(javafx.scene.paint.Color.YELLOW);
        routeLine.setStrokeWidth(4);
        Label routeIcon = new Label("⚡");
        Label routeLabel = new Label("Active Route Highlighting");
        routeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
        routeLegend.getChildren().addAll(routeCircle, routeLine, routeIcon, routeLabel);
        
        // Controls section
        Label controlsTitle = new Label("🎮 Interactive Controls:");
        controlsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        controlsTitle.setStyle("-fx-text-fill: #FFD700;");
        
        VBox controlsList = new VBox(5);
        String[] controls = {
            "• 🖱️ Click stations to select source/destination",
            "• 👆 Hover over stations for detailed information", 
            "• 🔍 Auto-route finding after selecting both stations",
            "• 🗑️ Use Clear button to reset selections",
            "• ⚡ Watch animated route highlighting"
        };
        
        for (String control : controls) {
            Label controlLabel = new Label(control);
            controlLabel.setStyle("-fx-text-fill: #CCCCCC; -fx-font-size: 11;");
            controlsList.getChildren().add(controlLabel);
        }
        
       legend.getChildren().addAll(
        legendTitle, 
        new Label(" "), // spacer
        purpleLegend, 
        aquaLegend, 
        interchangeLegend,  // ADD THIS LINE
        routeLegend,
        new Label(" "), // spacer
        controlsTitle,
        controlsList
        );
        
        mapPane.getChildren().add(legend);
    }

    // ENHANCED Route highlighting on Map with MAXIMUM VISIBILITY
    private void highlightRouteOnMap(List<Integer> route) {
        // Reset all stations to default first
        resetMapHighlighting();
        
        // Initialize route lines list if needed
        if (routeLines == null) {
            routeLines = new ArrayList<>();
        }
        
        // Highlight route stations with ENHANCED animations
        for (int i = 0; i < route.size(); i++) {
            int stationIndex = route.get(i);
            Circle station = stationCircles.get(stationIndex);
            
            if (station != null) {
                final int currentStep = i;
                
                // Create delayed animation for each station
                Timeline stationAnimation = new Timeline();
                KeyFrame keyFrame = new KeyFrame(Duration.millis(currentStep * 300), e -> {
                    // ENHANCED highlighting
                    station.setStroke(javafx.scene.paint.Color.YELLOW);
                    station.setStrokeWidth(5);
                    
                    // Add glowing effect
                    Circle glow = new Circle(station.getCenterX(), station.getCenterY(), 20);
                    glow.setFill(null);
                    glow.setStroke(javafx.scene.paint.Color.YELLOW);
                    glow.setStrokeWidth(3);
                    glow.setOpacity(0.3);
                    mapPane.getChildren().add(glow);
                    
                    // Pulsing animation
                    ScaleTransition pulse = new ScaleTransition(Duration.millis(500), station);
                    pulse.setFromX(1.0);
                    pulse.setFromY(1.0);
                    pulse.setToX(1.5);
                    pulse.setToY(1.5);
                    pulse.setCycleCount(2);
                    pulse.setAutoReverse(true);
                    pulse.play();
                    
                    // Glow pulse
                    ScaleTransition glowPulse = new ScaleTransition(Duration.millis(500), glow);
                    glowPulse.setFromX(1.0);
                    glowPulse.setFromY(1.0);
                    glowPulse.setToX(1.3);
                    glowPulse.setToY(1.3);
                    glowPulse.setCycleCount(Timeline.INDEFINITE);
                    glowPulse.setAutoReverse(true);
                    glowPulse.play();
                    
                    // Show step number
                    // Show step number
Label stepLabel = new Label("" + (currentStep + 1));
stepLabel.setLayoutX(station.getCenterX() - 8);
stepLabel.setLayoutY(station.getCenterY() - 35);
stepLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
stepLabel.setStyle("-fx-text-fill: black; -fx-background-color: yellow; -fx-background-radius: 15; -fx-padding: 5; -fx-border-color: black; -fx-border-width: 2; -fx-border-radius: 15;");
mapPane.getChildren().add(stepLabel);
stepLabels.add(stepLabel); // FIXED: Track the step label for removal later
                    
                    // Connect to previous station with animated line
                    if (currentStep > 0) {
                        int prevIndex = route.get(currentStep - 1);
                        Circle prevStation = stationCircles.get(prevIndex);
                        if (prevStation != null) {
                            Line routeLine = new Line(
                                prevStation.getCenterX(), 
                                prevStation.getCenterY(),
                                station.getCenterX(), 
                                station.getCenterY()
                            );
                            routeLine.setStroke(javafx.scene.paint.Color.YELLOW);
                            routeLine.setStrokeWidth(6);
                            routeLine.setOpacity(0.8);
                            routeLine.getStrokeDashArray().addAll(10d, 5d);
                            
                            // Animate the line drawing
                            routeLine.setStrokeLineCap(StrokeLineCap.ROUND);
                            mapPane.getChildren().add(routeLine);
                            routeLines.add(routeLine);
                            
                            // Moving dash animation
                            Timeline dashAnimation = new Timeline(
                                new KeyFrame(Duration.ZERO, new KeyValue(routeLine.strokeDashOffsetProperty(), 0)),
                                new KeyFrame(Duration.seconds(2), new KeyValue(routeLine.strokeDashOffsetProperty(), 30))
                            );
                            dashAnimation.setCycleCount(Timeline.INDEFINITE);
                            dashAnimation.play();
                        }
                    }
                });
                stationAnimation.getKeyFrames().add(keyFrame);
                stationAnimation.play();
            }
        }
        
        // Add completion message
        Timeline completionMessage = new Timeline(new KeyFrame(Duration.millis(route.size() * 300 + 1000), e -> {
            showEnhancedTemporaryMessage("🎉 Route highlighted! Journey ready!", 400, 100, "#00FF00");
        }));
        completionMessage.play();
    }

    // Create Progress Section with ENHANCED styling
    private VBox createProgressSection() {
        VBox progressSection = new VBox(10);
        progressSection.setAlignment(Pos.CENTER);
        progressSection.setPadding(new Insets(15));
        progressSection.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 10; -fx-border-color: #444; -fx-border-width: 1; -fx-border-radius: 10;");
        
        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(60, 60);
        progressIndicator.setVisible(false);
        progressIndicator.setStyle("-fx-accent: #FFD700;");
        
        statusLabel = new Label("");
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        statusLabel.setStyle("-fx-text-fill: #FFD700;");
        statusLabel.setVisible(false);
        
        progressSection.getChildren().addAll(progressIndicator, statusLabel);
        return progressSection;
    }

    // ENHANCED Find Route with Animation and MAXIMUM VISIBILITY
    private void findRouteWithAnimation() {
        String sourceName = sourceComboBox.getValue();
        String destinationName = destinationComboBox.getValue();

        if (sourceName == null || destinationName == null) {
            showAlert("Please select both source and destination stations");
            return;
        }

        if (sourceName.equals(destinationName)) {
            showAlert("Source and destination stations cannot be the same");
            return;
        }

        // Reset previous highlighting
        resetMapHighlighting();
        
        // Show ENHANCED progress
        progressIndicator.setVisible(true);
        statusLabel.setVisible(true);
        statusLabel.setText("🔍 Calculating optimal route...");
        
        // Create background task for route calculation with ENHANCED feedback
        Task<String> routeTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                updateMessage("🔍 Finding optimal route...");
                Thread.sleep(800); // Simulate processing time
                
                int sourceNumber = getStationIndex(sourceName);
                int destinationNumber = getStationIndex(destinationName);

                if (sourceNumber < 0 || destinationNumber < 0 || sourceNumber >= 30 || destinationNumber >= 30) {
                    throw new Exception("One or more stations not found");
                }

                updateMessage("🧮 Calculating shortest path using Dijkstra's algorithm...");
                Thread.sleep(600);
                
                String sourceColor = color[sourceNumber];
                dijkstra(sourceNumber, destinationNumber);
                float weight = dist[destinationNumber];

                StringBuilder result = new StringBuilder();
                result.append("🚇 PUNE METRO ROUTE DETAILS\n");
                result.append("═".repeat(50)).append("\n\n");
                result.append("📍 FROM: ").append(sourceName).append(" (Station #").append(sourceNumber).append(")\n");
                result.append("📍 TO: ").append(destinationName).append(" (Station #").append(destinationNumber).append(")\n");
                result.append("🎨 Starting Line: ").append(sourceColor).append(" Line\n\n");

                updateMessage("📋 Generating detailed route information...");
                Thread.sleep(400);
                
                findPath(sourceNumber, destinationNumber, sourceColor, result);
                result.append(String.format("\n📏 Total Distance: %.2f Km\n", weight));

                // Enhanced path calculation for time & interchange
                List<Integer> route = getPathAsList(sourceNumber, destinationNumber);
                int numberOfHops = route.size() - 1;
                int estimatedTime = numberOfHops * 2;
                int interchanges = 0;

                updateMessage("🔄 Checking for line interchanges...");
                Thread.sleep(300);

                for (int i = 1; i < route.size() - 1; i++) {
                    int prev = route.get(i - 1);
                    int curr = route.get(i);
                    int next = route.get(i + 1);

                    if (curr == 22 || curr == 9) {
                        String prevColor = color[prev];
                        String nextColor = color[next];
                        if (!prevColor.equals(nextColor)) {
                            interchanges++;
                            estimatedTime += 5;
                        }
                    }
                }

                result.append("🚉 Total Stations: ").append(route.size()).append("\n");
                result.append("🔄 Interchanges: ").append(interchanges).append("\n");

                if (estimatedTime >= 60) {
                    int hours = estimatedTime / 60;
                    int minutes = estimatedTime % 60;
                    result.append(String.format("🕒 Estimated Journey Time: %d hour%s %d minute%s\n",
                            hours, (hours != 1 ? "s" : ""),
                            minutes, (minutes != 1 ? "s" : "")));
                } else {
                    result.append("🕒 Estimated Journey Time: ").append(estimatedTime).append(" minute").append(estimatedTime != 1 ? "s" : "").append("\n");
                }

                if (interchanges > 0) {
                    result.append("⚠️ Note: Includes ").append(interchanges).append(" interchange");
                    result.append(interchanges > 1 ? "s" : "");
                    if (route.contains(22)) result.append(" at Civil Court");
                    if (route.contains(9)) result.append(" at Shivaji Nagar");
                    result.append("\n");
                }

                result.append("\n").append("═".repeat(50)).append("\n");
                result.append("💡 TIP: Watch the map for animated route highlighting!\n");
                
                updateMessage("🎨 Highlighting route on interactive map...");
                
                // Store route for map highlighting
                javafx.application.Platform.runLater(() -> highlightRouteOnMap(route));
                
                return result.toString();
            }
        };

        // Bind status label to task message with ENHANCED styling
        statusLabel.textProperty().bind(routeTask.messageProperty());

        routeTask.setOnSucceeded(e -> {
    resultArea.setText(routeTask.getValue());
    progressIndicator.setVisible(false);
    
    // FIXED: Unbind before setting text
    statusLabel.textProperty().unbind();
    statusLabel.setText("✅ Route calculation completed!");
    
    // Hide status after delay
    Timeline hideStatus = new Timeline(new KeyFrame(Duration.seconds(3), ev -> {
        statusLabel.setVisible(false);
    }));
    hideStatus.play();
});

routeTask.setOnFailed(e -> {
    showAlert("❌ Error: " + routeTask.getException().getMessage());
    progressIndicator.setVisible(false);
    
    // FIXED: Unbind before setting text
    statusLabel.textProperty().unbind();
    statusLabel.setText("❌ Route calculation failed!");
    
    Timeline hideStatus = new Timeline(new KeyFrame(Duration.seconds(3), ev -> {
        statusLabel.setVisible(false);
    }));
    hideStatus.play();
});

        // Run task in background thread
        Thread taskThread = new Thread(routeTask);
        taskThread.setDaemon(true);
        taskThread.start();
    }

    // ENHANCED ComboBox Styling
    private void styleComboBoxes() {
        // Style source combo box with ENHANCED gradient
        sourceComboBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #8E4EC6, #6A3093); " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: white; " +
            "-fx-border-width: 2; " +
            "-fx-font-size: 13;"
        );
        
        // Style destination combo box with ENHANCED gradient
        destinationComboBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #00CED1, #008B8B); " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: white; " +
            "-fx-border-width: 2; " +
            "-fx-font-size: 13;"
        );
        
        // Set preferred sizes for better visibility
        sourceComboBox.setPrefWidth(300);
        sourceComboBox.setPrefHeight(35);
        destinationComboBox.setPrefWidth(300);
        destinationComboBox.setPrefHeight(35);
    }

    // Update Map Theme with ENHANCED visibility
    private void updateMapTheme(boolean isLight) {
        if (mapPane != null) {
            if (isLight) {
                mapPane.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #666; -fx-border-width: 2; -fx-border-radius: 8;");
                // Update station labels for light theme
                for (Label label : stationLabels.values()) {
                    label.setStyle("-fx-text-fill: black; -fx-background-color: rgba(255,255,255,0.95); -fx-padding: 3; -fx-background-radius: 4; -fx-border-color: black; -fx-border-width: 1; -fx-border-radius: 4;");
                }
            } else {
                mapPane.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #666; -fx-border-width: 2; -fx-border-radius: 8;");
                // Update station labels for dark theme
                for (Label label : stationLabels.values()) {
                    label.setStyle("-fx-text-fill: #FFFFFF; -fx-background-color: rgba(0,0,0,0.8); -fx-padding: 3; -fx-background-radius: 4; -fx-border-color: white; -fx-border-width: 1; -fx-border-radius: 4;");
                }
            }
        }
        
        if (mapContainer != null && !mapContainer.getChildren().isEmpty()) {
            Label mapTitle = (Label) mapContainer.getChildren().get(0);
            if (isLight) {
                mapTitle.setStyle("-fx-text-fill: black;");
            } else {
                mapTitle.setStyle("-fx-text-fill: white;");
            }
        }
    }

    // Helper method to get path as list
    private List<Integer> getPathAsList(int source, int dest) {
        List<Integer> path = new ArrayList<>();
        int current = dest;

        while (current != -1) {
            path.add(0, current); // prepend to path
            if (current == source) break;
            current = predecessor[current];
        }

        return path;
    }

    // Enhanced alert dialog
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Metro Route Planner");
        alert.setHeaderText("Input Validation");
        alert.setContentText(message);
        
        // Style the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white;");
        
        alert.showAndWait();
    }

    // ENHANCED findPath method with better formatting
    static void findPath(int sourceNumber, int destinationNumber, String sourceColor, StringBuilder result) {
        int count = 0;
        while (destinationNumber != sourceNumber) {
            reversePath[count] = destinationNumber;
            int u = predecessor[destinationNumber];
            destinationNumber = u;
            count++;
        }

        reversePath[count++] = sourceNumber;

        for (int j = 0, i = count - 1; j < count && i >= 0; j++, i--) {
            correctPath[j] = reversePath[i];
        }

        if (count > 1) {
            int currentSource = correctPath[0];
            int nextToCurrentSource = correctPath[1];

            // Display ENHANCED time information
            LocalTime time = LocalTime.now();
            int hour = time.getHour();
            int minute = time.getMinute();

            result.append("🕐 CURRENT TIME: ").append(String.format("%02d:%02d", hour, minute)).append("\n");

            if (hour >= 6 && hour <= 23) { // Extended metro hours
                int arrivalMinute = getArrivalMinute(sourceNumber,
                        currentSource - nextToCurrentSource == -1 ||
                        currentSource - nextToCurrentSource == 13);

                if (minute <= arrivalMinute) {
                    result.append(String.format("🚇 Next Metro Arrival: %02d:%02d (Today)%n", hour, arrivalMinute));
                } else {
                    result.append(String.format("🚇 Next Metro Arrival: %02d:%02d (Next train)%n", (hour + 1) % 24, arrivalMinute));
                }
            } else {
                result.append("⚠️ Metro Service: Currently UNAVAILABLE (Operates 6:00 AM - 11:00 PM)\n");
            }

            // ENHANCED direction information
            result.append("🧭 BOARDING DIRECTION: ");
            if (currentSource - nextToCurrentSource == -1 || currentSource - nextToCurrentSource == 13) {
                result.append(getEnhancedDirectionMessage(sourceNumber, currentSource, nextToCurrentSource, true));
            } else if (currentSource - nextToCurrentSource == 1 || currentSource - nextToCurrentSource == -13) {
                result.append(getEnhancedDirectionMessage(sourceNumber, currentSource, nextToCurrentSource, false));
            }
        }

        result.append("\n🗺️ DETAILED JOURNEY PATH:\n");
        result.append("─".repeat(40)).append("\n");

        StringBuilder pathVisual = new StringBuilder();
        for (int i = 0; i < count; i++) {
            // Check for interchange points with ENHANCED messaging
            if (correctPath[i] == 9 || correctPath[i] == 22) {
                String interchangeInfo = solveConflict(i, count);
                if (!interchangeInfo.isEmpty()) {
                    result.append(interchangeInfo);
                }
            }
            
            // Add step number and station name
            pathVisual.append(String.format("(%d) %s", i + 1, getStationName(correctPath[i])));
            if (i < count - 1) {
                pathVisual.append("\n    ↓ (").append(color[correctPath[i]]).append(" Line)\n");
            }
        }
        
        result.append(pathVisual.toString()).append("\n");
    }

    // Helper method for enhanced direction message
    private static String getEnhancedDirectionMessage(int sourceNumber, int currentSource, int nextSource, boolean forward) {
        if (sourceNumber <= 12) {
            return forward ? "Towards Swargate (End of Purple Line)\n" : "Towards PCMC (Start of Purple Line)\n";
        } else {
            return forward ? "Towards Ramwadi (End of Aqua Line)\n" : "Towards Vanaz (Start of Aqua Line)\n";
        }
    }

    // Enhanced conflict resolution for interchanges
    static String solveConflict(int index, int count) {
        if (index > 0 && index < count - 1) {
            int prev = correctPath[index - 1];
            int current = correctPath[index];
            int next = correctPath[index + 1];
            
            String prevColor = color[prev];
            String currentColor = color[current];
            String nextColor = color[next];
            
            if (!prevColor.equals(nextColor)) {
                String stationName = getStationName(current);
                return String.format("\n🔄 INTERCHANGE at %s:\n" +
                       "   From: %s Line → To: %s Line\n" +
                       "   Walking time: ~3 minutes\n" +
                       "   Follow signs for %s Line platform\n\n",
                       stationName, prevColor, nextColor, nextColor);
            }
        }
        return "";
    }

    // Returns the station name for a given index
    public static String getStationName(int stationIndex) {
        switch (stationIndex) {
            case 0: return "PCMC";
            case 1: return "Sant Tukaram Nagar";
            case 2: return "Bhosari/Nashik Phata";
            case 3: return "Kasarwadi";
            case 4: return "Phugewadi";
            case 5: return "Dapodi";
            case 6: return "Bopodi";
            case 7: return "Khadaki";
            case 8: return "Range Hill";
            case 9: return "Shivaji Nagar";
            case 10: return "Budhwar Peth";
            case 11: return "Mandai";
            case 12: return "Swargate";
            case 13: return "Chandani Chowk";
            case 14: return "Vanaz";
            case 15: return "Anand Nagar";
            case 16: return "Ideal colony";
            case 17: return "Nal Stop";
            case 18: return "Garware College";
            case 19: return "Deccan Gymkhana";
            case 20: return "Chhatrapati Sambhaji Udyan";
            case 21: return "PMC";
            case 22: return "Civil Court";
            case 23: return "Mangalwar Peth";
            case 24: return "Pune Railway Station";
            case 25: return "Ruby Hall Clinic";
            case 26: return "Bund Garden";
            case 27: return "Yerawada";
            case 28: return "Kalayani Nagar";
            case 29: return "Ramwadi";
            default: return "Unknown Station";
        }
    }

    // ===== CORE ALGORITHM METHODS =====
    
    // Dijkstra's algorithm implementation
    public static void dijkstra(int sourceNumber, int destinationNumber) {
        boolean[] visited = new boolean[30]; // Only stations 0 to 29
        for (int i = 0; i < 30; i++) {
            predecessor[i] = -1; // Initialize to -1 for proper path reconstruction
            dist[i] = Float.MAX_VALUE;
            visited[i] = false;
        }

        dist[sourceNumber] = 0;

        for (int j = 0; j < 30; j++) {
            int minNode = minDistanceNode(dist, visited);
            if (minNode == -1)
                break;

            visited[minNode] = true;

            for (int k = 0; k < 30; k++) {
                if (!visited[k] && adj[minNode][k] != 0 && dist[minNode] != Float.MAX_VALUE
                        && dist[minNode] + adj[minNode][k] < dist[k]) {
                    predecessor[k] = minNode;
                    dist[k] = dist[minNode] + adj[minNode][k];
                }
            }
        }
    }

    public static int minDistanceNode(float[] dist, boolean[] visited) {
        float min = Float.MAX_VALUE;
        int minNode = -1;

        for (int i = 0; i < 30; i++) {
            if (!visited[i] && dist[i] < min) {
                min = dist[i];
                minNode = i;
            }
        }
        return minNode;
    }

    public static int station(String name) {
        name = name.trim().toLowerCase();

        if (name.equals("pcmc")) return 0;
        if (name.equals("sant tukaram nagar")) return 1;
        if (name.equals("bhosari") || name.equals("nashik phata") || name.equals("bhosari/nashik phata")) return 2;
        if (name.equals("kasarwadi")) return 3;
        if (name.equals("phugewadi")) return 4;
        if (name.equals("dapodi")) return 5;
        if (name.equals("bopodi")) return 6;
        if (name.equals("khadaki")) return 7;
        if (name.equals("range hill")) return 8;
        if (name.equals("shivaji nagar") || name.equals("shivajinagar")) return 9;
        if (name.equals("budhwar peth")) return 10;
        if (name.equals("mandai")) return 11;
        if (name.equals("swargate")) return 12;

        if (name.equals("chandani chowk")) return 13;
        if (name.equals("vanaz")) return 14;
        if (name.equals("anand nagar")) return 15;
        if (name.equals("ideal colony")) return 16;
        if (name.equals("nal stop")) return 17;
        if (name.equals("garware college")) return 18;
        if (name.equals("deccan gymkhana")) return 19;
        if (name.equals("chhatrapati sambhaji udyan")) return 20;
        if (name.equals("pmc")) return 21;
        if (name.equals("civil court")) return 22;
        if (name.equals("mangalwar peth")) return 23;
        if (name.equals("pune railway station")) return 24;
        if (name.equals("ruby hall clinic")) return 25;
        if (name.equals("bund garden")) return 26;
        if (name.equals("yerawada")) return 27;
        if (name.equals("kalayani nagar")) return 28;
        if (name.equals("ramwadi")) return 29;

        return -1;
    }

    public static void stationNotFound(String name) {
        System.out.println("Did you mean:-");

        if (checkStation(name, "PCMC")) {
            System.out.println("PCMC (search key)->0");
            push(0);
        }
        if (checkStation(name, "Sant Tukaram Nagar")) {
            System.out.println("Sant Tukaram Nagar (search key)->1");
            push(1);
        }
        if (checkStation(name, "Bhosari") || checkStation(name, "Nashik Phata")) {
            System.out.println("Bhosari/Nashik Phata (search key)->2");
            push(2);
        }
        if (checkStation(name, "Kasarwadi")) {
            System.out.println("Kasarwadi (search key)->3");
            push(3);
        }
        if (checkStation(name, "Phugewadi")) {
            System.out.println("Phugewadi (search key)->4");
            push(4);
        }
        if (checkStation(name, "Dapodi")) {
            System.out.println("Dapodi (search key)->5");
            push(5);
        }
        if (checkStation(name, "Bopodi")) {
            System.out.println("Bopodi (search key)->6");
            push(6);
        }
        if (checkStation(name, "Khadaki")) {
            System.out.println("Khadaki (search key)->7");
            push(7);
        }
        if (checkStation(name, "Range Hill")) {
            System.out.println("Range Hill (search key)->8");
            push(8);
        }
        if (checkStation(name, "Shivaji Nagar")) {
            System.out.println("Shivaji Nagar (search key)->9");
            push(9);
        }
        if (checkStation(name, "Budhwar Peth")) {
            System.out.println("Budhwar Peth (search key)->10");
            push(10);
        }
        if (checkStation(name, "Mandai")) {
            System.out.println("Mandai (search key)->11");
            push(11);
        }
        if (checkStation(name, "Swargate")) {
            System.out.println("Swargate (search key)->12");
            push(12);
        }

        // Aqua Line
        if (checkStation(name, "Chandani Chowk")) {
            System.out.println("Chandani Chowk (search key)->13");
            push(13);
        }
        if (checkStation(name, "Vanaz")) {
            System.out.println("Vanaz (search key)->14");
            push(14);
        }
        if (checkStation(name, "Anand Nagar")) {
            System.out.println("Anand Nagar (search key)->15");
            push(15);
        }
        if (checkStation(name, "Ideal Colony")) {
            System.out.println("Ideal Colony (search key)->16");
            push(16);
        }
        if (checkStation(name, "Nal Stop")) {
            System.out.println("Nal Stop (search key)->17");
            push(17);
        }
        if (checkStation(name, "Garware College")) {
            System.out.println("Garware College (search key)->18");
            push(18);
        }
        if (checkStation(name, "Deccan Gymkhana")) {
            System.out.println("Deccan Gymkhana (search key)->19");
            push(19);
        }
        if (checkStation(name, "Chhatrapati Sambhaji Udyan")) {
            System.out.println("Chhatrapati Sambhaji Udyan (search key)->20");
            push(20);
        }
        if (checkStation(name, "PMC")) {
            System.out.println("PMC (search key)->21");
            push(21);
        }
        if (checkStation(name, "Civil Court")) {
            System.out.println("Civil Court (search key)->22");
            push(22);
        }
        if (checkStation(name, "Mangalwar Peth")) {
            System.out.println("Mangalwar Peth (search key)->23");
            push(23);
        }
        if (checkStation(name, "Pune Railway Station")) {
            System.out.println("Pune Railway Station (search key)->24");
            push(24);
        }
        if (checkStation(name, "Ruby Hall Clinic")) {
            System.out.println("Ruby Hall Clinic (search key)->25");
            push(25);
        }
        if (checkStation(name, "Bund Garden")) {
            System.out.println("Bund Garden (search key)->26");
            push(26);
        }
        if (checkStation(name, "Yerawada")) {
            System.out.println("Yerawada (search key)->27");
            push(27);
        }
        if (checkStation(name, "Kalayani Nagar")) {
            System.out.println("Kalayani Nagar (search key)->28");
            push(28);
        }
        if (checkStation(name, "Ramwadi")) {
            System.out.println("Ramwadi (search key)->29");
            push(29);
        }
    }

    static void createGraph() {
        // Purple Line (PCMC to Swargate): 0 -> 1 -> 2 -> ... -> 9 -> 10 -> 11 -> 12
        adj[0][1] = adj[1][0] = 2.1f;   // PCMC <-> Sant Tukaram Nagar
        adj[1][2] = adj[2][1] = 0.7f;   // Sant Tukaram Nagar <-> Bhosari
        adj[2][3] = adj[3][2] = 1.5f;   // Bhosari <-> Kasarwadi
        adj[3][4] = adj[4][3] = 1.1f;   // Kasarwadi <-> Phugewadi
        adj[4][5] = adj[5][4] = 2.4f;   // Phugewadi <-> Dapodi
        adj[5][6] = adj[6][5] = 1.0f;   // Dapodi <-> Bopodi
        adj[6][7] = adj[7][6] = 1.4f;   // Bopodi <-> Khadaki
        adj[7][8] = adj[8][7] = 5.3f;   // Khadaki <-> Range Hill
        adj[8][9] = adj[9][8] = 2.9f;   // Range Hill <-> ShivajiNagar
        adj[9][10] = adj[10][9] = 1.1f; // ShivajiNagar <-> Budhwar Peth
        adj[10][11] = adj[11][10] = 0.65f; // Budhwar Peth <-> Mandai
        adj[11][12] = adj[12][11] = 1.7f;  // Mandai <-> Swargate

        // Aqua Line (Chandani Chowk to Ramwadi): 13 -> 14 -> ... -> 22 -> ... -> 29
        adj[13][14] = adj[14][13] = 1.2f;  // Chandani Chowk <-> Vanaz
        adj[14][15] = adj[15][14] = 0.95f; // Vanaz <-> Anand Nagar
        adj[15][16] = adj[16][15] = 1.2f;  // Anand Nagar <-> Ideal colony
        adj[16][17] = adj[17][16] = 1.0f;  // Ideal colony <-> Nal Stop
        adj[17][18] = adj[18][17] = 1.2f;  // Nal Stop <-> Garware College
        adj[18][19] = adj[19][18] = 0.9f;  // Garware College <-> Deccan Gymkhana
        adj[19][20] = adj[20][19] = 1.0f;  // Deccan Gymkhana <-> Chhatrapati Sambhaji Udyan
        adj[20][21] = adj[21][20] = 1.15f; // Chhatrapati Sambhaji Udyan <-> PMC
        adj[21][22] = adj[22][21] = 1.8f;  // PMC <-> Civil Court
        adj[22][23] = adj[23][22] = 1.7f;  // Civil Court <-> Mangalwar Peth
        adj[23][24] = adj[24][23] = 1.5f;  // Mangalwar Peth <-> Pune Railway Station
        adj[24][25] = adj[25][24] = 1.8f;  // Pune Railway Station <-> Ruby Hall Clinic
        adj[25][26] = adj[26][25] = 1.0f;  // Ruby Hall Clinic <-> Bund Garden
        adj[26][27] = adj[27][26] = 0.8f;  // Bund Garden <-> Yerawada
        adj[27][28] = adj[28][27] = 0.8f;  // Yerawada <-> Kalayani Nagar
        adj[28][29] = adj[29][28] = 0.8f;  // Kalayani Nagar <-> Ramwadi

        // Interchange connection: Shivaji Nagar (Purple) <-> Civil Court (Aqua)
        adj[9][22] = adj[22][9] = 0.5f;   // ShivajiNagar <-> Civil Court (interchange)
    }
}        
// Working with less stations
// import javafx.application.Application;
// import javafx.collections.ObservableList;
// import javafx.geometry.Insets;
// import javafx.geometry.Pos;
// import javafx.scene.Scene;
// import javafx.scene.control.*;
// import javafx.scene.layout.*;
// import javafx.scene.paint.Color;
// import javafx.scene.text.Font;
// import javafx.scene.text.FontWeight;
// import javafx.stage.Stage;
// import java.time.LocalTime;
// import java.util.*;
// import java.util.stream.Collectors;

// public class Main extends Application {
//     // Static variables and methods
//     static final int V = 30;
//     static final int MAX = 30;
//     static int[] predecessor = new int[30];
//     static int[] reversePath = new int[30];
//     static int[] correctPath = new int[30];
//     static String[] color = new String[30];
//     static Stack<Integer> stack = new Stack<>();
//     static float[][] adj = new float[MAX][MAX];
//     static float[] dist = new float[MAX];

//     // GUI components
//     private ComboBox<String> sourceComboBox;
//     private ComboBox<String> destinationComboBox;
//     private TextArea resultArea;
//     private List<String> stationNames;
//     private VBox routeDisplayBox;

//     public static void main(String[] args) {
//         launch(args);
//     }

//     @Override
//     public void start(Stage primaryStage) {
//         // Initialize data
//         giveColorToStation();
//         createGraph();
//         initializeStationNames();

//         // ===== Root Layout =====
//         routeDisplayBox = new VBox();
//         routeDisplayBox.setSpacing(10);
//         routeDisplayBox.setPadding(new Insets(20));

//         // ===== Header =====
//         Label header = new Label("PUNE METRO CONNECT");
//         header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
//         header.setTextFill(Color.WHITE);

//         // Theme toggle switch
//         ToggleButton themeToggle = new ToggleButton("🌞");
//         themeToggle.setFont(Font.font(18));

//         // Header container (Header + Toggle)
//         HBox headerBox = new HBox(20, header, themeToggle);
//         headerBox.setAlignment(Pos.CENTER_LEFT);
//         HBox.setHgrow(themeToggle, Priority.ALWAYS);
//         headerBox.setSpacing(10);

//         // ===== Input Grid =====
//         GridPane inputGrid = new GridPane();
//         inputGrid.setHgap(10);
//         inputGrid.setVgap(10);
//         inputGrid.setPadding(new Insets(10));

//         // Source
//         Label sourceLabel = new Label("Source Station:");
//         sourceLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
//         sourceComboBox = new ComboBox<>();
//         sourceComboBox.setPromptText("Select or type station name");
//         sourceComboBox.setEditable(true);
//         sourceComboBox.getItems().addAll(stationNames);

//         // Destination
//         Label destLabel = new Label("Destination Station:");
//         destLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
//         destinationComboBox = new ComboBox<>();
//         destinationComboBox.setPromptText("Select or type station name");
//         destinationComboBox.setEditable(true);
//         destinationComboBox.getItems().addAll(stationNames);

//         // Find Route Button
//         Button findRouteBtn = new Button("Find Route");
//         findRouteBtn.setPrefWidth(150);
//         findRouteBtn.setOnAction(e -> findRoute());

//         // Add to Grid
//         inputGrid.add(sourceLabel, 0, 0);
//         inputGrid.add(sourceComboBox, 1, 0);
//         inputGrid.add(destLabel, 0, 1);
//         inputGrid.add(destinationComboBox, 1, 1);
//         inputGrid.add(findRouteBtn, 1, 2);

//         // ===== Result Text Area =====
//         resultArea = new TextArea();
//         resultArea.setEditable(false);
//         resultArea.setWrapText(true);
//         resultArea.setPrefHeight(400);

//         // Add all UI elements to root
//         routeDisplayBox.getChildren().addAll(headerBox, inputGrid, resultArea);

//         // ===== Scene Setup =====
//         Scene scene = new Scene(routeDisplayBox, 700, 600);
        
//         // Apply basic styling since CSS files might not exist
//         routeDisplayBox.setStyle("-fx-background-color: #2b2b2b;");
//         header.setStyle("-fx-text-fill: white;");
//         sourceLabel.setStyle("-fx-text-fill: white;");
//         destLabel.setStyle("-fx-text-fill: white;");
//         resultArea.setStyle("-fx-control-inner-background: #3c3c3c; -fx-text-fill: white;");
//         inputGrid.setStyle("-fx-background-color: #2b2b2b;");
        
//         // Toggle Theme Logic
//         themeToggle.setOnAction(e -> {
//             if (themeToggle.isSelected()) {
//                 // Light theme
//                 routeDisplayBox.setStyle("-fx-background-color: #f0f0f0;");
//                 header.setStyle("-fx-text-fill: black;");
//                 sourceLabel.setStyle("-fx-text-fill: black;");
//                 destLabel.setStyle("-fx-text-fill: black;");
//                 resultArea.setStyle("-fx-control-inner-background: white; -fx-text-fill: black;");
//                 inputGrid.setStyle("-fx-background-color: #f0f0f0;");
//                 themeToggle.setText("🌜");
//             } else {
//                 // Dark theme
//                 routeDisplayBox.setStyle("-fx-background-color: #2b2b2b;");
//                 header.setStyle("-fx-text-fill: white;");
//                 sourceLabel.setStyle("-fx-text-fill: white;");
//                 destLabel.setStyle("-fx-text-fill: white;");
//                 resultArea.setStyle("-fx-control-inner-background: #3c3c3c; -fx-text-fill: white;");
//                 inputGrid.setStyle("-fx-background-color: #2b2b2b;");
//                 themeToggle.setText("🌞");
//             }
//         });

//         primaryStage.setTitle("Pune Metro Route Planner");
//         primaryStage.setScene(scene);
//         primaryStage.show();
//     }

//     // Helper to find the route as a list of station names
//     private List<String> findRouteBetweenStations(String source, String destination) {
//         int sourceIdx = getStationIndex(source);
//         int destIdx = getStationIndex(destination);
        
//         if (sourceIdx < 0 || destIdx < 0)
//             return Collections.emptyList();

//         dijkstra(sourceIdx, destIdx);

//         List<String> path = new ArrayList<>();
//         int current = destIdx;

//         while (current != sourceIdx) {
//             path.add(getStationName(current));
//             if (predecessor[current] == -1) {
//                 // No path found
//                 return Collections.emptyList();
//             }
//             current = predecessor[current];
//         }

//         path.add(getStationName(sourceIdx));
//         Collections.reverse(path);
//         return path;
//     }

//     private void initializeStationNames() {
//         stationNames = new ArrayList<>();
//         for (int i = 0; i < 30; i++) {
//             stationNames.add(getStationName(i).trim());
//         }
//         Collections.sort(stationNames);
//     }

//     // Helper method to check if the station name is similar to the target (case-insensitive, partial match)
//     public static boolean checkStation(String input, String target) {
//         if (input == null || target == null)
//             return false;
//         input = input.trim().toLowerCase();
//         target = target.trim().toLowerCase();
//         return input.contains(target) || target.contains(input);
//     }

//     public static int getStationIndex(String name) {
//         int idx = station(name);
//         return (idx != -1) ? idx : -1;
//     }

//     // Assign line colors based on index range for 30 stations
//     public static void giveColorToStation() {
//         // Purple Line: index 0 to 12
//         for (int i = 0; i <= 12; i++) {
//             color[i] = "Purple";
//         }

//         // Aqua Line: index 13 to 29
//         for (int i = 13; i < 30; i++) {
//             color[i] = "Aqua";
//         }
//     }

//     public static void push(int data) {
//         if (stack.size() >= 100) {
//             System.out.println("Stack Overflow");
//             return;
//         }
//         stack.push(data);
//     }

//     private static int getArrivalMinute(int src, boolean forward) {
//         // Applies only to stations 0–29
//         if (src < 0 || src >= 30)
//             return 0;

//         if (forward) {
//             if (src == 0 || src == 13) // Start of Purple (0) and Aqua (13)
//                 return 0;
//             if ((src >= 1 && src <= 4) || (src >= 14 && src <= 18))
//                 return 5;
//             if ((src >= 5 && src <= 8) || (src >= 19 && src <= 22))
//                 return 10;
//             if ((src >= 9 && src <= 11) || (src >= 23 && src <= 25))
//                 return 15;
//             if (src == 12 || src == 26 || src == 27)
//                 return 20;
//             if (src == 28 || src == 29)
//                 return 25;
//         } else {
//             if (src == 12 || src == 29) // End of Purple (12) and Aqua (29)
//                 return 30;
//             if ((src >= 1 && src <= 4) || (src >= 14 && src <= 18))
//                 return 25;
//             if ((src >= 5 && src <= 8) || (src >= 19 && src <= 22))
//                 return 20;
//             if ((src >= 9 && src <= 11) || (src >= 23 && src <= 25))
//                 return 15;
//             if (src == 0 || src == 13)
//                 return 10;
//         }

//         return 0; // fallback
//     }

//     private void findRoute() {
//         String sourceName = sourceComboBox.getValue();
//         String destinationName = destinationComboBox.getValue();

//         if (sourceName == null || destinationName == null) {
//             showAlert("Please select both source and destination stations");
//             return;
//         }

//         if (sourceName.equals(destinationName)) {
//             showAlert("Source and destination stations cannot be the same");
//             return;
//         }

//         int sourceNumber = getStationIndex(sourceName);
//         int destinationNumber = getStationIndex(destinationName);

//         if (sourceNumber < 0 || destinationNumber < 0 || sourceNumber >= 30 || destinationNumber >= 30) {
//             showAlert("One or more stations not found");
//             return;
//         }

//         String sourceColor = color[sourceNumber];

//         dijkstra(sourceNumber, destinationNumber);
//         float weight = dist[destinationNumber];

//         StringBuilder result = new StringBuilder();
//         result.append("🚇 Pune Metro Route Details\n\n");
//         result.append("From: ").append(sourceName).append("\n");
//         result.append("To: ").append(destinationName).append("\n\n");

//         findPath(sourceNumber, destinationNumber, sourceColor, result);
//         result.append(String.format("\nDistance to travel: %.2f Km\n", weight));

//         // Path calculation for time & interchange
//         List<Integer> route = getPathAsList(sourceNumber, destinationNumber);
//         int numberOfHops = route.size() - 1;
//         int estimatedTime = numberOfHops * 2; // 2 mins per edge
//         int interchanges = 0;

//         // Check for interchanges at Civil Court (station 22)
//         for (int i = 1; i < route.size() - 1; i++) {
//             int prev = route.get(i - 1);
//             int curr = route.get(i);
//             int next = route.get(i + 1);

//             // Check if current station is Civil Court and there's a line change
//             if (curr == 22) { // Civil Court station index
//                 String prevColor = color[prev];
//                 String nextColor = color[next];
//                 if (!prevColor.equals(nextColor)) {
//                     interchanges++;
//                     estimatedTime += 5; // extra time for interchange
//                 }
//             }
//         }

//         // Convert to hours/minutes
//         if (estimatedTime >= 60) {
//             int hours = estimatedTime / 60;
//             int minutes = estimatedTime % 60;
//             result.append(String.format("\n🕒 Estimated Time: %d hour%s %d minute%s",
//                     hours, (hours != 1 ? "s" : ""),
//                     minutes, (minutes != 1 ? "s" : "")));
//         } else {
//             result.append("\n🕒 Estimated Time: ").append(estimatedTime).append(" minute").append(estimatedTime != 1 ? "s" : "");
//         }

//         // Add interchange note
//         if (interchanges > 0) {
//             result.append(" (Includes ").append(interchanges).append(" interchange");
//             result.append(interchanges > 1 ? "s" : "").append(" at Civil Court)");
//         }

//         resultArea.setText(result.toString());
//     }

//     private List<Integer> getPathAsList(int source, int dest) {
//         List<Integer> path = new ArrayList<>();
//         int current = dest;

//         while (current != -1) {
//             path.add(0, current); // prepend to path
//             if (current == source) break;
//             current = predecessor[current];
//         }

//         return path;
//     }

//     private void showAlert(String message) {
//         Alert alert = new Alert(Alert.AlertType.WARNING);
//         alert.setTitle("Input Error");
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }

//     // Modified findPath to use StringBuilder instead of System.out
//     static void findPath(int sourceNumber, int destinationNumber, String sourceColor, StringBuilder result) {
//         int count = 0;
//         while (destinationNumber != sourceNumber) {
//             reversePath[count] = destinationNumber;
//             int u = predecessor[destinationNumber];
//             destinationNumber = u;
//             count++;
//         }

//         reversePath[count++] = sourceNumber;

//         for (int j = 0, i = count - 1; j < count && i >= 0; j++, i--) {
//             correctPath[j] = reversePath[i];
//         }

//         if (count > 1) {
//             int currentSource = correctPath[0];
//             int nextToCurrentSource = correctPath[1];

//             // Display time information
//             LocalTime time = LocalTime.now();
//             int hour = time.getHour();
//             int minute = time.getMinute();

//             if (hour >= 8 && hour <= 22) {
//                 int arrivalMinute = getArrivalMinute(sourceNumber,
//                         currentSource - nextToCurrentSource == -1 ||
//                         currentSource - nextToCurrentSource == 13);

//                 if (minute <= arrivalMinute) {
//                     result.append(String.format("Next metro arrives at: %02d:%02d%n", hour, arrivalMinute));
//                 } else {
//                     result.append(String.format("Next metro arrives at: %02d:%02d%n", (hour + 1) % 24, arrivalMinute));
//                 }
//             } else {
//                 result.append("Metro is not available now (operates 8AM–10PM)\n");
//             }

//             // Direction information
//             if (currentSource - nextToCurrentSource == -1 || currentSource - nextToCurrentSource == 13) {
//                 result.append(getDirectionMessage(sourceNumber, currentSource, nextToCurrentSource, true));
//             } else if (currentSource - nextToCurrentSource == 1 || currentSource - nextToCurrentSource == -13) {
//                 result.append(getDirectionMessage(sourceNumber, currentSource, nextToCurrentSource, false));
//             }
//         }

//         result.append("\n📍 Your Journey:\n\n");

//         StringBuilder pathVisual = new StringBuilder();
//         for (int i = 0; i < count; i++) {
//             // Check for interchange points
//             if (correctPath[i] == 9 || correctPath[i] == 22) {
//                 result.append(solveConflict(i, count));
//             }
//             pathVisual.append(getStationName(correctPath[i]));
//             if (i < count - 1) {
//                 pathVisual.append(" → ");
//             }
//         }
//         result.append(pathVisual).append("\n");
//     }

//     private static String getDirectionMessage(int sourceNumber, int currentSource, int nextToCurrentSource, boolean forward) {
//         if (forward) {
//             if (currentSource - nextToCurrentSource == 13) {
//                 return "Board metro heading towards Ramwadi\n";
//             } else if (color[sourceNumber].equals("Purple")) {
//                 return "Board metro heading towards Swargate\n";
//             } else {
//                 return "Board metro heading towards Ramwadi\n";
//             }
//         } else {
//             if (currentSource - nextToCurrentSource == -13) {
//                 return "Board metro heading towards PCMC\n";
//             } else if (color[sourceNumber].equals("Purple")) {
//                 return "Board metro heading towards PCMC\n";
//             } else {
//                 return "Board metro heading towards Chandani Chowk\n";
//             }
//         }
//     }

//     // Modified solveConflict to return String instead of printing
//     static String solveConflict(int i, int count) {
//         if (i <= 0 || i >= count - 1) return "";
        
//         int current = correctPath[i];
//         int prev = correctPath[i - 1];
//         int next = correctPath[i + 1];
//         StringBuilder message = new StringBuilder();

//         // Junction: Shivaji Nagar (connection point to Civil Court)
//         if (current == 9) {
//             if (!color[prev].equals(color[next])) {
//                 if (color[prev].equals("Purple") && color[next].equals("Aqua")) {
//                     message.append("\nChange route: Walk to Civil Court and board metro towards Ramwadi (Aqua Line)\n");
//                 } else if (color[prev].equals("Aqua") && color[next].equals("Purple")) {
//                     message.append("\nChange route: Walk to Civil Court and board metro towards Swargate (Purple Line)\n");
//                 }
//             }
//         }
//         // Junction: Civil Court (main interchange station)
//         else if (current == 22) {
//             if (!color[prev].equals(color[next])) {
//                 if (color[prev].equals("Aqua") && color[next].equals("Purple")) {
//                     if (next > current) {
//                         message.append("\nChange route: Board metro towards Swargate (Purple Line)\n");
//                     } else {
//                         message.append("\nChange route: Board metro towards PCMC (Purple Line)\n");
//                     }
//                 } else if (color[prev].equals("Purple") && color[next].equals("Aqua")) {
//                     if (next > current) {
//                         message.append("\nChange route: Board metro towards Ramwadi (Aqua Line)\n");
//                     } else {
//                         message.append("\nChange route: Board metro towards Chandani Chowk (Aqua Line)\n");
//                     }
//                 }
//             }
//         }

//         return message.toString();
//     }

//     public static String getStationName(int stationIndex) {
//         return switch (stationIndex) {
//             case 0 -> "PCMC";
//             case 1 -> "Sant Tukaram Nagar";
//             case 2 -> "Bhosari/Nashik Phata";
//             case 3 -> "Kasarwadi";
//             case 4 -> "Phugewadi";
//             case 5 -> "Dapodi";
//             case 6 -> "Bopodi";
//             case 7 -> "Khadaki";
//             case 8 -> "Range Hill";
//             case 9 -> "ShivajiNagar";
//             case 10 -> "Budhwar Peth";
//             case 11 -> "Mandai";
//             case 12 -> "Swargate";
//             case 13 -> "Chandani Chowk";
//             case 14 -> "Vanaz";
//             case 15 -> "Anand Nagar";
//             case 16 -> "Ideal colony";
//             case 17 -> "Nal Stop";
//             case 18 -> "Garware College";
//             case 19 -> "Deccan Gymkhana";
//             case 20 -> "Chhatrapati Sambhaji Udyan";
//             case 21 -> "PMC";
//             case 22 -> "Civil Court";
//             case 23 -> "Mangalwar Peth";
//             case 24 -> "Pune Railway Station";
//             case 25 -> "Ruby Hall Clinic";
//             case 26 -> "Bund Garden";
//             case 27 -> "Yerawada";
//             case 28 -> "Kalayani Nagar";
//             case 29 -> "Ramwadi";
//             default -> "Unknown Station";
//         };
//     }

//     public static void dijkstra(int sourceNumber, int destinationNumber) {
//         boolean[] visited = new boolean[30]; // Only stations 0 to 29
//         for (int i = 0; i < 30; i++) {
//             predecessor[i] = -1; // Initialize to -1 for proper path reconstruction
//             dist[i] = Float.MAX_VALUE;
//             visited[i] = false;
//         }

//         dist[sourceNumber] = 0;

//         for (int j = 0; j < 30; j++) {
//             int minNode = minDistanceNode(dist, visited);
//             if (minNode == -1)
//                 break;

//             visited[minNode] = true;

//             for (int k = 0; k < 30; k++) {
//                 if (!visited[k] && adj[minNode][k] != 0 && dist[minNode] != Float.MAX_VALUE
//                         && dist[minNode] + adj[minNode][k] < dist[k]) {
//                     predecessor[k] = minNode;
//                     dist[k] = dist[minNode] + adj[minNode][k];
//                 }
//             }
//         }
//     }

//     public static int minDistanceNode(float[] dist, boolean[] visited) {
//         float min = Float.MAX_VALUE;
//         int minNode = -1;

//         for (int i = 0; i < 30; i++) {
//             if (!visited[i] && dist[i] < min) {
//                 min = dist[i];
//                 minNode = i;
//             }
//         }
//         return minNode;
//     }

//     public static int station(String name) {
//         name = name.trim().toLowerCase();

//         if (name.equals("pcmc")) return 0;
//         if (name.equals("sant tukaram nagar")) return 1;
//         if (name.equals("bhosari") || name.equals("nashik phata") || name.equals("bhosari/nashik phata")) return 2;
//         if (name.equals("kasarwadi")) return 3;
//         if (name.equals("phugewadi")) return 4;
//         if (name.equals("dapodi")) return 5;
//         if (name.equals("bopodi")) return 6;
//         if (name.equals("khadaki")) return 7;
//         if (name.equals("range hill")) return 8;
//         if (name.equals("shivaji nagar") || name.equals("shivajinagar")) return 9;
//         if (name.equals("budhwar peth")) return 10;
//         if (name.equals("mandai")) return 11;
//         if (name.equals("swargate")) return 12;

//         if (name.equals("chandani chowk")) return 13;
//         if (name.equals("vanaz")) return 14;
//         if (name.equals("anand nagar")) return 15;
//         if (name.equals("ideal colony")) return 16;
//         if (name.equals("nal stop")) return 17;
//         if (name.equals("garware college")) return 18;
//         if (name.equals("deccan gymkhana")) return 19;
//         if (name.equals("chhatrapati sambhaji udyan")) return 20;
//         if (name.equals("pmc")) return 21;
//         if (name.equals("civil court")) return 22;
//         if (name.equals("mangalwar peth")) return 23;
//         if (name.equals("pune railway station")) return 24;
//         if (name.equals("ruby hall clinic")) return 25;
//         if (name.equals("bund garden")) return 26;
//         if (name.equals("yerawada")) return 27;
//         if (name.equals("kalayani nagar")) return 28;
//         if (name.equals("ramwadi")) return 29;

//         return -1;
//     }

//     public static void stationNotFound(String name) {
//         System.out.println("Did you mean:-");

//         if (checkStation(name, "PCMC")) {
//             System.out.println("PCMC (search key)->0");
//             push(0);
//         }
//         if (checkStation(name, "Sant Tukaram Nagar")) {
//             System.out.println("Sant Tukaram Nagar (search key)->1");
//             push(1);
//         }
//         if (checkStation(name, "Bhosari") || checkStation(name, "Nashik Phata")) {
//             System.out.println("Bhosari/Nashik Phata (search key)->2");
//             push(2);
//         }
//         if (checkStation(name, "Kasarwadi")) {
//             System.out.println("Kasarwadi (search key)->3");
//             push(3);
//         }
//         if (checkStation(name, "Phugewadi")) {
//             System.out.println("Phugewadi (search key)->4");
//             push(4);
//         }
//         if (checkStation(name, "Dapodi")) {
//             System.out.println("Dapodi (search key)->5");
//             push(5);
//         }
//         if (checkStation(name, "Bopodi")) {
//             System.out.println("Bopodi (search key)->6");
//             push(6);
//         }
//         if (checkStation(name, "Khadaki")) {
//             System.out.println("Khadaki (search key)->7");
//             push(7);
//         }
//         if (checkStation(name, "Range Hill")) {
//             System.out.println("Range Hill (search key)->8");
//             push(8);
//         }
//         if (checkStation(name, "Shivaji Nagar")) {
//             System.out.println("Shivaji Nagar (search key)->9");
//             push(9);
//         }
//         if (checkStation(name, "Budhwar Peth")) {
//             System.out.println("Budhwar Peth (search key)->10");
//             push(10);
//         }
//         if (checkStation(name, "Mandai")) {
//             System.out.println("Mandai (search key)->11");
//             push(11);
//         }
//         if (checkStation(name, "Swargate")) {
//             System.out.println("Swargate (search key)->12");
//             push(12);
//         }

//         // Aqua Line
//         if (checkStation(name, "Chandani Chowk")) {
//             System.out.println("Chandani Chowk (search key)->13");
//             push(13);
//         }
//         if (checkStation(name, "Vanaz")) {
//             System.out.println("Vanaz (search key)->14");
//             push(14);
//         }
//         if (checkStation(name, "Anand Nagar")) {
//             System.out.println("Anand Nagar (search key)->15");
//             push(15);
//         }
//         if (checkStation(name, "Ideal Colony")) {
//             System.out.println("Ideal Colony (search key)->16");
//             push(16);
//         }
//         if (checkStation(name, "Nal Stop")) {
//             System.out.println("Nal Stop (search key)->17");
//             push(17);
//         }
//         if (checkStation(name, "Garware College")) {
//             System.out.println("Garware College (search key)->18");
//             push(18);
//         }
//         if (checkStation(name, "Deccan Gymkhana")) {
//             System.out.println("Deccan Gymkhana (search key)->19");
//             push(19);
//         }
//         if (checkStation(name, "Chhatrapati Sambhaji Udyan")) {
//             System.out.println("Chhatrapati Sambhaji Udyan (search key)->20");
//             push(20);
//         }
//         if (checkStation(name, "PMC")) {
//             System.out.println("PMC (search key)->21");
//             push(21);
//         }
//         if (checkStation(name, "Civil Court")) {
//             System.out.println("Civil Court (search key)->22");
//             push(22);
//         }
//         if (checkStation(name, "Mangalwar Peth")) {
//             System.out.println("Mangalwar Peth (search key)->23");
//             push(23);
//         }
//         if (checkStation(name, "Pune Railway Station")) {
//             System.out.println("Pune Railway Station (search key)->24");
//             push(24);
//         }
//         if (checkStation(name, "Ruby Hall Clinic")) {
//             System.out.println("Ruby Hall Clinic (search key)->25");
//             push(25);
//         }
//         if (checkStation(name, "Bund Garden")) {
//             System.out.println("Bund Garden (search key)->26");
//             push(26);
//         }
//         if (checkStation(name, "Yerawada")) {
//             System.out.println("Yerawada (search key)->27");
//             push(27);
//         }
//         if (checkStation(name, "Kalayani Nagar")) {
//             System.out.println("Kalayani Nagar (search key)->28");
//             push(28);
//         }
//         if (checkStation(name, "Ramwadi")) {
//             System.out.println("Ramwadi (search key)->29");
//             push(29);
//         }
//     }

//     static void createGraph() {
//         // Purple Line (PCMC to Swargate): 0 -> 1 -> 2 -> ... -> 9 -> 10 -> 11 -> 12
//         adj[0][1] = adj[1][0] = 2.1f;   // PCMC <-> Sant Tukaram Nagar
//         adj[1][2] = adj[2][1] = 0.7f;   // Sant Tukaram Nagar <-> Bhosari
//         adj[2][3] = adj[3][2] = 1.5f;   // Bhosari <-> Kasarwadi
//         adj[3][4] = adj[4][3] = 1.1f;   // Kasarwadi <-> Phugewadi
//         adj[4][5] = adj[5][4] = 2.4f;   // Phugewadi <-> Dapodi
//         adj[5][6] = adj[6][5] = 1.0f;   // Dapodi <-> Bopodi
//         adj[6][7] = adj[7][6] = 1.4f;   // Bopodi <-> Khadaki
//         adj[7][8] = adj[8][7] = 5.3f;   // Khadaki <-> Range Hill
//         adj[8][9] = adj[9][8] = 2.9f;   // Range Hill <-> ShivajiNagar
//         adj[9][10] = adj[10][9] = 1.1f; // ShivajiNagar <-> Budhwar Peth
//         adj[10][11] = adj[11][10] = 0.65f; // Budhwar Peth <-> Mandai
//         adj[11][12] = adj[12][11] = 1.7f;  // Mandai <-> Swargate

//         // Aqua Line (Chandani Chowk to Ramwadi): 13 -> 14 -> ... -> 22 -> ... -> 29
//         adj[13][14] = adj[14][13] = 1.2f;  // Chandani Chowk <-> Vanaz
//         adj[14][15] = adj[15][14] = 0.95f; // Vanaz <-> Anand Nagar
//         adj[15][16] = adj[16][15] = 1.2f;  // Anand Nagar <-> Ideal colony
//         adj[16][17] = adj[17][16] = 1.0f;  // Ideal colony <-> Nal Stop
//         adj[17][18] = adj[18][17] = 1.2f;  // Nal Stop <-> Garware College
//         adj[18][19] = adj[19][18] = 0.9f;  // Garware College <-> Deccan Gymkhana
//         adj[19][20] = adj[20][19] = 1.0f;  // Deccan Gymkhana <-> Chhatrapati Sambhaji Udyan
//         adj[20][21] = adj[21][20] = 1.15f; // Chhatrapati Sambhaji Udyan <-> PMC
//         adj[21][22] = adj[22][21] = 1.8f;  // PMC <-> Civil Court
//         adj[22][23] = adj[23][22] = 1.7f;  // Civil Court <-> Mangalwar Peth
//         adj[23][24] = adj[24][23] = 1.5f;  // Mangalwar Peth <-> Pune Railway Station
//         adj[24][25] = adj[25][24] = 1.8f;  // Pune Railway Station <-> Ruby Hall Clinic
//         adj[25][26] = adj[26][25] = 1.0f;  // Ruby Hall Clinic <-> Bund Garden
//         adj[26][27] = adj[27][26] = 0.8f;  // Bund Garden <-> Yerawada
//         adj[27][28] = adj[28][27] = 0.8f;  // Yerawada <-> Kalayani Nagar
//         adj[28][29] = adj[29][28] = 0.8f;  // Kalayani Nagar <-> Ramwadi

//         // Interchange connection: Shivaji Nagar (Purple) <-> Civil Court (Aqua)
//         adj[9][22] = adj[22][9] = 0.5f;   // ShivajiNagar <-> Civil Court (interchange)
//     }
// }

// //Code 1
//Code with dark and light mode
// import javafx.application.Application;
// import javafx.collections.ObservableList;
// import javafx.geometry.Insets;
// import javafx.geometry.Pos;
// import javafx.scene.Scene;
// import javafx.scene.control.*;
// import javafx.scene.layout.*;
// import javafx.scene.paint.Color;
// import javafx.scene.text.Font;
// import javafx.scene.text.FontWeight;
// import javafx.stage.Stage;
// import java.time.LocalTime;
// import java.util.*;
// import java.util.stream.Collectors;

// public class Main extends Application {
//     // Your existing static variables and methods remain the same
//     static final int V = 91;
//     static final int MAX = 91;
//     static int[] predecessor = new int[100];
//     static int[] reversePath = new int[100];
//     static int[] correctPath = new int[100];
//     static String[] color = new String[100];
//     static Stack<Integer> stack = new Stack<>();
//     static float[][] adj = new float[MAX][MAX];
//     static float[] dist = new float[MAX];

//     // New GUI components
//     private ComboBox<String> sourceComboBox;
//     private ComboBox<String> destinationComboBox;
//     private TextArea resultArea;
//     private List<String> stationNames;
//     private VBox routeDisplayBox;

//     public static void main(String[] args) {
//         launch(args);
//     }

// @Override
// public void start(Stage primaryStage) {
//     // Initialize data
//     giveColorToStation();
//     createGraph();
//     initializeStationNames();

//     // ===== Root Layout =====
//     routeDisplayBox = new VBox();
//     routeDisplayBox.setSpacing(10);
//     routeDisplayBox.setPadding(new Insets(20));

//     // ===== Header =====
//     Label header = new Label("PUNE METRO CONNECT");
//     header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
//     header.setTextFill(Color.WHITE);

//     // Theme toggle switch
//     ToggleButton themeToggle = new ToggleButton("🌞");
//     themeToggle.setFont(Font.font(18));

//     // Header container (Header + Toggle)
//     HBox headerBox = new HBox(20, header, themeToggle);
//     headerBox.setAlignment(Pos.CENTER_LEFT);
//     HBox.setHgrow(themeToggle, Priority.ALWAYS);
//     headerBox.setSpacing(10);

//     // ===== Input Grid =====
//     GridPane inputGrid = new GridPane();
//     inputGrid.setHgap(10);
//     inputGrid.setVgap(10);
//     inputGrid.setPadding(new Insets(10));

//     // Source
//     Label sourceLabel = new Label("Source Station:");
//     sourceLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
//     sourceComboBox = new ComboBox<>();
//     sourceComboBox.setPromptText("Select or type station name");
//     sourceComboBox.setEditable(true);
//     sourceComboBox.getItems().addAll(stationNames);

//     // Destination
//     Label destLabel = new Label("Destination Station:");
//     destLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
//     destinationComboBox = new ComboBox<>();
//     destinationComboBox.setPromptText("Select or type station name");
//     destinationComboBox.setEditable(true);
//     destinationComboBox.getItems().addAll(stationNames);

//     // Find Route Button
//     Button findRouteBtn = new Button("Find Route");
//     findRouteBtn.setPrefWidth(150);
//     findRouteBtn.setOnAction(e -> findRoute());

//     // Add to Grid
//     inputGrid.add(sourceLabel, 0, 0);
//     inputGrid.add(sourceComboBox, 1, 0);
//     inputGrid.add(destLabel, 0, 1);
//     inputGrid.add(destinationComboBox, 1, 1);
//     inputGrid.add(findRouteBtn, 1, 2);

//     // ===== Result Text Area =====
//     resultArea = new TextArea();
//     resultArea.setEditable(false);
//     resultArea.setWrapText(true);
//     resultArea.setPrefHeight(400);

//     // Add all UI elements to root
//     routeDisplayBox.getChildren().addAll(headerBox, inputGrid, resultArea);

//     // ===== Scene Setup =====
//     Scene scene = new Scene(routeDisplayBox, 700, 600);
//     scene.getStylesheets().add(getClass().getResource("dark.css").toExternalForm());

//     // Toggle Theme Logic
//     themeToggle.setOnAction(e -> {
//         scene.getStylesheets().clear();
//         if (themeToggle.isSelected()) {
//             scene.getStylesheets().add(getClass().getResource("light.css").toExternalForm());
//             themeToggle.setText("🌜");
//         } else {
//             scene.getStylesheets().add(getClass().getResource("dark.css").toExternalForm());
//             themeToggle.setText("🌞");
//         }
//     });

//     primaryStage.setTitle("Pune Metro Route Planner");
//     primaryStage.setScene(scene);
//     primaryStage.show();
// }

//     // Helper to find the route as a list of station names
//     private List<String> findRouteBetweenStations(String source, String destination) {
//         int sourceIdx = getStationIndex(source);
//         int destIdx = getStationIndex(destination);
//         if (sourceIdx < 0 || destIdx < 0)
//             return Collections.emptyList();

//         dijkstra(sourceIdx, destIdx);

//         List<String> path = new ArrayList<>();
//         int current = destIdx;
//         while (current != sourceIdx) {
//             path.add(getStationName(current));
//             if (predecessor[current] == 0 && current != sourceIdx) {
//                 // No path found
//                 return Collections.emptyList();
//             }
//             current = predecessor[current];
//         }
//         path.add(getStationName(sourceIdx));
//         Collections.reverse(path);
//         return path;
//     }

//     private void initializeStationNames() {
//         stationNames = new ArrayList<>();
//         for (int i = 0; i <= 48; i++) {
//             stationNames.add(getStationName(i).replace("", "").trim());
//         }
//         Collections.sort(stationNames);
//     }

//     // Helper method to check if the station name is similar to the target (case-insensitive, partial match)
//     public static boolean checkStation(String input, String target) {
//         if (input == null || target == null)
//             return false;
//         input = input.trim().toLowerCase();
//         target = target.trim().toLowerCase();
//         return input.contains(target) || target.contains(input);
//     }

//     public static int getStationIndex(String name) {
//         int idx = station(name);
//         if (idx != -1) {
//             return idx;
//         } else {
//             return -50; // mimic the original error handling
//         }
//     }

//     public static void giveColorToStation() {
//         for (int i = 0; i <= 19; i++) {
//             color[i] = "Purple";
//         }

//         for (int i = 20; i <= 48; i++) {
//             color[i] = "Aqua";
//         }

//     }

//     public static void push(int data) {
//         if (stack.size() >= 100) {
//             System.out.println("Stack Overflow");
//             return;
//         }
//         stack.push(data);
//     }

//     private static int getArrivalMinute(int src, boolean forward) {
//         // Applies only for Purple (0–19) and Aqua (20–48)
//         if (src > 48)
//             return 0;

//         if (forward) {
//             if (src == 0 || src == 20)
//                 return 0;
//             if ((src >= 1 && src <= 4) || (src >= 21 && src <= 25))
//                 return 5;
//             if ((src >= 5 && src <= 8) || (src >= 26 && src <= 31))
//                 return 10;
//             if ((src >= 9 && src <= 11) || (src >= 32 && src <= 37))
//                 return 15;
//             if ((src >= 12 && src <= 15) || (src >= 38 && src <= 42))
//                 return 20;
//             if ((src >= 16 && src <= 18) || (src >= 43 && src <= 47))
//                 return 25;
//         } else {
//             if (src == 19 || src == 48)
//                 return 30;
//             if ((src >= 1 && src <= 4) || (src >= 21 && src <= 25))
//                 return 25;
//             if ((src >= 5 && src <= 8) || (src >= 26 && src <= 31))
//                 return 25;
//             if ((src >= 9 && src <= 11) || (src >= 32 && src <= 37))
//                 return 15;
//             if ((src >= 12 && src <= 15) || (src >= 38 && src <= 42))
//                 return 10;
//             if ((src >= 16 && src <= 18) || (src >= 43 && src <= 47))
//                 return 5;
//         }

//         return 0; // fallback
//     }

//     private void findRoute() {
//         String sourceName = sourceComboBox.getValue();
//         String destinationName = destinationComboBox.getValue();

//         if (sourceName == null || destinationName == null) {
//             showAlert("Please select both source and destination stations");
//             return;
//         }

//         if (sourceName.equals(destinationName)) {
//             showAlert("Source and destination stations cannot be the same");
//             return;
//         }

//         int sourceNumber = getStationIndex(sourceName);
//         int destinationNumber = getStationIndex(destinationName);

//         if (sourceNumber == -50 || destinationNumber == -50) {
//             showAlert("One or more stations not found");
//             return;
//         }

//         String sourceColor = color[sourceNumber];
//         String destinationColor = color[destinationNumber];

//         dijkstra(sourceNumber, destinationNumber);
//         float weight = dist[destinationNumber];

//         StringBuilder result = new StringBuilder();
//         result.append("🚇 Pune Metro Route Details\n\n");
//         result.append("From: ").append(sourceName).append("\n");
//         result.append("To: ").append(destinationName).append("\n\n");

//         findPath(sourceNumber, destinationNumber, sourceColor, result);

//         result.append("\nYou will reach your final destination\n");
//         result.append(String.format("\nDistance to travel: %.2f Km\n", weight));

//         if (weight <= 25) {
//             result.append("\nFare required: Rs. 10");
//         } else {
//             result.append("\nFare required: Rs. 20");
//         }

//         resultArea.setText(result.toString());
//     }

//     private void showAlert(String message) {
//         Alert alert = new Alert(Alert.AlertType.WARNING);
//         alert.setTitle("Input Error");
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }

//     // Modified findPath to use StringBuilder instead of System.out
//     static void findPath(int sourceNumber, int destinationNumber, String sourceColor, StringBuilder result) {
//         int count = 0;
//         while (destinationNumber != sourceNumber) {
//             reversePath[count] = destinationNumber;
//             int u = predecessor[destinationNumber];
//             destinationNumber = u;
//             count++;
//         }

//         reversePath[count++] = sourceNumber;

//         for (int j = 0, i = count; j <= count && i >= 0; j++, i--) {
//             correctPath[j] = reversePath[i];
//         }

//         int currentSource = correctPath[1];
//         int nextToCurrentSource = correctPath[2];

//         // Display time information
//         LocalTime time = LocalTime.now();
//         int hour = time.getHour();
//         int minute = time.getMinute();

//         if (hour >= 8 && hour <= 22) {
//             int arrivalMinute = getArrivalMinute(sourceNumber, (currentSource - nextToCurrentSource == -1 ||
//                     currentSource - nextToCurrentSource == 20 || currentSource - nextToCurrentSource == 19));

//             if (minute <= arrivalMinute) {
//                 result.append(String.format("Next metro arrives at: %02d:%02d%n", hour, arrivalMinute));
//             } else {
//                 result.append(String.format("Next metro arrives at: %02d:%02d%n", (hour + 1) % 24, arrivalMinute));
//             }
//         } else {
//             result.append("Metro is not available now (operates 8AM-10PM)\n");
//         }

//         // Direction information
//         if (currentSource - nextToCurrentSource == -1 || currentSource - nextToCurrentSource == 20
//                 || currentSource - nextToCurrentSource == 19) {
//             result.append(getDirectionMessage(sourceNumber, currentSource, nextToCurrentSource, true));
//         } else if (currentSource - nextToCurrentSource == 1 || currentSource - nextToCurrentSource == -20) {
//             result.append(getDirectionMessage(sourceNumber, currentSource, nextToCurrentSource, false));
//         }

//         result.append("\n📍 Your Journey:\n\n");

//         StringBuilder pathVisual = new StringBuilder();
//         for (int i = 1; i <= count; i++) {
//             if (correctPath[i] == 9 || correctPath[i] == 12 || correctPath[i] == 29) {
//                 result.append(solveConflict(i));
//             }
//             pathVisual.append(getStationName(correctPath[i]));
//             if (i < count) {
//                 pathVisual.append(" → ");
//             }
//         }
//         result.append(pathVisual).append("\n");
//     }

//     private static String getDirectionMessage(int sourceNumber, int currentSource, int nextToCurrentSource,
//             boolean right) {
//         if (right) {
//             if (currentSource - nextToCurrentSource == 20) {
//                 return "Board metro heading towards PCMC\n";
//             } else if (currentSource - nextToCurrentSource == 19) {
//                 return "Board metro heading towards Katraj\n";
//             } else if (color[sourceNumber].equals("Purple")) {
//                 return "Board metro heading towards Katraj\n";
//             } else {
//                 return "Board metro heading towards Wagholi\n";
//             }
//         } else {
//             if (currentSource - nextToCurrentSource == -20) {
//                 return "Board metro heading towards Katraj\n";
//             } else if (color[sourceNumber].equals("Purple")) {
//                 return "Board metro heading towards PCMC\n";
//             } else {
//                 return "Board metro heading towards Chandani Chowk\n";
//             }
//         }
//     }

//     // Modified solveConflict to return String instead of printing
//     static String solveConflict(int i) {
//         int current = correctPath[i];
//         int prev = correctPath[i - 1];
//         int next = correctPath[i + 1];
//         StringBuilder message = new StringBuilder();

//         // Junction: Shivaji Nagar
//         if (current == 9) {
//             if (!color[prev].equals(color[next])) {
//                 if (color[prev].equals("Purple") && color[next].equals("Aqua") && next == 28) {
//                     message.append("\nChange route: Board metro towards Chandani Chowk\n");
//                 }
//             }
//         }
//         // Junction: Civil Court
//         else if (current == 29) {
//             int next2 = correctPath[i + 2];
//             int next4 = correctPath.length > i + 4 ? correctPath[i + 4] : -1;

//             if (next == 10 && color[prev].equals("Aqua") && color[next].equals("Purple")) {
//                 message.append("\nChange route: Board metro towards Katraj (Purple Line)\n");
//             } else if (next2 == 8 && color[prev].equals("Aqua") && color[next2].equals("Purple")) {
//                 message.append("\nChange route: Board metro towards PCMC (Purple Line)\n");
//             } else if (color[prev].equals("Purple") && next == 28 && color[next].equals("Aqua")) {
//                 message.append("\nChange route: Board metro towards Chandani Chowk (Aqua Line)\n");
//             } else if (color[prev].equals("Purple") && next == 30 && color[next].equals("Aqua")) {
//                 message.append("\nChange route: Board metro towards Wagholi (Aqua Line)\n");
//             }
//         }

//         return message.toString();
//     }

//     // All your existing helper methods remain the same (getStationIndex, station, giveColorToStation, etc.)
//     // ...
//     public static String getStationName(int t) {
//         return switch (t) {
//             //Purple Line Starts
//             case 0 -> "PCMC";
//             case 1 -> "Sant Tukaram Nagar";
//             case 2 -> "Bhosari/Nashik Phata";
//             case 3 -> "Kasarwadi";
//             case 4 -> "Phugewadi";
//             case 5 -> "Dapodi";
//             case 6 -> "Bopodi";
//             case 7 -> "Khadaki";
//             case 8 -> "Range Hill";
//             case 9 -> "ShivajiNagar";
//             case 10 -> "Budhwar Peth";
//             case 11 -> "Mandai";
//             case 12 -> "Swargate";
//             case 13 -> "Laxmi Narayan Talkies";
//             case 14 -> "Bhapkar Petrol Pump";
//             case 15 -> "Padmavati";
//             case 16 -> "Balaji Nagar";
//             case 17 -> "Bharti Vidyapeeth gate";
//             case 18 -> "Sarpodyan";
//             case 19 -> "Katraj";
//             //Purple Line Ends
//             //Aqua Begins
//             case 20 -> "Chandani Chowk";
//             case 21 -> "Vanaz";
//             case 22 -> "Anand Nagar";
//             case 23 -> "Ideal colony";
//             case 24 -> "Nal Stop";
//             case 25 -> "Garware College";
//             case 26 -> "Deccan Gymkhana";
//             case 27 -> "Chhatrapati Sambhaji Udyan";
//             case 28 -> "PMC";
//             case 29 -> "Civil Court";
//             case 30 -> "Mangalwar Peth";
//             case 31 -> "Pune Railway Station";
//             case 32 -> "Ruby Hall Clinic";
//             case 33 -> "Bund Garden";
//             case 34 -> "Yerawada";
//             case 35 -> "Kalayani Nagar";
//             case 36 -> "Ramwadi";
//             case 37 -> "Wadgaon Sheri Phata";
//             case 38 -> "Viman Nagar Corner";
//             case 39 -> "Tata Guardroom";
//             case 40 -> "Kharadi Bypass";
//             case 41 -> "Janak Baba Dargha";
//             case 42 -> "Aaple Ghar";
//             case 43 -> "Khandve Nagar";
//             case 44 -> "Ubale Nagar";
//             case 45 -> "Godown Talera";
//             case 46 -> "Soyrik Mangal Karyalay";
//             case 47 -> "Kamal Bagh";
//             case 48 -> "Wagholi";
//             //Aqua Ends
//             default -> "Unknown Station";
//         };
//     }

//     public static void dijkstra(int sourceNumber, int destinationNumber) {
//         boolean[] visited = new boolean[49]; // Only consider stations 0 to 48
//         for (int i = 0; i < 49; i++) {
//             predecessor[i] = 0;
//             dist[i] = Float.MAX_VALUE;
//             visited[i] = false;
//         }

//         dist[sourceNumber] = 0;

//         for (int j = 0; j < 49; j++) {
//             int minNode = minDistanceNode(dist, visited);
//             if (minNode == -1)
//                 break; // No reachable node

//             visited[minNode] = true;

//             for (int k = 0; k < 49; k++) {
//                 if (!visited[k] && adj[minNode][k] != 0 && dist[minNode] != Float.MAX_VALUE
//                         && dist[minNode] + adj[minNode][k] < dist[k]) {
//                     predecessor[k] = minNode;
//                     dist[k] = dist[minNode] + adj[minNode][k];
//                 }
//             }
//         }
//     }

//     public static int minDistanceNode(float[] dist, boolean[] visited) {
//         float min = Float.MAX_VALUE;
//         int minNode = -1;

//         for (int i = 0; i < 49; i++) {
//             if (!visited[i] && dist[i] < min) {
//                 min = dist[i];
//                 minNode = i;
//             }
//         }
//         return minNode;
//     }

//     public static int station(String name) {
//         name = name.trim().toLowerCase();

//         // Purple line
//         if (name.equals("pcmc"))
//             return 0;
//         if (name.equals("sant tukaram nagar"))
//             return 1;
//         if (name.equals("bhosari") || name.equals("nashik phata"))
//             return 2;
//         if (name.equals("kasarwadi"))
//             return 3;
//         if (name.equals("phugewadi"))
//             return 4;
//         if (name.equals("dapodi"))
//             return 5;
//         if (name.equals("bopodi"))
//             return 6;
//         if (name.equals("khadaki"))
//             return 7;
//         if (name.equals("range hill"))
//             return 8;
//         if (name.equals("shivaji nagar"))
//             return 9;
//         if (name.equals("budhwar peth"))
//             return 10;
//         if (name.equals("mandai"))
//             return 11;
//         if (name.equals("swargate"))
//             return 12;
//         if (name.equals("laxmi narayan talkies"))
//             return 13;
//         if (name.equals("bhapkar petrol pump"))
//             return 14;
//         if (name.equals("padmavati"))
//             return 15;
//         if (name.equals("balaji nagar"))
//             return 16;
//         if (name.equals("bharti vidyapeeth gate"))
//             return 17;
//         if (name.equals("sarpodyan"))
//             return 18;
//         if (name.equals("katraj"))
//             return 19;

//         // Aqua line
//         if (name.equals("chandani chowk"))
//             return 20;
//         if (name.equals("vanaz"))
//             return 21;
//         if (name.equals("anand nagar"))
//             return 22;
//         if (name.equals("ideal colony"))
//             return 23;
//         if (name.equals("nal stop"))
//             return 24;
//         if (name.equals("garware college"))
//             return 25;
//         if (name.equals("deccan gymkhana"))
//             return 26;
//         if (name.equals("chhatrapati sambhaji udyan"))
//             return 27;
//         if (name.equals("pmc"))
//             return 28;
//         if (name.equals("civil court"))
//             return 29;
//         if (name.equals("mangalwar peth"))
//             return 30;
//         if (name.equals("pune railway station"))
//             return 31;
//         if (name.equals("ruby hall clinic"))
//             return 32;
//         if (name.equals("bund garden"))
//             return 33;
//         if (name.equals("yerawada"))
//             return 34;
//         if (name.equals("kalayani nagar"))
//             return 35;
//         if (name.equals("ramwadi"))
//             return 36;
//         if (name.equals("wadgaon sheri phata"))
//             return 37;
//         if (name.equals("viman nagar corner"))
//             return 38;
//         if (name.equals("tata guardroom"))
//             return 39;
//         if (name.equals("kharadi bypass"))
//             return 40;
//         if (name.equals("janak baba dargha"))
//             return 41;
//         if (name.equals("aaple ghar"))
//             return 42;
//         if (name.equals("khandve nagar"))
//             return 43;
//         if (name.equals("ubale nagar"))
//             return 44;
//         if (name.equals("godown talera"))
//             return 45;
//         if (name.equals("soyrik mangal karyalay"))
//             return 46;
//         if (name.equals("kamal bagh"))
//             return 47;
//         if (name.equals("wagholi"))
//             return 48;

//         // If no match
//         return -1;
//     }

//     public static void stationNotFound(String name) {
//         System.out.println("Did you mean:-");

//         if (checkStation(name, "PCMC")) {
//             System.out.println("PCMC  (search key)->0");
//             push(0);
//         }

//         if (checkStation(name, "Sant Tukaram Nagar")) {
//             System.out.println("Sant Tukaram Nagar (search key)->1");
//             push(1);
//         }

//         if (checkStation(name, "Bhosari") || checkStation(name, "Nashik Phata")) {
//             System.out.println("Bhosari/Nashik Phata (search key)->2");
//             push(2);
//         }

//         if (checkStation(name, "Kasarwadi")) {
//             System.out.println("Kasarwadi  (search key)->3");
//             push(3);
//         }

//         if (checkStation(name, "Phugewadi")) {
//             System.out.println("Phugewadi  (search key)->4");
//             push(4);
//         }

//         if (checkStation(name, "Dapodi")) {
//             System.out.println("Dapodi (search key)->5");
//             push(5);
//         }

//         if (checkStation(name, "Bopodi")) {
//             System.out.println("Bopodi  (search key)->6");
//             push(6);
//         }

//         if (checkStation(name, "Khadaki")) {
//             System.out.println("Khadaki  (search key)->7");
//             push(7);
//         }

//         if (checkStation(name, "Range Hill")) {
//             System.out.println("Range Hill  (search key)->8");
//             push(8);
//         }

//         if (checkStation(name, "Shivaji Nagar")) {
//             System.out.println("Shivaji Nagar  (search key)->9");
//             push(9);
//         }

//         if (checkStation(name, "Budhwar Peth")) {
//             System.out.println("Budhwar Peth  (search key)->10");
//             push(10);
//         }

//         if (checkStation(name, "Mandai")) {
//             System.out.println("Mandai (search key)->11");
//             push(11);
//         }

//         if (checkStation(name, "Swargate")) {
//             System.out.println("Swargate  (search key)->12");
//             push(12);
//         }

//         if (checkStation(name, "Laxmi Narayan Talkies")) {
//             System.out.println("Laxmi Narayan Talkies  (search key)->13");
//             push(13);
//         }

//         if (checkStation(name, "Bhapkar Petrol Pump")) {
//             System.out.println("Bhapkar Petrol Pump  (search key)->14");
//             push(14);
//         }

//         if (checkStation(name, "Padmavati")) {
//             System.out.println("Padmavati  (search key)->15");
//             push(15);
//         }

//         if (checkStation(name, "Balaji Nagar")) {
//             System.out.println("Balaji Nagar  (search key)->16");
//             push(16);
//         }

//         if (checkStation(name, "Bharti Vidyapeeth Gate")) {
//             System.out.println("Bharti Vidyapeeth Gate  (search key)->17");
//             push(17);
//         }

//         if (checkStation(name, "Sarpodyan")) {
//             System.out.println("Sarpodyan  (search key)->18");
//             push(18);
//         }

//         if (checkStation(name, "Katraj")) {
//             System.out.println("Katraj  (search key)->19");
//             push(19);
//         }

//         // Aqua Line

//         if (checkStation(name, "Chandani Chowk")) {
//             System.out.println("Chandani Chowk (search key)->20");
//             push(20);
//         }

//         if (checkStation(name, "Vanaz")) {
//             System.out.println("Vanaz (search key)->21");
//             push(21);
//         }

//         if (checkStation(name, "Anand Nagar")) {
//             System.out.println("Anand Nagar  (search key)->22");
//             push(22);
//         }

//         if (checkStation(name, "Ideal colony")) {
//             System.out.println("Ideal colony  (search key)->23");
//             push(23);
//         }

//         if (checkStation(name, "Nal Stop")) {
//             System.out.println("Nal Stop  (search key)->24");
//             push(24);
//         }

//         if (checkStation(name, "Garware College")) {
//             System.out.println("Garware College  (search key)->25");
//             push(25);
//         }

//         if (checkStation(name, "Deccan Gymkhana")) {
//             System.out.println("Deccan Gymkhana  (search key)->26");
//             push(26);
//         }

//         if (checkStation(name, "Chhatrapati Sambhaji Udyan")) {
//             System.out.println("Chhatrapati Sambhaji Udyan (search key)->27");
//             push(27);
//         }

//         if (checkStation(name, "PMC")) {
//             System.out.println("PMC (search key)->28");
//             push(28);
//         }

//         if (checkStation(name, "Civil Court")) {
//             System.out.println("Civil Court  (search key)->29");
//             push(29);
//         }

//         if (checkStation(name, "Mangalwar Peth")) {
//             System.out.println("Mangalwar Peth  (search key)->30");
//             push(30);
//         }

//         if (checkStation(name, "Pune Railway Station")) {
//             System.out.println("Pune Railway Station  (search key)->31");
//             push(31);
//         }

//         if (checkStation(name, "Ruby Hall Clinic")) {
//             System.out.println("Ruby Hall Clinic  (search key)->32");
//             push(32);
//         }

//         if (checkStation(name, "Bund Garden")) {
//             System.out.println("Bund Garden  (search key)->33");
//             push(33);
//         }

//         if (checkStation(name, "Yerawada")) {
//             System.out.println("Yerawada  (search key)->34");
//             push(34);
//         }

//         if (checkStation(name, "Kalayani Nagar")) {
//             System.out.println("Kalayani Nagar  (search key)->35");
//             push(35);
//         }

//         if (checkStation(name, "Ramwadi")) {
//             System.out.println("Ramwadi (search key)->36");
//             push(36);
//         }

//         if (checkStation(name, "Wadgaon Sheri Phata")) {
//             System.out.println("Wadgaon Sheri Phata (search key)->37");
//             push(37);
//         }

//         if (checkStation(name, "Viman Nagar Corner")) {
//             System.out.println("Viman Nagar Corner  (search key)->38");
//             push(38);
//         }

//         if (checkStation(name, "Tata Guardroom")) {
//             System.out.println("Tata Guardroom  (search key)->39");
//             push(39);
//         }

//         if (checkStation(name, "Kharadi Bypass")) {
//             System.out.println("Kharadi Bypass (search key)->40");
//             push(40);
//         }

//         if (checkStation(name, "Janak Baba Dargha")) {
//             System.out.println("Janak Baba Dargha  (search key)->41");
//             push(41);
//         }

//         if (checkStation(name, "Aaple Ghar")) {
//             System.out.println("Aaple Ghar (search key)->42");
//             push(42);
//         }

//         if (checkStation(name, "Khandve Nagar")) {
//             System.out.println("Khandve Nagar  (search key)->43");
//             push(43);
//         }

//         if (checkStation(name, "Ubale Nagar")) {
//             System.out.println("Ubale Nagar (search key)->44");
//             push(44);
//         }

//         if (checkStation(name, "Godown Talera")) {
//             System.out.println("Godown Talera (search key)->45");
//             push(45);
//         }

//         if (checkStation(name, "Soyrik Mangal Karyalay")) {
//             System.out.println("Soyrik Mangal Karyalay  (search key)->46");
//             push(46);
//         }

//         if (checkStation(name, "Kamal Bagh")) {
//             System.out.println("Kamal Bagh  (search key)->47");
//             push(47);
//         }

//         if (checkStation(name, "Wagholi")) {
//             System.out.println("Wagholi  (search key)->48");
//             push(48);
//         }
//     }

//     static void createGraph() {
//         // Create a 2D adjacency matrix of appropriate size (e.g., 49x49)
//         adj[0][1] = adj[1][0] = 2.1f;
//         adj[1][2] = adj[2][1] = 0.7f;
//         adj[2][3] = adj[3][2] = 1.5f;
//         adj[3][4] = adj[4][3] = 1.1f;
//         adj[4][5] = adj[5][4] = 2.4f;
//         adj[5][6] = adj[6][5] = 1.0f;
//         adj[6][7] = adj[7][6] = 1.4f;
//         adj[7][8] = adj[8][7] = 5.3f;
//         adj[8][9] = adj[9][8] = 2.9f;
//         adj[9][29] = adj[29][9] = 1.0f;
//         adj[29][10] = adj[10][29] = 1.1f;
//         adj[10][11] = adj[11][10] = 0.65f;
//         adj[11][12] = adj[12][11] = 1.7f;
//         adj[12][13] = adj[13][12] = 0.4f;
//         adj[13][14] = adj[14][13] = 1.3f;
//         adj[14][15] = adj[15][14] = 1.6f;
//         adj[15][16] = adj[16][15] = 1.6f;
//         adj[16][17] = adj[17][16] = 0.8f;
//         adj[17][18] = adj[18][17] = 1.1f;
//         adj[18][19] = adj[19][18] = 0.9f;

//         // Aqua Line
//         adj[20][21] = adj[21][20] = 1.15f;
//         adj[21][22] = adj[22][21] = 1.8f;
//         adj[22][23] = adj[23][22] = 1.7f;
//         adj[23][24] = adj[24][23] = 1.5f;
//         adj[24][25] = adj[25][24] = 1.8f;
//         adj[25][26] = adj[26][25] = 1.0f;
//         adj[26][27] = adj[27][26] = 0.8f;
//         adj[27][28] = adj[28][27] = 0.8f;
//         adj[28][29] = adj[29][28] = 0.8f;
//         adj[29][30] = adj[30][29] = 1.5f;
//         adj[30][31] = adj[31][30] = 1.3f;
//         adj[31][32] = adj[32][31] = 0.9f;
//         adj[32][33] = adj[33][32] = 1.3f;
//         adj[33][34] = adj[34][33] = 1.0f;
//         adj[34][35] = adj[35][34] = 2.5f;
//         adj[35][36] = adj[36][35] = 3.0f;
//         adj[36][37] = adj[37][36] = 1.2f;
//         adj[37][38] = adj[38][37] = 1.4f;
//         adj[38][39] = adj[39][38] = 1.4f;
//         adj[39][40] = adj[40][39] = 1.2f;
//         adj[40][41] = adj[41][40] = 0.75f;
//         adj[41][42] = adj[42][41] = 2.0f;
//         adj[42][43] = adj[43][42] = 2.4f;
//         adj[43][44] = adj[44][43] = 1.3f;
//         adj[44][45] = adj[45][44] = 1.8f;
//         adj[45][46] = adj[46][45] = 0.75f;
//         adj[46][47] = adj[47][46] = 1.4f;
//         adj[47][48] = adj[48][47] = 4.0f;
//     }

// }


//New Addition in Code
//Adding time clocks in it
//Code 2
// 30/07/2025

// import javafx.application.Application;
// import javafx.collections.ObservableList;
// import javafx.geometry.Insets;
// import javafx.geometry.Pos;
// import javafx.scene.Scene;
// import javafx.scene.control.*;
// import javafx.scene.layout.*;
// import javafx.scene.paint.Color;
// import javafx.scene.text.Font;
// import javafx.scene.text.FontWeight;
// import javafx.stage.Stage;
// import java.time.LocalTime;
// import java.util.*;
// import java.util.stream.Collectors;

// public class Main extends Application {
//     // Your existing static variables and methods remain the same
//     static final int V = 91;
//     static final int MAX = 91;
//     static int[] predecessor = new int[100];
//     static int[] reversePath = new int[100];
//     static int[] correctPath = new int[100];
//     static String[] color = new String[100];
//     static Stack<Integer> stack = new Stack<>();
//     static float[][] adj = new float[MAX][MAX];
//     static float[] dist = new float[MAX];

//     // New GUI components
//     private ComboBox<String> sourceComboBox;
//     private ComboBox<String> destinationComboBox;
//     private TextArea resultArea;
//     private List<String> stationNames;
//     private VBox routeDisplayBox;

//     public static void main(String[] args) {
//         launch(args);
//     }

//     @Override
//     public void start(Stage primaryStage) {
//         // Initialize data
//         giveColorToStation();
//         createGraph();
//         initializeStationNames();

//         // ===== Root Layout =====
//         routeDisplayBox = new VBox();
//         routeDisplayBox.setSpacing(10);
//         routeDisplayBox.setPadding(new Insets(20));

//         // ===== Header =====
//         Label header = new Label("PUNE METRO CONNECT");
//         header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
//         header.setTextFill(Color.WHITE);

//         // Theme toggle switch
//         ToggleButton themeToggle = new ToggleButton("🌞");
//         themeToggle.setFont(Font.font(18));

//         // Header container (Header + Toggle)
//         HBox headerBox = new HBox(20, header, themeToggle);
//         headerBox.setAlignment(Pos.CENTER_LEFT);
//         HBox.setHgrow(themeToggle, Priority.ALWAYS);
//         headerBox.setSpacing(10);

//         // ===== Input Grid =====
//         GridPane inputGrid = new GridPane();
//         inputGrid.setHgap(10);
//         inputGrid.setVgap(10);
//         inputGrid.setPadding(new Insets(10));

//         // Source
//         Label sourceLabel = new Label("Source Station:");
//         sourceLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
//         sourceComboBox = new ComboBox<>();
//         sourceComboBox.setPromptText("Select or type station name");
//         sourceComboBox.setEditable(true);
//         sourceComboBox.getItems().addAll(stationNames);

//         // Destination
//         Label destLabel = new Label("Destination Station:");
//         destLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
//         destinationComboBox = new ComboBox<>();
//         destinationComboBox.setPromptText("Select or type station name");
//         destinationComboBox.setEditable(true);
//         destinationComboBox.getItems().addAll(stationNames);

//         // Find Route Button
//         Button findRouteBtn = new Button("Find Route");
//         findRouteBtn.setPrefWidth(150);
//         findRouteBtn.setOnAction(e -> findRoute());

//         // Add to Grid
//         inputGrid.add(sourceLabel, 0, 0);
//         inputGrid.add(sourceComboBox, 1, 0);
//         inputGrid.add(destLabel, 0, 1);
//         inputGrid.add(destinationComboBox, 1, 1);
//         inputGrid.add(findRouteBtn, 1, 2);

//         // ===== Result Text Area =====
//         resultArea = new TextArea();
//         resultArea.setEditable(false);
//         resultArea.setWrapText(true);
//         resultArea.setPrefHeight(400);

//         // Add all UI elements to root
//         routeDisplayBox.getChildren().addAll(headerBox, inputGrid, resultArea);

//         // ===== Scene Setup =====
//         Scene scene = new Scene(routeDisplayBox, 700, 600);
//         scene.getStylesheets().add(getClass().getResource("dark.css").toExternalForm());

//         // Toggle Theme Logic
//         themeToggle.setOnAction(e -> {
//             scene.getStylesheets().clear();
//             if (themeToggle.isSelected()) {
//                 scene.getStylesheets().add(getClass().getResource("light.css").toExternalForm());
//                 themeToggle.setText("🌜");
//             } else {
//                 scene.getStylesheets().add(getClass().getResource("dark.css").toExternalForm());
//                 themeToggle.setText("🌞");
//             }
//         });

//         primaryStage.setTitle("Pune Metro Route Planner");
//         primaryStage.setScene(scene);
//         primaryStage.show();
//     }

//     // Helper to find the route as a list of station names
//     private List<String> findRouteBetweenStations(String source, String destination) {
//         int sourceIdx = getStationIndex(source);
//         int destIdx = getStationIndex(destination);
//         if (sourceIdx < 0 || destIdx < 0)
//             return Collections.emptyList();

//         dijkstra(sourceIdx, destIdx);

//         List<String> path = new ArrayList<>();
//         int current = destIdx;
//         while (current != sourceIdx) {
//             path.add(getStationName(current));
//             if (predecessor[current] == 0 && current != sourceIdx) {
//                 // No path found
//                 return Collections.emptyList();
//             }
//             current = predecessor[current];
//         }
//         path.add(getStationName(sourceIdx));
//         Collections.reverse(path);
//         return path;
//     }

//     private void initializeStationNames() {
//         stationNames = new ArrayList<>();
//         for (int i = 0; i <= 48; i++) {
//             stationNames.add(getStationName(i).replace("", "").trim());
//         }
//         Collections.sort(stationNames);
//     }

//     // Helper method to check if the station name is similar to the target (case-insensitive, partial match)
//     public static boolean checkStation(String input, String target) {
//         if (input == null || target == null)
//             return false;
//         input = input.trim().toLowerCase();
//         target = target.trim().toLowerCase();
//         return input.contains(target) || target.contains(input);
//     }

//     public static int getStationIndex(String name) {
//         int idx = station(name);
//         if (idx != -1) {
//             return idx;
//         } else {
//             return -50; // mimic the original error handling
//         }
//     }

//     public static void giveColorToStation() {
//         for (int i = 0; i <= 19; i++) {
//             color[i] = "Purple";
//         }

//         for (int i = 20; i <= 48; i++) {
//             color[i] = "Aqua";
//         }

//     }

//     public static void push(int data) {
//         if (stack.size() >= 100) {
//             System.out.println("Stack Overflow");
//             return;
//         }
//         stack.push(data);
//     }

//     private static int getArrivalMinute(int src, boolean forward) {
//         // Applies only for Purple (0–19) and Aqua (20–48)
//         if (src > 48)
//             return 0;

//         if (forward) {
//             if (src == 0 || src == 20)
//                 return 0;
//             if ((src >= 1 && src <= 4) || (src >= 21 && src <= 25))
//                 return 5;
//             if ((src >= 5 && src <= 8) || (src >= 26 && src <= 31))
//                 return 10;
//             if ((src >= 9 && src <= 11) || (src >= 32 && src <= 37))
//                 return 15;
//             if ((src >= 12 && src <= 15) || (src >= 38 && src <= 42))
//                 return 20;
//             if ((src >= 16 && src <= 18) || (src >= 43 && src <= 47))
//                 return 25;
//         } else {
//             if (src == 19 || src == 48)
//                 return 30;
//             if ((src >= 1 && src <= 4) || (src >= 21 && src <= 25))
//                 return 25;
//             if ((src >= 5 && src <= 8) || (src >= 26 && src <= 31))
//                 return 25;
//             if ((src >= 9 && src <= 11) || (src >= 32 && src <= 37))
//                 return 15;
//             if ((src >= 12 && src <= 15) || (src >= 38 && src <= 42))
//                 return 10;
//             if ((src >= 16 && src <= 18) || (src >= 43 && src <= 47))
//                 return 5;
//         }

//         return 0; // fallback
//     }

// private void findRoute() {
//     String sourceName = sourceComboBox.getValue();
//     String destinationName = destinationComboBox.getValue();

//     if (sourceName == null || destinationName == null) {
//         showAlert("Please select both source and destination stations");
//         return;
//     }

//     if (sourceName.equals(destinationName)) {
//         showAlert("Source and destination stations cannot be the same");
//         return;
//     }

//     int sourceNumber = getStationIndex(sourceName);
//     int destinationNumber = getStationIndex(destinationName);

//     if (sourceNumber == -50 || destinationNumber == -50) {
//         showAlert("One or more stations not found");
//         return;
//     }

//     String sourceColor = color[sourceNumber];

//     dijkstra(sourceNumber, destinationNumber);
//     float weight = dist[destinationNumber];

//     StringBuilder result = new StringBuilder();
//     result.append("🚇 Pune Metro Route Details\n\n");
//     result.append("From: ").append(sourceName).append("\n");
//     result.append("To: ").append(destinationName).append("\n\n");

//     findPath(sourceNumber, destinationNumber, sourceColor, result);
//     result.append(String.format("\nDistance to travel: %.2f Km\n", weight));

//     // 🔧 Path calculation for time & interchange
//     List<Integer> route = getPathAsList(sourceNumber, destinationNumber);
//     int numberOfHops = route.size() - 1;
//     int estimatedTime = numberOfHops * 2; // 2 mins per edge
//     int interchanges = 0;

//     for (int i = 1; i < route.size() - 1; i++) {
//         int prev = route.get(i - 1);
//         int curr = route.get(i);
//         int next = route.get(i + 1);

//         if (stationNames.get(curr).equalsIgnoreCase("Civil Court")) {
//             String prevColor = color[prev];
//             String nextColor = color[next];
//             if (!prevColor.equals(nextColor)) {
//                 interchanges++;
//                 estimatedTime += 5; // extra time for interchange
//             }
//         }
//     }

//     // 🕒 Convert to hours/minutes
//     if (estimatedTime >= 60) {
//         int hours = estimatedTime / 60;
//         int minutes = estimatedTime % 60;
//         result.append(String.format("\n🕒 Estimated Time: %d hour%s %d minute%s",
//                 hours, (hours > 1 ? "s" : ""),
//                 minutes, (minutes != 1 ? "s" : "")));
//     } else {
//         result.append("\n🕒 Estimated Time: ").append(estimatedTime).append(" minute").append(estimatedTime != 1 ? "s" : "");
//     }

//     // Add interchange note
//     if (interchanges > 0) {
//         result.append(" (Includes ").append(interchanges).append(" interchange");
//         result.append(interchanges > 1 ? "s" : "").append(" at Civil Court)");
//     }

//     resultArea.setText(result.toString());
// }
// private List<Integer> getPathAsList(int source, int dest) {
//     List<Integer> path = new ArrayList<>();
//     int current = dest;

//     while (current != -1) {
//         path.add(0, current); // prepend to path
//         if (current == source) break;
//         current = predecessor[current];
//     }

//     return path;
// }

//     private void showAlert(String message) {
//         Alert alert = new Alert(Alert.AlertType.WARNING);
//         alert.setTitle("Input Error");
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }

//     // Modified findPath to use StringBuilder instead of System.out
//     static void findPath(int sourceNumber, int destinationNumber, String sourceColor, StringBuilder result) {
//         int count = 0;
//         while (destinationNumber != sourceNumber) {
//             reversePath[count] = destinationNumber;
//             int u = predecessor[destinationNumber];
//             destinationNumber = u;
//             count++;
//         }

//         reversePath[count++] = sourceNumber;

//         for (int j = 0, i = count; j <= count && i >= 0; j++, i--) {
//             correctPath[j] = reversePath[i];
//         }

//         int currentSource = correctPath[1];
//         int nextToCurrentSource = correctPath[2];

//         // Display time information
//         LocalTime time = LocalTime.now();
//         int hour = time.getHour();
//         int minute = time.getMinute();

//         if (hour >= 8 && hour <= 22) {
//             int arrivalMinute = getArrivalMinute(sourceNumber, (currentSource - nextToCurrentSource == -1 ||
//                     currentSource - nextToCurrentSource == 20 || currentSource - nextToCurrentSource == 19));

//             if (minute <= arrivalMinute) {
//                 result.append(String.format("Next metro arrives at: %02d:%02d%n", hour, arrivalMinute));
//             } else {
//                 result.append(String.format("Next metro arrives at: %02d:%02d%n", (hour + 1) % 24, arrivalMinute));
//             }
//         } else {
//             result.append("Metro is not available now (operates 8AM-10PM)\n");
//         }

//         // Direction information
//         if (currentSource - nextToCurrentSource == -1 || currentSource - nextToCurrentSource == 20
//                 || currentSource - nextToCurrentSource == 19) {
//             result.append(getDirectionMessage(sourceNumber, currentSource, nextToCurrentSource, true));
//         } else if (currentSource - nextToCurrentSource == 1 || currentSource - nextToCurrentSource == -20) {
//             result.append(getDirectionMessage(sourceNumber, currentSource, nextToCurrentSource, false));
//         }

//         result.append("\n📍 Your Journey:\n\n");

//         StringBuilder pathVisual = new StringBuilder();
//         for (int i = 1; i <= count; i++) {
//             if (correctPath[i] == 9 || correctPath[i] == 12 || correctPath[i] == 29) {
//                 result.append(solveConflict(i));
//             }
//             pathVisual.append(getStationName(correctPath[i]));
//             if (i < count) {
//                 pathVisual.append(" → ");
//             }
//         }
//         result.append(pathVisual).append("\n");
//     }

//     private static String getDirectionMessage(int sourceNumber, int currentSource, int nextToCurrentSource,boolean right) {
//         if (right) {
//             if (currentSource - nextToCurrentSource == 20) {
//                 return "Board metro heading towards PCMC\n";
//             } else if (currentSource - nextToCurrentSource == 19) {
//                 return "Board metro heading towards Katraj\n";
//             } else if (color[sourceNumber].equals("Purple")) {
//                 return "Board metro heading towards Katraj\n";
//             } else {
//                 return "Board metro heading towards Wagholi\n";
//             }
//         } else {
//             if (currentSource - nextToCurrentSource == -20) {
//                 return "Board metro heading towards Katraj\n";
//             } else if (color[sourceNumber].equals("Purple")) {
//                 return "Board metro heading towards PCMC\n";
//             } else {
//                 return "Board metro heading towards Chandani Chowk\n";
//             }
//         }
//     }

//     // Modified solveConflict to return String instead of printing
//     static String solveConflict(int i) {
//         int current = correctPath[i];
//         int prev = correctPath[i - 1];
//         int next = correctPath[i + 1];
//         StringBuilder message = new StringBuilder();

//         // Junction: Shivaji Nagar
//         if (current == 9) {
//             if (!color[prev].equals(color[next])) {
//                 if (color[prev].equals("Purple") && color[next].equals("Aqua") && next == 28) {
//                     message.append("\nChange route: Board metro towards Chandani Chowk\n");
//                 }
//             }
//         }
//         // Junction: Civil Court
//         else if (current == 29) {
//             int next2 = correctPath[i + 2];
//             int next4 = correctPath.length > i + 4 ? correctPath[i + 4] : -1;

//             if (next == 10 && color[prev].equals("Aqua") && color[next].equals("Purple")) {
//                 message.append("\nChange route: Board metro towards Katraj (Purple Line)\n");
//             } else if (next2 == 8 && color[prev].equals("Aqua") && color[next2].equals("Purple")) {
//                 message.append("\nChange route: Board metro towards PCMC (Purple Line)\n");
//             } else if (color[prev].equals("Purple") && next == 28 && color[next].equals("Aqua")) {
//                 message.append("\nChange route: Board metro towards Chandani Chowk (Aqua Line)\n");
//             } else if (color[prev].equals("Purple") && next == 30 && color[next].equals("Aqua")) {
//                 message.append("\nChange route: Board metro towards Wagholi (Aqua Line)\n");
//             }
//         }

//         return message.toString();
//     }

//     // All your existing helper methods remain the same (getStationIndex, station, giveColorToStation, etc.)
//     // ...
//     public static String getStationName(int t) {
//         return switch (t) {
//             //Purple Line Starts
//             case 0 -> "PCMC";
//             case 1 -> "Sant Tukaram Nagar";
//             case 2 -> "Bhosari/Nashik Phata";
//             case 3 -> "Kasarwadi";
//             case 4 -> "Phugewadi";
//             case 5 -> "Dapodi";
//             case 6 -> "Bopodi";
//             case 7 -> "Khadaki";
//             case 8 -> "Range Hill";
//             case 9 -> "ShivajiNagar";
//             case 10 -> "Budhwar Peth";
//             case 11 -> "Mandai";
//             case 12 -> "Swargate";
//             case 13 -> "Laxmi Narayan Talkies";
//             case 14 -> "Bhapkar Petrol Pump";
//             case 15 -> "Padmavati";
//             case 16 -> "Balaji Nagar";
//             case 17 -> "Bharti Vidyapeeth gate";
//             case 18 -> "Sarpodyan";
//             case 19 -> "Katraj";
//             //Purple Line Ends

//             //Aqua Begins
//             case 20 -> "Chandani Chowk";
//             case 21 -> "Vanaz";
//             case 22 -> "Anand Nagar";
//             case 23 -> "Ideal colony";
//             case 24 -> "Nal Stop";
//             case 25 -> "Garware College";
//             case 26 -> "Deccan Gymkhana";
//             case 27 -> "Chhatrapati Sambhaji Udyan";
//             case 28 -> "PMC";
//             case 29 -> "Civil Court";
//             case 30 -> "Mangalwar Peth";
//             case 31 -> "Pune Railway Station";
//             case 32 -> "Ruby Hall Clinic";
//             case 33 -> "Bund Garden";
//             case 34 -> "Yerawada";
//             case 35 -> "Kalayani Nagar";
//             case 36 -> "Ramwadi";
//             case 37 -> "Wadgaon Sheri Phata";
//             case 38 -> "Viman Nagar Corner";
//             case 39 -> "Tata Guardroom";
//             case 40 -> "Kharadi Bypass";
//             case 41 -> "Janak Baba Dargha";
//             case 42 -> "Aaple Ghar";
//             case 43 -> "Khandve Nagar";
//             case 44 -> "Ubale Nagar";
//             case 45 -> "Godown Talera";
//             case 46 -> "Soyrik Mangal Karyalay";
//             case 47 -> "Kamal Bagh";
//             case 48 -> "Wagholi";
//             //Aqua Ends
//             default -> "Unknown Station";
//         };
//     }

//     public static void dijkstra(int sourceNumber, int destinationNumber) {
//         boolean[] visited = new boolean[49]; // Only consider stations 0 to 48
//         for (int i = 0; i < 49; i++) {
//             predecessor[i] = 0;
//             dist[i] = Float.MAX_VALUE;
//             visited[i] = false;
//         }

//         dist[sourceNumber] = 0;

//         for (int j = 0; j < 49; j++) {
//             int minNode = minDistanceNode(dist, visited);
//             if (minNode == -1)
//                 break; // No reachable node

//             visited[minNode] = true;

//             for (int k = 0; k < 49; k++) {
//                 if (!visited[k] && adj[minNode][k] != 0 && dist[minNode] != Float.MAX_VALUE
//                         && dist[minNode] + adj[minNode][k] < dist[k]) {
//                     predecessor[k] = minNode;
//                     dist[k] = dist[minNode] + adj[minNode][k];
//                 }
//             }
//         }
//     }

//     public static int minDistanceNode(float[] dist, boolean[] visited) {
//         float min = Float.MAX_VALUE;
//         int minNode = -1;

//         for (int i = 0; i < 49; i++) {
//             if (!visited[i] && dist[i] < min) {
//                 min = dist[i];
//                 minNode = i;
//             }
//         }
//         return minNode;
//     }

//     public static int station(String name) {
//         name = name.trim().toLowerCase();

//         // Purple line
//         if (name.equals("pcmc"))
//             return 0;
//         if (name.equals("sant tukaram nagar"))
//             return 1;
//         if (name.equals("bhosari") || name.equals("nashik phata"))
//             return 2;
//         if (name.equals("kasarwadi"))
//             return 3;
//         if (name.equals("phugewadi"))
//             return 4;
//         if (name.equals("dapodi"))
//             return 5;
//         if (name.equals("bopodi"))
//             return 6;
//         if (name.equals("khadaki"))
//             return 7;
//         if (name.equals("range hill"))
//             return 8;
//         if (name.equals("shivaji nagar"))
//             return 9;
//         if (name.equals("budhwar peth"))
//             return 10;
//         if (name.equals("mandai"))
//             return 11;
//         if (name.equals("swargate"))
//             return 12;
//         if (name.equals("laxmi narayan talkies"))
//             return 13;
//         if (name.equals("bhapkar petrol pump"))
//             return 14;
//         if (name.equals("padmavati"))
//             return 15;
//         if (name.equals("balaji nagar"))
//             return 16;
//         if (name.equals("bharti vidyapeeth gate"))
//             return 17;
//         if (name.equals("sarpodyan"))
//             return 18;
//         if (name.equals("katraj"))
//             return 19;

//         // Aqua line
//         if (name.equals("chandani chowk"))
//             return 20;
//         if (name.equals("vanaz"))
//             return 21;
//         if (name.equals("anand nagar"))
//             return 22;
//         if (name.equals("ideal colony"))
//             return 23;
//         if (name.equals("nal stop"))
//             return 24;
//         if (name.equals("garware college"))
//             return 25;
//         if (name.equals("deccan gymkhana"))
//             return 26;
//         if (name.equals("chhatrapati sambhaji udyan"))
//             return 27;
//         if (name.equals("pmc"))
//             return 28;
//         if (name.equals("civil court"))
//             return 29;
//         if (name.equals("mangalwar peth"))
//             return 30;
//         if (name.equals("pune railway station"))
//             return 31;
//         if (name.equals("ruby hall clinic"))
//             return 32;
//         if (name.equals("bund garden"))
//             return 33;
//         if (name.equals("yerawada"))
//             return 34;
//         if (name.equals("kalayani nagar"))
//             return 35;
//         if (name.equals("ramwadi"))
//             return 36;
//         if (name.equals("wadgaon sheri phata"))
//             return 37;
//         if (name.equals("viman nagar corner"))
//             return 38;
//         if (name.equals("tata guardroom"))
//             return 39;
//         if (name.equals("kharadi bypass"))
//             return 40;
//         if (name.equals("janak baba dargha"))
//             return 41;
//         if (name.equals("aaple ghar"))
//             return 42;
//         if (name.equals("khandve nagar"))
//             return 43;
//         if (name.equals("ubale nagar"))
//             return 44;
//         if (name.equals("godown talera"))
//             return 45;
//         if (name.equals("soyrik mangal karyalay"))
//             return 46;
//         if (name.equals("kamal bagh"))
//             return 47;
//         if (name.equals("wagholi"))
//             return 48;

//         // If no match
//         return -1;
//     }

//     public static void stationNotFound(String name) {
//         System.out.println("Did you mean:-");

//         if (checkStation(name, "PCMC")) {
//             System.out.println("PCMC  (search key)->0");
//             push(0);
//         }

//         if (checkStation(name, "Sant Tukaram Nagar")) {
//             System.out.println("Sant Tukaram Nagar (search key)->1");
//             push(1);
//         }

//         if (checkStation(name, "Bhosari") || checkStation(name, "Nashik Phata")) {
//             System.out.println("Bhosari/Nashik Phata (search key)->2");
//             push(2);
//         }

//         if (checkStation(name, "Kasarwadi")) {
//             System.out.println("Kasarwadi  (search key)->3");
//             push(3);
//         }

//         if (checkStation(name, "Phugewadi")) {
//             System.out.println("Phugewadi  (search key)->4");
//             push(4);
//         }

//         if (checkStation(name, "Dapodi")) {
//             System.out.println("Dapodi (search key)->5");
//             push(5);
//         }

//         if (checkStation(name, "Bopodi")) {
//             System.out.println("Bopodi  (search key)->6");
//             push(6);
//         }

//         if (checkStation(name, "Khadaki")) {
//             System.out.println("Khadaki  (search key)->7");
//             push(7);
//         }

//         if (checkStation(name, "Range Hill")) {
//             System.out.println("Range Hill  (search key)->8");
//             push(8);
//         }

//         if (checkStation(name, "Shivaji Nagar")) {
//             System.out.println("Shivaji Nagar  (search key)->9");
//             push(9);
//         }

//         if (checkStation(name, "Budhwar Peth")) {
//             System.out.println("Budhwar Peth  (search key)->10");
//             push(10);
//         }

//         if (checkStation(name, "Mandai")) {
//             System.out.println("Mandai (search key)->11");
//             push(11);
//         }

//         if (checkStation(name, "Swargate")) {
//             System.out.println("Swargate  (search key)->12");
//             push(12);
//         }

//         if (checkStation(name, "Laxmi Narayan Talkies")) {
//             System.out.println("Laxmi Narayan Talkies  (search key)->13");
//             push(13);
//         }

//         if (checkStation(name, "Bhapkar Petrol Pump")) {
//             System.out.println("Bhapkar Petrol Pump  (search key)->14");
//             push(14);
//         }

//         if (checkStation(name, "Padmavati")) {
//             System.out.println("Padmavati  (search key)->15");
//             push(15);
//         }

//         if (checkStation(name, "Balaji Nagar")) {
//             System.out.println("Balaji Nagar  (search key)->16");
//             push(16);
//         }

//         if (checkStation(name, "Bharti Vidyapeeth Gate")) {
//             System.out.println("Bharti Vidyapeeth Gate  (search key)->17");
//             push(17);
//         }

//         if (checkStation(name, "Sarpodyan")) {
//             System.out.println("Sarpodyan  (search key)->18");
//             push(18);
//         }

//         if (checkStation(name, "Katraj")) {
//             System.out.println("Katraj  (search key)->19");
//             push(19);
//         }

//         // Aqua Line

//         if (checkStation(name, "Chandani Chowk")) {
//             System.out.println("Chandani Chowk (search key)->20");
//             push(20);
//         }

//         if (checkStation(name, "Vanaz")) {
//             System.out.println("Vanaz (search key)->21");
//             push(21);
//         }

//         if (checkStation(name, "Anand Nagar")) {
//             System.out.println("Anand Nagar  (search key)->22");
//             push(22);
//         }

//         if (checkStation(name, "Ideal colony")) {
//             System.out.println("Ideal colony  (search key)->23");
//             push(23);
//         }

//         if (checkStation(name, "Nal Stop")) {
//             System.out.println("Nal Stop  (search key)->24");
//             push(24);
//         }

//         if (checkStation(name, "Garware College")) {
//             System.out.println("Garware College  (search key)->25");
//             push(25);
//         }

//         if (checkStation(name, "Deccan Gymkhana")) {
//             System.out.println("Deccan Gymkhana  (search key)->26");
//             push(26);
//         }

//         if (checkStation(name, "Chhatrapati Sambhaji Udyan")) {
//             System.out.println("Chhatrapati Sambhaji Udyan (search key)->27");
//             push(27);
//         }

//         if (checkStation(name, "PMC")) {
//             System.out.println("PMC (search key)->28");
//             push(28);
//         }

//         if (checkStation(name, "Civil Court")) {
//             System.out.println("Civil Court  (search key)->29");
//             push(29);
//         }

//         if (checkStation(name, "Mangalwar Peth")) {
//             System.out.println("Mangalwar Peth  (search key)->30");
//             push(30);
//         }

//         if (checkStation(name, "Pune Railway Station")) {
//             System.out.println("Pune Railway Station  (search key)->31");
//             push(31);
//         }

//         if (checkStation(name, "Ruby Hall Clinic")) {
//             System.out.println("Ruby Hall Clinic  (search key)->32");
//             push(32);
//         }

//         if (checkStation(name, "Bund Garden")) {
//             System.out.println("Bund Garden  (search key)->33");
//             push(33);
//         }

//         if (checkStation(name, "Yerawada")) {
//             System.out.println("Yerawada  (search key)->34");
//             push(34);
//         }

//         if (checkStation(name, "Kalayani Nagar")) {
//             System.out.println("Kalayani Nagar  (search key)->35");
//             push(35);
//         }

//         if (checkStation(name, "Ramwadi")) {
//             System.out.println("Ramwadi (search key)->36");
//             push(36);
//         }

//         if (checkStation(name, "Wadgaon Sheri Phata")) {
//             System.out.println("Wadgaon Sheri Phata (search key)->37");
//             push(37);
//         }

//         if (checkStation(name, "Viman Nagar Corner")) {
//             System.out.println("Viman Nagar Corner  (search key)->38");
//             push(38);
//         }

//         if (checkStation(name, "Tata Guardroom")) {
//             System.out.println("Tata Guardroom  (search key)->39");
//             push(39);
//         }

//         if (checkStation(name, "Kharadi Bypass")) {
//             System.out.println("Kharadi Bypass (search key)->40");
//             push(40);
//         }

//         if (checkStation(name, "Janak Baba Dargha")) {
//             System.out.println("Janak Baba Dargha  (search key)->41");
//             push(41);
//         }

//         if (checkStation(name, "Aaple Ghar")) {
//             System.out.println("Aaple Ghar (search key)->42");
//             push(42);
//         }

//         if (checkStation(name, "Khandve Nagar")) {
//             System.out.println("Khandve Nagar  (search key)->43");
//             push(43);
//         }

//         if (checkStation(name, "Ubale Nagar")) {
//             System.out.println("Ubale Nagar (search key)->44");
//             push(44);
//         }

//         if (checkStation(name, "Godown Talera")) {
//             System.out.println("Godown Talera (search key)->45");
//             push(45);
//         }

//         if (checkStation(name, "Soyrik Mangal Karyalay")) {
//             System.out.println("Soyrik Mangal Karyalay  (search key)->46");
//             push(46);
//         }

//         if (checkStation(name, "Kamal Bagh")) {
//             System.out.println("Kamal Bagh  (search key)->47");
//             push(47);
//         }

//         if (checkStation(name, "Wagholi")) {
//             System.out.println("Wagholi  (search key)->48");
//             push(48);
//         }
//     }

//     static void createGraph() {
//         // Create a 2D adjacency matrix of appropriate size (e.g., 49x49)
//         adj[0][1] = adj[1][0] = 2.1f;
//         adj[1][2] = adj[2][1] = 0.7f;
//         adj[2][3] = adj[3][2] = 1.5f;
//         adj[3][4] = adj[4][3] = 1.1f;
//         adj[4][5] = adj[5][4] = 2.4f;
//         adj[5][6] = adj[6][5] = 1.0f;
//         adj[6][7] = adj[7][6] = 1.4f;
//         adj[7][8] = adj[8][7] = 5.3f;
//         adj[8][9] = adj[9][8] = 2.9f;
//         adj[9][29] = adj[29][9] = 1.0f;
//         adj[29][10] = adj[10][29] = 1.1f;
//         adj[10][11] = adj[11][10] = 0.65f;
//         adj[11][12] = adj[12][11] = 1.7f;
//         adj[12][13] = adj[13][12] = 0.4f;
//         adj[13][14] = adj[14][13] = 1.3f;
//         adj[14][15] = adj[15][14] = 1.6f;
//         adj[15][16] = adj[16][15] = 1.6f;
//         adj[16][17] = adj[17][16] = 0.8f;
//         adj[17][18] = adj[18][17] = 1.1f;
//         adj[18][19] = adj[19][18] = 0.9f;

//         // Aqua Line
//         adj[20][21] = adj[21][20] = 1.15f;
//         adj[21][22] = adj[22][21] = 1.8f;
//         adj[22][23] = adj[23][22] = 1.7f;
//         adj[23][24] = adj[24][23] = 1.5f;
//         adj[24][25] = adj[25][24] = 1.8f;
//         adj[25][26] = adj[26][25] = 1.0f;
//         adj[26][27] = adj[27][26] = 0.8f;
//         adj[27][28] = adj[28][27] = 0.8f;
//         adj[28][29] = adj[29][28] = 0.8f;
//         adj[29][30] = adj[30][29] = 1.5f;
//         adj[30][31] = adj[31][30] = 1.3f;
//         adj[31][32] = adj[32][31] = 0.9f;
//         adj[32][33] = adj[33][32] = 1.3f;
//         adj[33][34] = adj[34][33] = 1.0f;
//         adj[34][35] = adj[35][34] = 2.5f;
//         adj[35][36] = adj[36][35] = 3.0f;
//         adj[36][37] = adj[37][36] = 1.2f;
//         adj[37][38] = adj[38][37] = 1.4f;
//         adj[38][39] = adj[39][38] = 1.4f;
//         adj[39][40] = adj[40][39] = 1.2f;
//         adj[40][41] = adj[41][40] = 0.75f;
//         adj[41][42] = adj[42][41] = 2.0f;
//         adj[42][43] = adj[43][42] = 2.4f;
//         adj[43][44] = adj[44][43] = 1.3f;
//         adj[44][45] = adj[45][44] = 1.8f;
//         adj[45][46] = adj[46][45] = 0.75f;
//         adj[46][47] = adj[47][46] = 1.4f;
//         adj[47][48] = adj[48][47] = 4.0f;
//     }

// }
