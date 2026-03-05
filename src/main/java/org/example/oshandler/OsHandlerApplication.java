package org.example.oshandler;

import org.example.oshandler.core.PCB;
import org.example.oshandler.engine.myEngine;
import org.example.oshandler.scheduler.PriorityRR;
import org.example.oshandler.ui.MainUI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.*;
import java.util.List;

@SpringBootApplication
public class OsHandlerApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(OsHandlerApplication.class, args);

        myEngine engine = new myEngine();
        int quantum=1;
        int decay =2;
        List<PCB> processes = List.of(
                new PCB("P1",10,0,6),
                new PCB("P2",5,1,4),
                new PCB("P3",8,2,5),
                new PCB("P4",1,3,10),
                new PCB("P5",2,4,3),
                new PCB("P6",3,5,2),
                new PCB("P7",4,6,7),
                new PCB("P8",6,7,1),
                new PCB("P9",7,8,4)

        );

        engine.load(processes,new PriorityRR(),quantum,decay);

        SwingUtilities.invokeLater(() -> {

            MainUI ui = new MainUI(engine,processes,quantum,decay);
            ui.setVisible(true);

        });

    }

}
