package org.example.oshandler.scheduler;

import org.example.oshandler.core.PCB;

import java.util.List;

public interface Scheduler {
void onArrive(PCB pcb);
void onBlock(PCB pcb);
void onWake(PCB pcb);
void onFinish(PCB pcb);

PCB pickNext(int now,PCB running);
List<List<PCB>> readyQueuesView();

}
