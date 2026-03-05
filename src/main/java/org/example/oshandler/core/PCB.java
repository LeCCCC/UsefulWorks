package org.example.oshandler.core;

public class PCB {
    public enum State{
        WAIT,READY,EXCUTE,FINISH
    }
    private String name;
    private Integer priority;
    private Integer arrivalTime;
    private Integer totalTime;
    private Integer usedTime;
    private State state ;
    private Integer startTime;
    private Integer finishTime;

    public PCB(String name, int priority, int arrivalTime, int totalTime) {
        this.name = name;
        this.priority = priority;
        this.arrivalTime = arrivalTime;
        this.totalTime = totalTime;
        this.usedTime = 0;
        this.state = State.READY;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Integer arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Integer getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(Integer totalTime) {
        this.totalTime = totalTime;
    }

    public Integer getUsedTime() {
        return usedTime;
    }

    public void setUsedTime(Integer usedTime) {
        this.usedTime = usedTime;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }

    public Integer getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Integer finishTime) {
        this.finishTime = finishTime;
    }

    public int remainingTime(){
        return totalTime-usedTime;
    }//返回该PCB还要运行多长时间
    public boolean isFinished(){
        return usedTime>=totalTime;
    }//返回是否运行完毕
    public Integer turnaroundTime(){
        if(finishTime == null){
            return null;
        }return finishTime-arrivalTime;
    }//返回周转时间
}
