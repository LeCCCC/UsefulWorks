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

        List<PCB> processes = List.of(
                new PCB("P1",10,0,6),
                new PCB("P2",5,1,4),
                new PCB("P3",8,2,5)
        );

        engine.load(processes,new PriorityRR(),2,2);

        SwingUtilities.invokeLater(() -> {

            MainUI ui = new MainUI(engine);
            ui.setVisible(true);

        });

    }

}
