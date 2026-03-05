package org.example.oshandler.engine;

import org.example.oshandler.core.PCB;

import java.util.List;

public class Snapshot {
    public int time;
    public String running;                 // 当前执行进程名（可空）
    public List<PCB> all;                  // 所有进程的当前信息
    public List<List<PCB>> readyQueues;    // 1个或3个队列
    public String log;                     // 本步发生了什么
}
