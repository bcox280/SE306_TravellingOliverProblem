package uoa.se306.travellingoliverproblem.scheduler;

import uoa.se306.travellingoliverproblem.graph.Graph;
import uoa.se306.travellingoliverproblem.graph.Node;
import uoa.se306.travellingoliverproblem.schedule.*;
import uoa.se306.travellingoliverproblem.scheduler.heuristics.CostFunction;

import java.util.*;

/*
Scheduler for the A Star Algorithm
 */
public class AStarSearchScheduler extends Scheduler {

    private boolean foundOptimal = false;

    private Set<MinimalSchedule> existingSchedules;
    private PriorityQueue<ScheduleAStar> candidateSchedules = new PriorityQueue<>();

    public AStarSearchScheduler(Graph graph, int amountOfProcessors) {
        super(graph, amountOfProcessors);
    }

    @Override
    protected void calculateSchedule(Schedule currentSchedule){
        if (currentSchedule instanceof ScheduleAStar){
            solveAStar((ScheduleAStar)currentSchedule);
        }else{
            throw new InvalidScheduleException("currentSchedule is not an instance of ScheduleAStar");
        }
    }


    private void solveAStar(ScheduleAStar currentSchedule) {

        candidateSchedules.add(currentSchedule);


        while (!foundOptimal) {

            ScheduleAStar partial = candidateSchedules.poll();

            if (partial.getAvailableNodes().isEmpty()){
                foundOptimal = true;
                bestSchedule = partial;
            }else {
                // Get all the available nodes in the schedule
                Set<Node> availableNodes = new HashSet<>(partial.getAvailableNodes());
                for (Node node : availableNodes) {
                    ScheduledProcessor[] processors = partial.getProcessors();

                    for (int i = 0; i < processors.length; i++) {
                        ScheduledProcessor processor = processors[i];
                        int processorStartTime;
                        int startTime = 0;
                        // iterate over all the parent nodes
                        for (Node parentNode : node.getParents().keySet()) {
                            // for all the processors of this current schedule
                            // if any of the processors contains the parentNode
                            // get the endTime of when this parentNode finishes inside that processor
                            for (ScheduledProcessor checkProcessor : partial.getProcessors()) {
                                ScheduleEntry sEntry = checkProcessor.getEntry(parentNode);
                                if (sEntry != null) {
                                    processorStartTime = sEntry.getEndTime();
                                    // add the communication cost between childNode and parentNode if not on same processor
                                    processorStartTime += processor != checkProcessor ? parentNode.getChildren().get(node) : 0;
                                    // if the processorStartTime is larger than the current startTime
                                    // update the current startTime
                                    if (processorStartTime > startTime) {
                                        startTime = processorStartTime;
                                        break;
                                    }
                                }
                            }
                        }
                        //get the earliestStartTime that this available node can be scheduled (In this processor)
                        startTime = processor.getEarliestStartAfter(startTime, node.getCost());
                        //create a copy of our partialSchedule
                        ScheduleAStar tempSchedule = new ScheduleAStar(partial);
                        //add the availableNode into processor i at time startTime in the schedule
                        tempSchedule.addToSchedule(node, i, startTime);
                        tempSchedule.getCostFunction();
                        //================================ debugging mode=====================================
                        //System.out.println("tempSchedule function cost = " + tempSchedule.getCost() + " tempSchedule available nodes = " + tempSchedule.getAvailableNodes());


                        //====================================================================================
                        candidateSchedules.add(tempSchedule);

                    }
                }
            }
        }
    }
}
