package uoa.se306.travellingoliverproblem.visualiser;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.chart.ChartData;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.util.Duration;
import uoa.se306.travellingoliverproblem.fileIO.DotFileWriter;
import uoa.se306.travellingoliverproblem.graph.Graph;
import uoa.se306.travellingoliverproblem.graph.Node;
import uoa.se306.travellingoliverproblem.schedule.Schedule;
import uoa.se306.travellingoliverproblem.scheduler.SchedulerRunner;
import uoa.se306.travellingoliverproblem.visualiser.graph.GraphNode;
import uoa.se306.travellingoliverproblem.visualiser.graph.SequentialGraphDrawer;
import uoa.se306.travellingoliverproblem.visualiser.schedule.ScheduleDrawer;

import java.util.Map;

import static uoa.se306.travellingoliverproblem.scheduler.Scheduler.COMPUTATIONAL_LOAD;

public class FXController {
    @FXML
    private Pane graphPane;

    @FXML
    private Pane schedulePane;

    @FXML
    private HBox tilesBox;

    @FXML
    private Pane statusPane;

    @FXML
    private Text statusText;

    @FXML
    private Text scheduleTitleText;

    @FXML
    private ScrollPane graphScrollPane;

    private Map<Node, GraphNode> graphNodeMap;
    private Timeline timeline;
    private long lastBranches = 0;
    private Schedule lastSchedule;

    private void drawGraph(Graph graph) {
        SequentialGraphDrawer drawer = new SequentialGraphDrawer(graphPane, graph, graphScrollPane);
        drawer.drawGraph();
        graphNodeMap = drawer.getGraphNodes();
    }

    public void startProcessing(Graph inputGraph, int processors, String outputName) {
        Task<Void> task = SchedulerRunner.getInstance().startSchedulerJavaFXTask(inputGraph, processors);
        drawGraph(SchedulerRunner.getInstance().getInputGraph());
        long startTime = System.nanoTime();

        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                long endTime = System.nanoTime();
                statusText.setText("Done, took " + (endTime - startTime) / 1000000 + " ms");
                statusPane.setStyle("-fx-background-color: green;");
                timeline.setCycleCount(1);
                timeline.playFromStart();
                SchedulerRunner.getInstance().printResult();
                drawSchedule(SchedulerRunner.getInstance().getSchedule());
                scheduleTitleText.setText("Best Schedule");
                DotFileWriter fileWriter = new DotFileWriter(inputGraph, SchedulerRunner.getInstance().getSchedule(), outputName);
                fileWriter.outputSchedule();
            }
        });
        new Thread(task).start();
        startPolling();
    }

    private void startPolling() {
        // Setup tiles
        Tile memoryTile = TileBuilder.create().skinType(Tile.SkinType.BAR_GAUGE)
                .title("Memory usage")
                .unit("MB")
                .maxValue(4000)
                .animated(true)
                .build();

        Tile generatedBranches = TileBuilder.create().skinType(Tile.SkinType.SMOOTH_AREA_CHART)
                .title("Branches Generated")
                .decimals(0)
                .minWidth(400)
                .chartData(new ChartData(0), new ChartData(0))
                .animated(false)
                .smoothing(true)
                .build();

        Tile boundedBranches = TileBuilder.create().skinType(Tile.SkinType.DONUT_CHART)
                .title("Branch Bound ratio")
                .decimals(0)
                .animated(true)
                .minWidth(350)
                .build();

        Tile branchRate = TileBuilder.create().skinType(Tile.SkinType.SMOOTH_AREA_CHART)
                .title("Branches/sec")
                .decimals(0)
                .minWidth(300)
                .chartData(new ChartData(0), new ChartData(0))
                .animated(false)
                .smoothing(true)
                .build();

        Tile branchConsidered = TileBuilder.create().skinType(Tile.SkinType.SMOOTH_AREA_CHART)
                .title("Branches Considered")
                .decimals(0)
                .chartData(new ChartData(0), new ChartData(0))
                .animated(false)
                .minWidth(300)
                .smoothing(true)
                .build();

        Tile bestTime = TileBuilder.create().skinType(Tile.SkinType.SMOOTH_AREA_CHART)
                .title("Current best schedule length")
                .decimals(0)
                .chartData(new ChartData(COMPUTATIONAL_LOAD), new ChartData(COMPUTATIONAL_LOAD))
                .animated(true)
                .smoothing(true)
                .build();

        tilesBox.getChildren().addAll(memoryTile, boundedBranches, generatedBranches, branchRate, branchConsidered, bestTime);
        timeline = new Timeline(new KeyFrame(Duration.millis(1000), event -> {
            // Check for new schedule
            if (lastSchedule == null || !lastSchedule.equals(SchedulerRunner.getInstance().getScheduler().getCurrentBestSchedule())) {
                lastSchedule = SchedulerRunner.getInstance().getScheduler().getCurrentBestSchedule();
                drawSchedule(lastSchedule);
                bestTime.addChartData(new ChartData(lastSchedule.getOverallTime()));
            }
            // Update statistics
            double memoryUse = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1000000d;
            long totalBranches = SchedulerRunner.getInstance().getScheduler().getBranchesConsidered() + SchedulerRunner.getInstance().getScheduler().getBranchesKilled();
            memoryTile.setValue(memoryUse);
            generatedBranches.addChartData(new ChartData(totalBranches));
            branchRate.addChartData(new ChartData(totalBranches - lastBranches));
            branchConsidered.addChartData(new ChartData(SchedulerRunner.getInstance().getScheduler().getBranchesConsidered()));
            // Setup data for pie chart
            ChartData cd1 = new ChartData("Considered", SchedulerRunner.getInstance().getScheduler().getBranchesConsidered(), Tile.GREEN);
            ChartData cd2 = new ChartData("Pruned", SchedulerRunner.getInstance().getScheduler().getBranchesKilled() - SchedulerRunner.getInstance().getScheduler().getBranchesKilledDuplication(), Tile.BLUE);
            ChartData cd3 = new ChartData("Duplicates", SchedulerRunner.getInstance().getScheduler().getBranchesKilledDuplication(), Tile.YELLOW_ORANGE);
            boundedBranches.setChartData(cd1, cd2, cd3);
            lastBranches = totalBranches;
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void drawSchedule(Schedule schedule) {
        schedulePane.getChildren().clear();
        ScheduleDrawer drawer = new ScheduleDrawer(schedulePane, schedule, graphNodeMap);
        drawer.drawSchedule();
    }
}
