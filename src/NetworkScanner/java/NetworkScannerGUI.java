package NetworkScanner.java;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkScannerGUI extends JFrame {

	private static final long serialVersionUID = 4132782507679118489L;
	private DefaultTableModel model;
    private JButton refreshButton;
    private JButton scanButton;
    private JButton stopButton;
    private JButton exportButton;
    private JLabel statusLabel;
	public NetworkScannerGUI() {
        super("Network Scanner - Software by Jayden");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        model = new DefaultTableModel();
        model.addColumn("IP Address");
        model.addColumn("Computer Name");
        model.addColumn("Operating System");

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshButton.setEnabled(false);
                scanButton.setEnabled(false);
                statusLabel.setText("Scanning network, please wait...");
                model.setRowCount(0);
                new Thread(() -> {
					scanNetwork();
				}).start();
            }
        });

        scanButton = new JButton("Scan");
        scanButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scanButton.setEnabled(false);
                statusLabel.setText("Scanning network, please wait...");
                model.setRowCount(0);
                new Thread(() -> {
					scanNetwork();
				}).start();
            }
        });

        stopButton = new JButton("Stop Scanning");
        stopButton.setVisible(false);
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusLabel.setText("Scanning stopped by user");
                JOptionPane.showMessageDialog(NetworkScannerGUI.this, "Scanning stopped by user");
            }
        });

        exportButton = new JButton("Export");
        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exportTableContents();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);
        buttonPanel.add(scanButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(exportButton);

        statusLabel = new JLabel("Ready");

        JPanel statusPanel = new JPanel();
        statusPanel.add(statusLabel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(statusPanel, BorderLayout.NORTH);

        setContentPane(panel);
        setSize(600, 400);
        setVisible(true);
    }

private void scanNetwork() {
    model.setRowCount(0);
    scanButton.setEnabled(false);
    refreshButton.setEnabled(false);
    exportButton.setEnabled(false);
    stopButton.setVisible(true);
    statusLabel.setText("Scanning network...");

    int timeout = 3000; // milliseconds
    int numProcessors = Runtime.getRuntime().availableProcessors();
    ExecutorService threadPool = Executors.newFixedThreadPool(numProcessors);
    Map<InetAddress, String[]> cache = new HashMap<>();

    final JProgressBar progressBar = new JProgressBar(0, 255);
    progressBar.setStringPainted(true);
    progressBar.setString("Scanning...");
    add(progressBar, BorderLayout.SOUTH);
    revalidate();

    for (int i = 1; i <= 255; i++) {
        final int j = i;
        threadPool.submit(() -> {
            try {
                String host = "192.168.1." + j;
                InetAddress address = InetAddress.getByName(host);

                if (!address.isReachable(timeout)) {
                    return;
                }

                String[] cachedData = cache.get(address);
                String computerName = null;
                String operatingSystem = null;

                if (cachedData != null) {
                    computerName = cachedData[0];
                    operatingSystem = cachedData[1];
                } else {
                    computerName = address.getCanonicalHostName();
                    operatingSystem = getOperatingSystem1(address);
                    cache.put(address, new String[] { computerName, operatingSystem });
                }

                String[] rowData = new String[] { host, computerName, operatingSystem };
                model.addRow(rowData);
                progressBar.setValue(j);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    threadPool.shutdown();
    try {
        threadPool.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }

    remove(progressBar);
    refreshButton.setEnabled(true);
    scanButton.setEnabled(true);
    stopButton.setVisible(false);
    exportButton.setEnabled(true);
    statusLabel.setText("Scanning completed");
    JOptionPane.showMessageDialog(this, "Scanning completed");
}

private String getOperatingSystem1(InetAddress address) throws IOException {
    String os = "Null";

    String hostAddress = address.getHostAddress();
    String[] command = {"ping", "-n", "1", "-w", "1000", hostAddress};
    Process p = Runtime.getRuntime().exec(command);

    try (Scanner s = new Scanner(p.getInputStream())) {
        while (s.hasNextLine()) {
            String line = s.nextLine().toLowerCase();
            if (line.contains("ttl=")) {
                if (line.contains("windows")) {
                    os = "Windows";
                } else if (line.contains("linux")) {
                    os = "Linux";
                } else if (line.contains("mac")) {
                    os = "Mac OS";
                }
                break;
            }
        }
    }

    return os;
}

    private void exportTableContents() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                for (int i = 0; i < model.getRowCount(); i++) {
                    String ipAddress = (String) model.getValueAt(i, 0);
                    String computerName = (String) model.getValueAt(i, 1);
                    String operatingSystem = (String) model.getValueAt(i, 2);

                    writer.write(ipAddress + "\t" + computerName + "\t" + operatingSystem + "\n");
                }

                JOptionPane.showMessageDialog(this, "Table contents exported to file: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new NetworkScannerGUI();
    }
}

