package org.example.oshandler.ui;

import org.example.oshandler.core.PCB;
import org.example.oshandler.engine.Snapshot;
import org.example.oshandler.engine.myEngine;
import org.example.oshandler.scheduler.MLFQ3;
import org.example.oshandler.scheduler.PriorityRR;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MainUI extends JFrame {

    private final myEngine engine;
    private final List<PCB> processTemplates;
    private final int quantum;
    private final int decay;
    private boolean useMlfq = false;
    private final JLabel timeLabel = new JLabel("时间: 0");
    private final JLabel runningLabel = new JLabel("运行进程: -");
    private JLabel schedulerLabel = new JLabel("当前算法: 时间片轮转");
    private JLabel avgTurnaroundLabel = new JLabel("平均周转时间: -");
    private final JTextArea queueArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();

    private JTable processTable;
    private DefaultTableModel tableModel;

    private Timer timer;
    private boolean finishedNotified = false;
    private boolean deadlockNotified = false;

    public MainUI(myEngine engine, List<PCB> processTemplates, int quantum, int decay) {
        this.engine = engine;
        this.processTemplates = processTemplates;
        this.quantum = quantum;
        this.decay = decay;
        setTitle("操作系统调度模拟");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        initUI();
        updateUI(engine.currentSnapshot("系统初始化完成"));
    }

    private void initUI() {

        setLayout(new BorderLayout());

        // 顶部信息
        JPanel topPanel = new JPanel();
        topPanel.add(timeLabel);
        topPanel.add(runningLabel);
        topPanel.add(schedulerLabel);
        topPanel.add(avgTurnaroundLabel);
        add(topPanel,BorderLayout.NORTH);

        // 中间区域
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // 左侧 队列 + 日志
        JPanel leftPanel = new JPanel(new GridLayout(2,1));

        queueArea.setBorder(BorderFactory.createTitledBorder("就绪队列"));
        queueArea.setEditable(false);

        logArea.setBorder(BorderFactory.createTitledBorder("调度日志"));
        logArea.setEditable(false);

        leftPanel.add(new JScrollPane(queueArea));
        leftPanel.add(new JScrollPane(logArea));

        centerSplit.setLeftComponent(leftPanel);

        // 右侧 进程表
        String[] columns = {"进程","状态","优先级","剩余时间","开始时间","结束时间","周转时间"};
        tableModel = new DefaultTableModel(columns,0);
        processTable = new JTable(tableModel);

        JScrollPane tablePane = new JScrollPane(processTable);
        tablePane.setBorder(BorderFactory.createTitledBorder("所有进程"));

        centerSplit.setRightComponent(tablePane);

        add(centerSplit,BorderLayout.CENTER);

        // 底部按钮
        JPanel bottomPanel = new JPanel();

        JButton addBtn = new JButton("添加线程");
        JButton stepBtn = new JButton("下一步");
        JButton autoBtn = new JButton("自动运行");
        JButton stopBtn = new JButton("停止");
        JButton switchBtn = new JButton("切换算法");

        bottomPanel.add(addBtn);
        bottomPanel.add(stepBtn);
        bottomPanel.add(autoBtn);
        bottomPanel.add(stopBtn);
        bottomPanel.add(switchBtn);

        add(bottomPanel,BorderLayout.SOUTH);
        addBtn.addActionListener(e -> openAddProcessDialog());
        // Step按钮
        stepBtn.addActionListener(e -> {

            if(timer != null && timer.isRunning()){
                return;
            }

            updateUI(engine.step());
        });
        // 自动运行
        autoBtn.addActionListener(e -> {

            if(timer != null && timer.isRunning()){
                return;
            }

            timer = new Timer(800, ev -> {

                if(!engine.isAllFinished()){
                    updateUI(engine.step());
                    if (engine.isDeadlocked()) {
                        timer.stop();
                        showDeadlockMessage();
                    }
                }else{
                    timer.stop();
                    showFinishedMessage();
                }

            });

            timer.start();
        });

        // 停止
        stopBtn.addActionListener(e -> {
            if(timer!=null){
                timer.stop();
            }
        });

        switchBtn.addActionListener(e -> {
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            useMlfq = !useMlfq;
            resetEngineByCurrentScheduler();
        });

        schedulerLabel.setText("当前算法: " + engine.getSchedulerName());
    }

    private void resetEngineByCurrentScheduler() {
        List<PCB> copied = new ArrayList<>();
        for (PCB p : processTemplates) {
            copied.add(new PCB(p.getName(), p.getPriority(), p.getArrivalTime(), p.getTotalTime()));
        }

        if (useMlfq) {
            engine.load(copied, new MLFQ3(quantum), quantum, decay);
        } else {
            engine.load(copied, new PriorityRR(), quantum, decay);
        }

        finishedNotified = false;
        deadlockNotified = false;
        logArea.setText("");
        updateUI(engine.currentSnapshot("系统初始化完成"));
    }


    private void openAddProcessDialog() {

        JTextField nameField = new JTextField();
        JTextField priorityField = new JTextField();
        JTextField arrivalField = new JTextField();
        JTextField totalTimeField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("线程名:"));
        panel.add(nameField);
        panel.add(new JLabel("优先级(数字越小优先级越高):"));
        panel.add(priorityField);
        panel.add(new JLabel("到达时间:"));
        panel.add(arrivalField);
        panel.add(new JLabel("总运行时间:"));
        panel.add(totalTimeField);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "添加线程",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("线程名不能为空");
            }

            int priority = Integer.parseInt(priorityField.getText().trim());
            int arrival = Integer.parseInt(arrivalField.getText().trim());
            int totalTime = Integer.parseInt(totalTimeField.getText().trim());

            if (arrival < 0 || totalTime <= 0) {
                throw new IllegalArgumentException("到达时间需>=0，总运行时间需>0");
            }

            PCB process = new PCB(name, priority, arrival, totalTime);
            engine.addProcess(process);
            updateUI(engine.currentSnapshot("新增线程 " + name));
            JOptionPane.showMessageDialog(this, "线程添加成功: " + name);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "优先级、到达时间、总运行时间必须是整数", "输入错误", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "输入错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void updateUI(Snapshot s){

        timeLabel.setText("时间: " + s.time);
        runningLabel.setText("运行进程: " + s.running);
        schedulerLabel.setText("当前算法: " + engine.getSchedulerName());
        if (engine.isAllFinished()) {
            avgTurnaroundLabel.setText(String.format("平均周转时间: %.2f", engine.getAverageTurnaround()));
        } else {
            avgTurnaroundLabel.setText("平均周转时间: -");
        }
        // 更新队列
        StringBuilder queueText = new StringBuilder();

        List<List<PCB>> queues = s.readyQueues;

        for(int i=0;i<queues.size();i++){

            queueText.append("Q").append(i+1).append(": ");

            for(PCB p:queues.get(i)){
                queueText.append(p.getName()).append(" ");
            }

            queueText.append("\n");
        }

        queueArea.setText(queueText.toString());

        // 更新日志
        if (s.log != null && !s.log.isBlank()) {
            logArea.append("t=" + s.time + " " + s.log + "\n");
        }

        // 更新表格
        tableModel.setRowCount(0);

        for(PCB p:s.all){

            Object[] row = {
                    p.getName(),
                    p.getState(),
                    p.getPriority(),
                    p.remainingTime(),
                    p.getStartTime(),
                    p.getFinishTime(),
                    p.turnaroundTime()
            };

            tableModel.addRow(row);
        }
        if (engine.isAllFinished()) {
            showFinishedMessage();
        }
        if (engine.isDeadlocked()) {
            showDeadlockMessage();
        }
    }

    private void showFinishedMessage() {
        if (finishedNotified) {
            return;
        }
        finishedNotified = true;
        JOptionPane.showMessageDialog(this,
                String.format("所有进程已完成！\n算法: %s\n平均周转时间: %.2f",
                        engine.getSchedulerName(),
                        engine.getAverageTurnaround()));
    }

    private void showDeadlockMessage() {
        if (deadlockNotified) {
            return;
        }
        deadlockNotified = true;
        JOptionPane.showMessageDialog(this,
                String.format("系统处于死锁状态，已停止运行。\n算法: %s",
                        engine.getSchedulerName()));
    }
}
