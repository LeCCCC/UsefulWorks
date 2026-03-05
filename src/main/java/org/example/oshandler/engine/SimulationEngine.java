package org.example.oshandler.engine;

import org.example.oshandler.core.PCB;
import org.example.oshandler.scheduler.Scheduler;

import java.util.List;

public interface SimulationEngine {
    void load(List<PCB> processes, Scheduler scheduler, int quantum, int decay);
    Snapshot step();       // 单步推进一次（一次调度/一次时间片）
    void block(String name);
    void wake(String name);
    void kill(String name);
    boolean isAllFinished();
}
