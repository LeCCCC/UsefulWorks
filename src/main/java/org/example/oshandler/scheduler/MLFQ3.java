package org.example.oshandler.scheduler;

import org.example.oshandler.core.PCB;

import java.util.*;

public class MLFQ3 implements Scheduler{
    // 0=Q1, 1=Q2, 2=Q3
    private final Deque<PCB>[] queues = new Deque[] {
            new ArrayDeque<>(),
            new ArrayDeque<>(),
            new ArrayDeque<>()
    };

    // 记录每个进程当前所在队列级别
    private final Map<PCB, Integer> levelMap = new IdentityHashMap<>();

    // 基础时间片（Q1 用它；Q2=2*base；Q3=4*base）
    private final int baseQuantum;

    public MLFQ3(int baseQuantum) {
        if (baseQuantum <= 0) throw new IllegalArgumentException("baseQuantum must be > 0");
        this.baseQuantum = baseQuantum;
    }

    @Override
    public void onArrive(PCB pcb) {
        if (pcb == null) return;
        pcb.setState(PCB.State.READY);
        enqueueAtLevel(pcb, 0); // 新到达进Q1
    }

    @Override
    public void onBlock(PCB pcb) {
        if (pcb == null) return;
        pcb.setState(PCB.State.WAIT);
        removeFromAllQueues(pcb);
        // 阻塞后 levelMap 保留也行；这里保留，唤醒时会重置到Q1
    }

    @Override
    public void onWake(PCB pcb) {
        if (pcb == null) return;
        pcb.setState(PCB.State.READY);
        enqueueAtLevel(pcb, 0); // 唤醒回到Q1（常见MLFQ规则）
    }

    @Override
    public void onFinish(PCB pcb) {
        if (pcb == null) return;
        pcb.setState(PCB.State.FINISH);
        removeFromAllQueues(pcb);
        levelMap.remove(pcb);
    }

    /**
     * Engine 在“一个时间片执行完毕后”调用 pickNext(now, running)。
     * 我们在这里把 running 按规则降级并重新入队，然后选下一位。
     */
    @Override
    public PCB pickNext(int now, PCB running) {
        // 1) 处理刚刚运行完时间片的进程：未完成则降级入队（轮转）
        if (running != null
                && running.getState() == PCB.State.EXCUTE
                && !running.isFinished()) {

            int curLevel = levelOf(running);
            int nextLevel = Math.min(2, curLevel + 1); // Q1->Q2->Q3，Q3留在Q3

            running.setState(PCB.State.READY);
            enqueueAtLevel(running, nextLevel);
        }

        // 2) 从高到低选队首
        PCB next = pollNextReady();
        if (next == null) return null;

        next.setState(PCB.State.EXCUTE);
        if (next.getStartTime() == null) next.setStartTime(now);
        return next;
    }

    @Override
    public List<List<PCB>> readyQueuesView() {
        // 返回快照（只读）
        List<PCB> q1 = List.copyOf(queues[0]);
        List<PCB> q2 = List.copyOf(queues[1]);
        List<PCB> q3 = List.copyOf(queues[2]);
        return List.of(q1, q2, q3);
    }

    // -------------------- 供 Engine / UI 使用的辅助方法（不影响接口） --------------------

    /** 当前 pcb 所在队列级别（默认Q1） */
    public int levelOf(PCB pcb) {
        return levelMap.getOrDefault(pcb, 0);
    }

    /** 获取某级队列的时间片：Q1=base, Q2=2*base, Q3=4*base */
    public int getQuantumForLevel(int level) {
        return switch (level) {
            case 0 -> baseQuantum;
            case 1 -> baseQuantum * 2;
            default -> baseQuantum * 4;
        };
    }

    /** 获取某进程当前应使用的时间片 */
    public int getQuantumFor(PCB pcb) {
        return getQuantumForLevel(levelOf(pcb));
    }

    /**
     * 是否应该抢占：
     * - 如果 running 在较低队列，而更高队列非空，则应抢占（常见MLFQ规则）
     */
    public boolean shouldPreempt(PCB running) {
        if (running == null) return true;
        int rl = levelOf(running);
        for (int l = 0; l < rl; l++) {
            if (!queues[l].isEmpty()) return true;
        }
        return false;
    }

    // -------------------- 内部工具 --------------------

    private void enqueueAtLevel(PCB pcb, int level) {
        // 防重复
        removeFromAllQueues(pcb);
        levelMap.put(pcb, level);
        queues[level].addLast(pcb);
    }

    private PCB pollNextReady() {
        for (int i = 0; i < 3; i++) {
            PCB p = queues[i].pollFirst();
            if (p != null) return p;
        }
        return null;
    }

    private void removeFromAllQueues(PCB pcb) {
        for (int i = 0; i < 3; i++) {
            queues[i].remove(pcb);
        }
    }
}
