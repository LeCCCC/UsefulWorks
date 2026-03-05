package org.example.oshandler.ui;

import org.example.oshandler.core.PCB;
import org.example.oshandler.engine.Snapshot;
import org.example.oshandler.engine.myEngine;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MainUI extends JFrame {

    private myEngine engine;

    private JLabel timeLabel = new JLabel("时间: 0");
    private JLabel runningLabel = new JLabel("运行进程: -");

    private JTextArea queueArea = new JTextArea();
    private JTextArea logArea = new JTextArea();

    private JTable processTable;
    private DefaultTableModel tableModel;

    private Timer timer;

    public MainUI(myEngine engine) {
        this.engine = engine;

        setTitle("操作系统调度模拟");
        setSize(900,600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        initUI();
    }

    private void initUI() {

        setLayout(new BorderLayout());

        // 顶部信息
        JPanel topPanel = new JPanel();
        topPanel.add(timeLabel);
        topPanel.add(runningLabel);

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
        String[] columns = {"进程","状态","优先级","剩余时间","开始时间","结束时间"};
        tableModel = new DefaultTableModel(columns,0);
        processTable = new JTable(tableModel);

        JScrollPane tablePane = new JScrollPane(processTable);
        tablePane.setBorder(BorderFactory.createTitledBorder("进程"));

        centerSplit.setRightComponent(tablePane);

        add(centerSplit,BorderLayout.CENTER);

        // 底部按钮
        JPanel bottomPanel = new JPanel();

        JButton stepBtn = new JButton("下一步");
        JButton autoBtn = new JButton("自动运行");
        JButton stopBtn = new JButton("停止");

        bottomPanel.add(stepBtn);
        bottomPanel.add(autoBtn);
        bottomPanel.add(stopBtn);

        add(bottomPanel,BorderLayout.SOUTH);

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
                }else{
                    timer.stop();
                    JOptionPane.showMessageDialog(this,"所有进程已完成！");
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
    }

    private void updateUI(Snapshot s){

        timeLabel.setText("时间: " + s.time);
        runningLabel.setText("运行进程: " + s.running);

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
        logArea.append("t=" + s.time + " " + s.log + "\n");

        // 更新表格
        tableModel.setRowCount(0);

        for(PCB p:s.all){

            Object[] row = {
                    p.getName(),
                    p.getState(),
                    p.getPriority(),
                    p.remainingTime(),
                    p.getStartTime(),
                    p.getFinishTime()
            };

            tableModel.addRow(row);
        }
    }
}