package pl.edu.agh.idziak.asw.visualizer.gui.root;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableObjectValue;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edu.agh.idziak.asw.OutputPlan;
import pl.edu.agh.idziak.asw.grid2d.G2DCollectiveState;
import pl.edu.agh.idziak.asw.grid2d.G2DEntityState;
import pl.edu.agh.idziak.asw.grid2d.G2DStateSpace;
import pl.edu.agh.idziak.asw.visualizer.testing.grid2d.model.Entity;
import pl.edu.agh.idziak.asw.visualizer.testing.grid2d.model.TestCase;
import pl.edu.agh.idziak.common.UntypedTwoMapsIterator;
import pl.edu.agh.idziak.common.WalkingPairIterator;

import java.util.List;

/**
 * Created by Tomasz on 28.08.2016.
 */
public class GridCanvasController {
    private static final Logger LOG = LoggerFactory.getLogger(GridCanvasController.class);
    private static final int CELL_WIDTH = 40;
    private static final int ENTITY_WIDTH = CELL_WIDTH * 2 / 3;
    private Canvas canvas;
    private TestCase currentTestCase;
    private final ChangeListener<OutputPlan<G2DStateSpace, G2DCollectiveState, G2DEntityState, Integer, Double>> outputPlanChangeListener
            = (obs, oldVal, newVal) -> repaint();

    public GridCanvasController(Canvas canvas, ObservableObjectValue<TestCase>
            testCaseObjectProperty) {
        this.canvas = canvas;
        testCaseObjectProperty.addListener((observable, oldValue, newTestCase) -> {
            if (currentTestCase != null) {
                currentTestCase.outputPlanProperty().removeListener(outputPlanChangeListener);
            }
            drawTestCase(newTestCase);
            currentTestCase.outputPlanProperty().addListener(outputPlanChangeListener);
        });
    }

    private void repaint() {
        drawTestCase(currentTestCase);
    }

    private void drawTestCase(TestCase testCase) {
        LOG.info("Redrawing test case");
        currentTestCase = testCase;
        GraphicsContext gc = canvas.getGraphicsContext2D();

        if (testCase == null) {
            return;
        }

        G2DStateSpace stateSpace = testCase.getInputPlan().getStateSpace();

        drawStateSpace(gc, stateSpace);

        if (currentTestCase.outputPlanProperty().isNotNull().get()) {
            drawOutputPlan(gc);
        }
        drawEntities(gc, testCase);
        drawDeviationZones(gc, testCase);
    }

    private static void drawDeviationZones(GraphicsContext gc, TestCase testCase) {
        if (testCase.outputPlanProperty().isNull().get()) {
            return;
        }

        testCase.outputPlanProperty().get().getDeviationZonePlans().forEach(devZonePlan ->
                devZonePlan.getDeviationZone().getStates().forEach(entityState ->
                        drawDevZoneState(gc, entityState)));
    }

    private static void drawDevZoneState(GraphicsContext gc, G2DEntityState state) {
        gc.save();

        int topY = getTopPosForIndex(state.getRow());
        int bottomY = getTopPosForIndex(state.getRow() + 1);
        int leftX = getTopPosForIndex(state.getCol());
        int rightX = getTopPosForIndex(state.getCol() + 1);

        clipRect(gc, leftX, topY, CELL_WIDTH, CELL_WIDTH);

        int linesInBetween = 4;
        double spaceBetweenLines = (double) CELL_WIDTH / linesInBetween;

        gc.setStroke(Color.GREY);
        gc.setLineWidth(0.3);
        for (int i = 0; i < linesInBetween; i++) {
            double y1 = topY + spaceBetweenLines * (i + 1);
            double x2 = leftX + spaceBetweenLines * (i + 1);
            gc.strokeLine(leftX, y1, x2, topY);
        }

        for (int i = 0; i < linesInBetween; i++) {
            double y2 = topY + spaceBetweenLines * (i + 1);
            double x1 = leftX + spaceBetweenLines * (i + 1);
            gc.strokeLine(x1, bottomY, rightX, y2);
        }

        gc.restore();
    }

    private static void clipRect(GraphicsContext gc, int x, int y, int width, int height) {
        gc.beginPath();
        gc.rect(x, y, x + width, y + height);
        gc.closePath();
        gc.clip();
    }

    private static int getTopPosForIndex(int index) {
        return index * CELL_WIDTH;
    }

    private void drawStateSpace(GraphicsContext gc, G2DStateSpace stateSpace) {
        int rows = stateSpace.getRows();
        int cols = stateSpace.getCols();

        canvas.setWidth(CELL_WIDTH * cols);
        canvas.setHeight(CELL_WIDTH * rows);
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);

        for (int curRow = 0; curRow <= rows; curRow++) {
            int yPos = curRow * CELL_WIDTH;
            gc.strokeLine(0, yPos, cols * CELL_WIDTH, yPos);
        }

        for (int curCol = 0; curCol <= cols; curCol++) {
            int xPos = curCol * CELL_WIDTH;
            gc.strokeLine(xPos, 0, xPos, rows * CELL_WIDTH);
        }
    }

    private void drawOutputPlan(GraphicsContext gc) {
        List<G2DCollectiveState> collectivePath = currentTestCase.outputPlanProperty().get().getCollectivePath().get();

        WalkingPairIterator<G2DCollectiveState> it = new WalkingPairIterator<>(collectivePath);
        while (it.hasNext()) {
            it.next();
            drawPathFragments(it.getFirst(), it.getSecond(), gc);
        }
    }

    private static void drawPathFragments(G2DCollectiveState first, G2DCollectiveState second, GraphicsContext gc) {
        UntypedTwoMapsIterator<G2DEntityState> it =
                new UntypedTwoMapsIterator<>(first.getEntityStates(), second.getEntityStates());

        while (it.hasNext()) {
            it.next();
            drawPathFragment(it.getFirstValue(), it.getSecondValue(), gc);
        }
    }

    private static void drawPathFragment(G2DEntityState first, G2DEntityState second, GraphicsContext gc) {
        gc.save();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeLine(getCenterPosForIndex(first.getCol()),
                getCenterPosForIndex(first.getRow()),
                getCenterPosForIndex(second.getCol()),
                getCenterPosForIndex(second.getRow()));
        gc.restore();
    }

    private static int getCenterPosForIndex(int pos) {
        return getTopPosForIndex(pos) + CELL_WIDTH / 2;
    }

    private static void drawEntities(GraphicsContext gc, TestCase testCase) {
        testCase.getInputPlan()
                .getInitialCollectiveState()
                .getEntityStates()
                .entrySet()
                .forEach(entry -> drawInitialEntityState(entry.getKey(), entry.getValue(), gc));
        testCase.getInputPlan()
                .getTargetCollectiveState()
                .getEntityStates()
                .entrySet()
                .forEach(entry -> drawTargetEntityState(entry.getKey(), entry.getValue(), gc));
    }

    private static void drawTargetEntityState(Object entity, G2DEntityState state, GraphicsContext gc) {
        gc.save();
        int leftX = getTopPosForIndex(state.getCol());
        int topY = getTopPosForIndex(state.getRow());

        gc.setFill(Color.LIGHTBLUE);
        int entityRectOffset = (CELL_WIDTH - ENTITY_WIDTH) / 2;
        gc.fillRect(leftX + entityRectOffset, topY + entityRectOffset, ENTITY_WIDTH, ENTITY_WIDTH);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        if (entity instanceof Entity) {
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font(20));
            gc.fillText(
                    ((Entity) entity).getId().toString(),
                    leftX + CELL_WIDTH / 2,
                    topY + CELL_WIDTH / 2
            );
        }

        gc.restore();
    }

    private static void drawInitialEntityState(Object entity, G2DEntityState state, GraphicsContext gc) {
        gc.save();
        int leftX = CELL_WIDTH * state.getCol();
        int topY = CELL_WIDTH * state.getRow();
        /*gc.beginPath();
        gc.rect(leftX, topY, leftX + CELL_WIDTH, topY + CELL_WIDTH);
        gc.closePath();
        gc.clip();*/

        gc.setFill(Color.ORANGE);
        int entityRectOffset = (CELL_WIDTH - ENTITY_WIDTH) / 2;
        gc.fillRect(leftX + entityRectOffset, topY + entityRectOffset, ENTITY_WIDTH, ENTITY_WIDTH);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        if (entity instanceof Entity) {
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font(20));
            gc.fillText(
                    ((Entity) entity).getId().toString(),
                    leftX + CELL_WIDTH / 2,
                    topY + CELL_WIDTH / 2
            );
        }

        gc.restore();
    }

    private void debugFillCanvas(GraphicsContext gc) {
        canvas.getGraphicsContext2D().setFill(Color.GREEN);
        canvas.getGraphicsContext2D().fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

}