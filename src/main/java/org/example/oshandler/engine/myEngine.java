package org.example.oshandler.engine;

import org.example.oshandler.core.PCB;
import org.example.oshandler.scheduler.PriorityRR;
import org.example.oshandler.scheduler.Scheduler;
import org.example.oshandler.scheduler.MLFQ3;

import java.util.*;
import java.util.stream.Collectors;

public class myEngine implements SimulationEngine{

    private List<PCB> all = new ArrayList<>();
    private Scheduler scheduler;

    private int now = 0;
    private int quantum = 1;     // 时间片长度
    private int decay = 2;       // 每个时间片后优先级下降 +2（你也可以改成 +3）
    private PCB running = null;

    // 到达事件表：time -> PCBs
    private final Map<Integer, List<PCB>> arrivals = new HashMap<>();

    public void load(List<PCB> processes, Scheduler scheduler, int quantum, int decay) {
        this.all = new ArrayList<>(processes);
        this.scheduler = scheduler;
        this.quantum = quantum;
        this.decay = decay;
        this.now = 0;
        this.running = null;
        this.arrivals.clear();

        // 初始化到达表：arrivalTime 相同的放一起
        for (PCB p : all) {
            arrivals.computeIfAbsent(p.getArrivalTime(), k -> new ArrayList<>()).add(p);
            // 初始状态任务书要求 READY，但还没到达前其实可以当“未进入系统”
            // 这里保持你 PCB 构造里的 READY，不影响；引擎只在到达时入就绪队列
        }
    }


    public Snapshot step() {
        StringBuilder log = new StringBuilder();

        // 1) 处理到达
        List<PCB> arrived = arrivals.getOrDefault(now, List.of());


        for (PCB p : all) {

            if (p.getArrivalTime() <= now &&
                    p.getState() == PCB.State.READY &&
                    !scheduler.readyQueuesView().stream()
                            .flatMap(List::stream)
                            .toList()
                            .contains(p)) {

                scheduler.onArrive(p);

                log.append("到达 ").append(p.getName())
                        .append("(pri=").append(p.getPriority()).append(")；");
            }
        }

        // 2) 到达/唤醒 可能触发抢占（仅对 PriorityRR 演示）
        if (scheduler instanceof PriorityRR prr && running != null && running.getState() == PCB.State.EXCUTE) {
            // 如果本时刻有到达的，检查是否抢占
            for (PCB p : arrived) {
                if (prr.shouldPreempt(running, p)) {
                    // 抢占：把 running 放回就绪队列，然后立刻重新选
                    log.append("抢占：").append(p.getName()).append(" 优先级更高；");
                    // 把 running 交给 pickNext 来归队轮转更统一：这里直接手动归队也行
                    running.setState(PCB.State.READY);
                    prr.onArrive(running); // 归队（注意：onArrive 会去重）
                    running = null;
                    break;
                }
            }
        }
        // MLFQ 抢占（高队列优先）
        if (scheduler instanceof MLFQ3 mlfq && running != null) {
            if (mlfq.shouldPreempt(running)) {

                log.append("MLFQ抢占；");

                running.setState(PCB.State.READY);
                scheduler.onArrive(running);
                running = null;
            }
        }
        // 3) 若当前没有 running，则选一个
        if (running == null) {
            running = scheduler.pickNext(now, null);
            if (running != null) log.append("选中执行 ").append(running.getName()).append("；");
        }

        // 4) 运行一个时间片（或剩余不足一个片）
        if (running == null) {
            // CPU 空转：时间推进 1（也可以推进到下一个到达点，但先简单）
            now += 1;
            log.append("CPU空转；");
            return snapshot(log.toString());
        }

        int q = quantum;

        if (scheduler instanceof MLFQ3 mlfq) {
            q = mlfq.getQuantumFor(running);
        }

        int runTime = Math.min(q, running.remainingTime());

        running.setState(PCB.State.EXCUTE);
        running.setUsedTime(running.getUsedTime() + runTime);

        log.append("运行 ").append(running.getName())
                .append(" ").append(runTime).append("；");

        now += runTime;

        // 5) 时间片结束：优先级衰减（限制到 100）
        if (scheduler instanceof PriorityRR) {
            running.setPriority(Math.min(100, running.getPriority() + decay));
            log.append("时间片结束 pri+=")
                    .append(decay)
                    .append(" -> ")
                    .append(running.getPriority())
                    .append("；");
        }


        log.append("时间片结束 pri+=").append(decay)
                .append(" -> ").append(running.getPriority()).append("；");

        // 6) 完成判定
        if (running.isFinished()) {
            running.setFinishTime(now);
            scheduler.onFinish(running);
            log.append("完成 ").append(running.getName()).append("；");
            running = null;
        }

        // 7) 选择下一个（会把未完成的 running 归队，形成轮转）
        running = scheduler.pickNext(now, running);
        if (running != null) log.append("下一执行 ").append(running.getName()).append("；");

        return snapshot(log.toString());
    }

    public void block(String name) {
        PCB p = findByName(name);
        if (p == null) return;
        scheduler.onBlock(p);
        if (running == p) running = null;
    }

    public void wake(String name) {
        PCB p = findByName(name);
        if (p == null) return;
        scheduler.onWake(p);

        // 唤醒后抢占（PriorityRR）
        if (scheduler instanceof PriorityRR prr && prr.shouldPreempt(running, p)) {
            if (running != null && running.getState() == PCB.State.EXCUTE && !running.isFinished()) {
                running.setState(PCB.State.READY);
                prr.onArrive(running);
            }
            running = null;
        }
    }

    public void kill(String name) {
        PCB p = findByName(name);
        if (p == null) return;
        p.setFinishTime(now);
        scheduler.onFinish(p);
        if (running == p) running = null;
    }


    public boolean isAllFinished() {
        return all.stream().allMatch(PCB::isFinished);
    }

    private Snapshot snapshot(String log) {
        Snapshot s = new Snapshot();
        s.time = now;
        s.running = (running == null ? null : running.getName());
        s.all = all.stream()
                .sorted(Comparator.comparing(PCB::getName))
                .collect(Collectors.toList());
        s.readyQueues = scheduler.readyQueuesView();
        s.log = log;
        return s;
    }

    private PCB findByName(String name) {
        for (PCB p : all) {
            if (Objects.equals(p.getName(), name)) return p;
        }
        return null;
    }

    public double getAverageTurnaround() {
        return all.stream()
                .mapToInt(PCB::turnaroundTime)
                .average()
                .orElse(0);
    }
}
