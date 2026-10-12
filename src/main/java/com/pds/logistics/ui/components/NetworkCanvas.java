package com.pds.logistics.ui.components;

import com.pds.logistics.model.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Custom JavaFX Canvas component for rendering the PDS logistics network graph.
 *
 * <p>Visual encoding:
 * <ul>
 *   <li>DEPOT nodes – amber square with white label</li>
 *   <li>WAREHOUSE nodes – teal rounded rectangle</li>
 *   <li>FAIR_PRICE_SHOP nodes – green circle</li>
 *   <li>Unused route edges – dark grey thin lines</li>
 *   <li>Active flow edges – bright amber/teal thick lines, width ∝ allocated flow</li>
 *   <li>Disabled/failed route edges – red dashed lines</li>
 * </ul>
 */
public class NetworkCanvas extends Canvas {

    // ── Colour palette ────────────────────────────────────────────────────────
    private static final Color BG          = Color.web("#0d1117");
    private static final Color GRID_LINE   = Color.web("#1c2230");
    private static final Color EDGE_IDLE   = Color.web("#30363d");
    private static final Color EDGE_FLOW   = Color.web("#f59e0b");
    private static final Color EDGE_FAIL   = Color.web("#ef4444");
    private static final Color NODE_DEPOT  = Color.web("#f59e0b");
    private static final Color NODE_WH     = Color.web("#14b8a6");
    private static final Color NODE_FPS    = Color.web("#22c55e");
    private static final Color NODE_BORDER = Color.web("#1c2230");
    private static final Color TEXT_COLOR  = Color.web("#e6edf3");
    private static final Color TEXT_MUTED  = Color.web("#8b949e");
    private static final Color TOOLTIP_BG  = Color.web("#1c2230");

    private static final double NODE_R = 20;

    // ── Data ──────────────────────────────────────────────────────────────────
    private List<Location>       locations;
    private List<TransportRoute> routes;
    private List<AllocationResult> allocations;

    // ── Tooltip state ─────────────────────────────────────────────────────────
    private Location       hoveredNode;
    private TransportRoute hoveredRoute;
    private double         mouseX, mouseY;

    public NetworkCanvas(double width, double height) {
        super(width, height);

        // Mouse hover for tooltips
        setOnMouseMoved(this::onMouseMoved);

        // Redraw when resized
        widthProperty().addListener((obs, o, n) -> draw());
        heightProperty().addListener((obs, o, n) -> draw());
    }

    /** Called by the controller to set data and trigger a redraw */
    public void setData(List<Location>       locations,
                        List<TransportRoute> routes,
                        List<AllocationResult> allocations) {
        this.locations   = locations;
        this.routes      = routes;
        this.allocations = allocations;
        draw();
    }

    // ─── Main draw routine ────────────────────────────────────────────────────

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double W = getWidth();
        double H = getHeight();

        // Background
        gc.setFill(BG);
        gc.fillRect(0, 0, W, H);

        // Subtle grid
        drawGrid(gc, W, H);

        if (locations == null || locations.isEmpty()) {
            gc.setFill(TEXT_MUTED);
            gc.setFont(Font.font("Inter", 14));
            gc.fillText("No network data loaded. Add locations and routes first.", 40, H / 2.0);
            return;
        }

        // Draw edges first (below nodes)
        if (routes != null) {
            for (TransportRoute route : routes) {
                drawEdge(gc, route, W, H);
            }
        }

        // Draw nodes on top
        for (Location loc : locations) {
            drawNode(gc, loc, W, H);
        }

        // Draw tooltip if hovering
        if (hoveredNode != null) {
            drawNodeTooltip(gc, hoveredNode, mouseX, mouseY);
        }
    }

    private void drawGrid(GraphicsContext gc, double W, double H) {
        gc.setStroke(GRID_LINE);
        gc.setLineWidth(0.5);
        for (double x = 0; x < W; x += 60) {
            gc.strokeLine(x, 0, x, H);
        }
        for (double y = 0; y < H; y += 60) {
            gc.strokeLine(0, y, W, y);
        }
    }

    private void drawEdge(GraphicsContext gc, TransportRoute route, double W, double H) {
        Location src  = route.getSource();
        Location dst  = route.getDestination();
        if (src == null || dst == null) return;

        double x1 = src.getCanvasX() * W;
        double y1 = src.getCanvasY() * H;
        double x2 = dst.getCanvasX() * W;
        double y2 = dst.getCanvasY() * H;

        // Find allocation for this route (if any)
        double flow = 0;
        if (allocations != null) {
            for (AllocationResult a : allocations) {
                if (a.getRoute().getId() == route.getId()) {
                    flow = a.getAllocatedMT();
                    break;
                }
            }
        }

        boolean isFailed = !route.isActive();
        boolean hasFlow  = flow > 0.001;

        gc.save();
        if (isFailed) {
            gc.setStroke(EDGE_FAIL);
            gc.setLineWidth(1.5);
            gc.setLineDashes(8, 5);
        } else if (hasFlow) {
            double ratio = Math.min(1.0, flow / Math.max(1, route.getCapacityMT()));
            gc.setStroke(EDGE_FLOW);
            gc.setLineWidth(1.5 + ratio * 5.0);
            gc.setLineDashes(null);
            // Glow effect
            gc.setEffect(new javafx.scene.effect.DropShadow(8, EDGE_FLOW));
        } else {
            gc.setStroke(EDGE_IDLE);
            gc.setLineWidth(1.0);
            gc.setLineDashes(null);
        }

        gc.strokeLine(x1, y1, x2, y2);

        // Arrowhead
        drawArrow(gc, x1, y1, x2, y2, isFailed ? EDGE_FAIL : hasFlow ? EDGE_FLOW : EDGE_IDLE);

        // Flow label
        if (hasFlow) {
            double mx = (x1 + x2) / 2;
            double my = (y1 + y2) / 2;
            gc.setEffect(null);
            gc.setFill(TEXT_COLOR);
            gc.setFont(Font.font("Inter", FontWeight.BOLD, 10));
            gc.fillText("%.0f MT".formatted(flow), mx + 4, my - 4);
        }

        gc.restore();
    }

    private void drawArrow(GraphicsContext gc, double x1, double y1, double x2, double y2, Color color) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        double ux = dx / len, uy = dy / len;

        // Arrow tip is NODE_R before destination
        double tipX = x2 - ux * (NODE_R + 3);
        double tipY = y2 - uy * (NODE_R + 3);
        double perpX = -uy * 5, perpY = ux * 5;

        gc.setFill(color);
        gc.fillPolygon(
                new double[]{tipX, tipX - ux * 10 + perpX, tipX - ux * 10 - perpX},
                new double[]{tipY, tipY - uy * 10 + perpY, tipY - uy * 10 - perpY},
                3
        );
    }

    private void drawNode(GraphicsContext gc, Location loc, double W, double H) {
        double cx = loc.getCanvasX() * W;
        double cy = loc.getCanvasY() * H;

        Color fillColor = switch (loc.getType()) {
            case DEPOT             -> NODE_DEPOT;
            case WAREHOUSE         -> NODE_WH;
            case FAIR_PRICE_SHOP   -> NODE_FPS;
            default                -> Color.GREY;
        };

        gc.save();

        // Shadow / glow for hovered node
        if (loc == hoveredNode) {
            gc.setEffect(new javafx.scene.effect.DropShadow(16, fillColor));
        }

        // Draw shape based on type
        switch (loc.getType()) {
            case DEPOT -> {
                // Square for depots
                gc.setFill(fillColor);
                gc.fillRect(cx - NODE_R, cy - NODE_R, NODE_R * 2, NODE_R * 2);
                gc.setStroke(NODE_BORDER);
                gc.setLineWidth(2);
                gc.strokeRect(cx - NODE_R, cy - NODE_R, NODE_R * 2, NODE_R * 2);
            }
            case WAREHOUSE -> {
                // Rounded rect for warehouses
                gc.setFill(fillColor);
                gc.fillRoundRect(cx - NODE_R, cy - NODE_R, NODE_R * 2, NODE_R * 2, 8, 8);
                gc.setStroke(NODE_BORDER);
                gc.setLineWidth(2);
                gc.strokeRoundRect(cx - NODE_R, cy - NODE_R, NODE_R * 2, NODE_R * 2, 8, 8);
            }
            default -> {
                // Circle for FPS and others
                gc.setFill(fillColor);
                gc.fillOval(cx - NODE_R, cy - NODE_R, NODE_R * 2, NODE_R * 2);
                gc.setStroke(NODE_BORDER);
                gc.setLineWidth(2);
                gc.strokeOval(cx - NODE_R, cy - NODE_R, NODE_R * 2, NODE_R * 2);
            }
        }

        gc.setEffect(null);

        // Node code label (inside)
        String icon = switch (loc.getType()) {
            case DEPOT           -> "D";
            case WAREHOUSE       -> "W";
            case FAIR_PRICE_SHOP -> "F";
            default              -> "?";
        };
        gc.setFill(Color.web("#0d1117"));
        gc.setFont(Font.font("Inter", FontWeight.BOLD, 11));
        gc.fillText(icon, cx - 4, cy + 4);

        // Name label below node
        gc.setFill(TEXT_COLOR);
        gc.setFont(Font.font("Inter", 10));
        String shortName = loc.getName().length() > 16 ? loc.getName().substring(0, 14) + "…" : loc.getName();
        gc.fillText(shortName, cx - shortName.length() * 2.8, cy + NODE_R + 14);

        gc.restore();
    }

    private void drawNodeTooltip(GraphicsContext gc, Location loc, double mx, double my) {
        String text = "%s\n%s\n%s, %s".formatted(
                loc.getName(), loc.getType().getDisplayName(), loc.getDistrict(), loc.getState());
        String[] lines = text.split("\n");

        double tw = 180, th = lines.length * 18 + 16;
        double tx = Math.min(mx + 10, getWidth() - tw - 10);
        double ty = Math.min(my + 10, getHeight() - th - 10);

        gc.setFill(TOOLTIP_BG);
        gc.fillRoundRect(tx, ty, tw, th, 8, 8);
        gc.setStroke(Color.web("#30363d"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(tx, ty, tw, th, 8, 8);
        gc.setFill(TEXT_COLOR);
        gc.setFont(Font.font("Inter", 11));
        for (int i = 0; i < lines.length; i++) {
            gc.fillText(lines[i], tx + 8, ty + 16 + i * 18);
        }
    }

    // ─── Mouse interaction ────────────────────────────────────────────────────

    private void onMouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        hoveredNode = null;

        if (locations == null) return;
        double W = getWidth(), H = getHeight();

        for (Location loc : locations) {
            double cx = loc.getCanvasX() * W;
            double cy = loc.getCanvasY() * H;
            if (Math.hypot(mouseX - cx, mouseY - cy) <= NODE_R + 4) {
                hoveredNode = loc;
                break;
            }
        }
        draw();
    }
}
