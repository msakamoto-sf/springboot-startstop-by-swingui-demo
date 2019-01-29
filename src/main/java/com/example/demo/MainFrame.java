package com.example.demo;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private ConfigurableApplicationContext springAppContext;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainFrame frame = new MainFrame(args);
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public MainFrame(final String[] args) {
        setTitle("SpringBoot and Launch4j demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 450, 300);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu menuMain = new JMenu("start/stop spring boot webapp");
        menuBar.add(menuMain);

        JMenuItem menuItemStartSpringBootWebapp = new JMenuItem("start spring boot webapp");
        menuMain.add(menuItemStartSpringBootWebapp);

        JMenuItem menuItemOpenInTheBrowser = new JMenuItem("open in the browser");
        menuItemOpenInTheBrowser.setEnabled(false);
        menuMain.add(menuItemOpenInTheBrowser);

        JMenuItem menuItemStopSpringBootWebapp = new JMenuItem("stop spring boot webapp");
        menuItemStopSpringBootWebapp.setEnabled(false);
        menuMain.add(menuItemStopSpringBootWebapp);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        JTextArea taLog = new JTextArea();
        taLog.setEditable(false);
        scrollPane.setViewportView(taLog);

        LogbackSwingTextareaAppender.addToRootLogger(taLog);

        HttpPortInitializedListener httpPortInitializedListener = new HttpPortInitializedListener();
        StaticGlobalRefs.setHttpPortInitializedListener(httpPortInitializedListener);
        menuItemStartSpringBootWebapp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Logger log = LoggerFactory.getLogger(this.getClass());
                log.info("spring boot starting..."); // これは JTextArea に反映される。
                springAppContext = SpringApplication.run(MainFrame.class, args);
                menuItemStartSpringBootWebapp.setEnabled(false);
                menuItemOpenInTheBrowser.setEnabled(true);
                menuItemStopSpringBootWebapp.setEnabled(true);
                log.info("spring boot started."); // これが反映されない。SpringBootの起動中に、何かlogbackに仕掛けが入るから？
            }
        });
        menuItemOpenInTheBrowser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int httpPort = httpPortInitializedListener.getHttpPort();
                System.err.println("listener accepted http-port = " + httpPort);
                if (httpPort > 1) {
                    final Desktop desktop = Desktop.getDesktop();
                    try {
                        final URI uri = new URI("http://localhost:" + httpPort + "/");
                        desktop.browse(uri);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    System.err.println("http-port is 0, skip opening browser.");
                }
            }
        });
        menuItemStopSpringBootWebapp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Logger log = LoggerFactory.getLogger(this.getClass());
                log.info("spring boot stopping..."); // これも反映されない。謎。
                springAppContext.close();
                menuItemStartSpringBootWebapp.setEnabled(true);
                menuItemOpenInTheBrowser.setEnabled(false);
                menuItemStopSpringBootWebapp.setEnabled(false);
                log.info("spring boot stopped"); // これも反映されない。謎。
            }
        });
    }
}
