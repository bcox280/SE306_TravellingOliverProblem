package uoa.se306.travellingoliverproblem.schedule;

import uoa.se306.travellingoliverproblem.graph.Node;

/*
A ScheduledEntry is a node that has been scheduled on a processor
 */
public class ScheduleEntry implements Comparable<ScheduleEntry> {
    private int startTime;
    private int length;
    private Node node;

    // Constructor
    public ScheduleEntry(int startTime, Node node) {
        this.startTime = startTime;
        this.node = node;
        this.length = node.getCost();
    }

    // Returns the start time of the ScheduledEntry
    public int getStartTime() {
        return startTime;
    }

    // Returns the run time of the ScheduledEntry
    public int getLength() {
        return length;
    }

    // Returns the time at which the ScheduledEntry ended on the processor
    public int getEndTime() {
        return startTime + length;
    }

    @Override
    public String toString() {
        return node.toString();
    }

    // Compare ScheduleEntries based on their end time
    @Override
    public int compareTo(ScheduleEntry o) {
        return Integer.compare(startTime + length, o.startTime + o.length);
    }
}
