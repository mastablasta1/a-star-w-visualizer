package pl.edu.agh.idziak.asw.visualizer.testing;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edu.agh.idziak.asw.astar.CurrentStateMonitor;
import pl.edu.agh.idziak.asw.common.Statistics;
import pl.edu.agh.idziak.asw.impl.AlgorithmType;
import pl.edu.agh.idziak.asw.impl.grid2d.*;
import pl.edu.agh.idziak.asw.model.ASWOutputPlan;
import pl.edu.agh.idziak.asw.visualizer.testing.grid2d.model.TestCase;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Tomasz on 04.09.2016.
 */
public class TestExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(TestExecutor.class);

    private final ObservableObjectValue<TestCase> activeTestCase;
    private final SimpleObjectProperty<Statistics> statistics;
    private final ExecutorService executorService;
    private final ExecutionObserver executionObserver;

    private final GridASWPlanner gridASWPlanner;
    private final GridAStarOnlyPlanner gridAStarOnlyPlanner;
    private final GridWavefrontOnlyPlanner gridWavefrontOnlyPlanner;

    public TestExecutor(ObservableObjectValue<TestCase> activeTestCase, ExecutionObserver executionObserver) {
        this.activeTestCase = activeTestCase;
        this.executionObserver = executionObserver;
        statistics = new SimpleObjectProperty<>();
        gridASWPlanner = new GridASWPlanner();
        gridASWPlanner.setAStarCurrentStateMonitor(new AStarMonitor());
        gridAStarOnlyPlanner = new GridAStarOnlyPlanner();
        gridWavefrontOnlyPlanner = new GridWavefrontOnlyPlanner();
        executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build());
    }

    

    private static class AStarMonitor implements CurrentStateMonitor<GridCollectiveState> {
        private int iterations = 0;

        @Override
        public void accept(GridCollectiveState currentState, Collection<GridCollectiveState> neighbors,
                           int openSetSize, int closedSetSize) {
            if (iterations++ % 100 == 0) {
                if (LOG.isDebugEnabled())
                    LOG.debug("iterations={}, openSet={}, closedSet={}", iterations, openSetSize, closedSetSize);
            }
        }

        private void reset() {
            iterations = 0;
        }
    }

    void invokeTestInNewThread(AlgorithmType algorithmType) {
        TestCase testCase = activeTestCase.get();
        if (testCase == null)
            return;
        TestExecutionTask testExecutionTask = new TestExecutionTask(testCase, algorithmType);
        executorService.submit(testExecutionTask);
    }

    private ASWOutputPlan<GridStateSpace, GridCollectiveState> executeTestWithGivenStrategy(GridInputPlan inputPlan, AlgorithmType algorithmType) {
        switch (algorithmType) {
            case ASW:
                return gridASWPlanner.calculatePlan(inputPlan);
            case ASTAR_ONLY:
                return gridAStarOnlyPlanner.calculatePlan(inputPlan);
            case WAVEFRONT:
                return gridWavefrontOnlyPlanner.calculatePlan(inputPlan);
        }
        return null;
    }

    ObservableObjectValue<Statistics> statisticsProperty() {
        return statistics;
    }

    private class TestExecutionTask extends Task<Void> {

        private final TestCase testCase;
        private AlgorithmType algorithmType;
        private ASWOutputPlan<GridStateSpace, GridCollectiveState> outputPlan;

        private TestExecutionTask(TestCase testCase, AlgorithmType algorithmType) {
            this.testCase = testCase;
            this.algorithmType = algorithmType;
        }

        @Override
        protected Void call() throws Exception {
            outputPlan = executeTestWithGivenStrategy(testCase.getInputPlan(), algorithmType);
            return null;
        }

        @Override
        protected void succeeded() {
            LOG.info("Test executed, path: {}", outputPlan);
            testCase.setOutputPlan(outputPlan);
            executionObserver.executionSucceeded(testCase);
        }

        @Override
        protected void failed() {
            executionObserver.executionFailed(getException());
        }
    }
}
