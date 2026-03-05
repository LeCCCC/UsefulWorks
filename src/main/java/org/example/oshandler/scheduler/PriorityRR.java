package org.example.oshandler.scheduler;

import org.example.oshandler.core.PCB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PriorityRR implements Scheduler{
    //就绪队列，保持进入顺序，同优先级按这个轮转
    private final List<PCB> ready = new ArrayList<>();

    @Override
    public void onArrive(PCB pcb) {
    if(pcb ==null)return;
    pcb.setState(PCB.State.READY);
    addReady(pcb);//到达时进入就绪队列
    }

    @Override
    public void onBlock(PCB pcb) {
    if(pcb==null)return;
    pcb.setState(PCB.State.WAIT);
    ready.remove(pcb);//如果在就绪队列里则移除
    }

    @Override
    public void onWake(PCB pcb) {
        if (pcb == null) return;
        pcb.setState(PCB.State.READY);
        addReady(pcb);
    }

    @Override
    public void onFinish(PCB pcb) {
        if (pcb == null) return;
        pcb.setState(PCB.State.FINISH);
        ready.remove(pcb);
    }
//挑选一个传入到ready队列里的进程返回
    @Override
    public PCB pickNext(int now, PCB running) {
        //正在运行的PCB running由engine传入，如果running没结束则engine会在一个时间片结束后调用该方法把该进程扔回ready轮转;
        if(running !=null
                && running.getState()==PCB.State.EXCUTE
                && !running.isFinished()){
            running.setState(PCB.State.READY);
            addReady(running);
        }

        if(ready.isEmpty())return null;


        //获取此时优先级最高的进程
        int bestIdx = 0;
        int bestPriority = ready.get(0).getPriority();
        for (int i = 1; i < ready.size(); i++) {
            int p = ready.get(i).getPriority();
            if (p < bestPriority) {
                bestPriority = p;
                bestIdx = i;
            }
        }
        PCB next = ready.remove(bestIdx);
        next.setState(PCB.State.EXCUTE);

        //如果该进程是首次执行记录下它的开始时间
        if(next.getStartTime()==null){
            next.setStartTime(now);
        }return next;
    }

    @Override
    public List<List<PCB>> readyQueuesView() {
        // 返回只读快照，避免 UI 修改内部队列
        return List.of(Collections.unmodifiableList(new ArrayList<>(ready)));
    }

    //保证不重复加入该类维护的队列，避免 arrive/wake 连点导致重复
    private void addReady(PCB pcb) {
        if (!ready.contains(pcb)) ready.add(pcb);
    }

    //判断唤醒或者到达的pcb是否优于running，是的话则抢占
    public boolean shouldPreempt(PCB running, PCB awakenedOrArrived) {
        if (running == null) return true;
        if (awakenedOrArrived == null) return false;
        if (running.getState() != PCB.State.EXCUTE) return true;
        // 数值越小优先级越高
        return awakenedOrArrived.getPriority() < running.getPriority();
    }
    }

