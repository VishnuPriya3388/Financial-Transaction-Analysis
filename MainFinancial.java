import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class MainFinancial {
    private JFrame frame;
    private JTextArea outputArea;
    private DrawPanel drawPanel;
    private TruePackedMemoryGraph pmg;
    private Set<Integer> highlightedNodes = new HashSet<>();

    public static void main(String[] args) {
        TruePackedMemoryGraph pmg = new TruePackedMemoryGraph(5000);

        // Load dataset
        String filename = "transactions.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length < 3) continue;
                int sender = Integer.parseInt(parts[0]);
                int receiver = Integer.parseInt(parts[1]);
                double amount = Double.parseDouble(parts[2]);
                pmg.insertNode(sender, 10);
                pmg.insertNode(receiver, 10);
                pmg.addEdge(sender, receiver, amount);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to load dataset: " + ex.getMessage());
            return;
        }

        // Launch GUI
        SwingUtilities.invokeLater(() -> new MainFinancial(pmg).frame.setVisible(true));
    }

    public MainFinancial(TruePackedMemoryGraph pmg) {
        this.pmg = pmg;
        buildUI();
        refreshReport();
    }

    private void buildUI() {
        frame = new JFrame("PMG Financial Transaction Network");
        frame.setSize(1200, 700);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel startLabel = new JLabel("Start Node:");
        JTextField startField = new JTextField(6);
        JButton bfsButton = new JButton("Run BFS");
        JButton refreshButton = new JButton("Refresh Summary");
        JButton copyButton = new JButton("Copy Output");
        JButton saveButton = new JButton("Save Output");

        JLabel rangeLabel = new JLabel("Node Range:");
        JTextField minField = new JTextField(4);
        JTextField maxField = new JTextField(4);
        JButton rangeButton = new JButton("Apply Range");

        controlPanel.add(startLabel);
        controlPanel.add(startField);
        controlPanel.add(bfsButton);
        controlPanel.add(refreshButton);
        controlPanel.add(copyButton);
        controlPanel.add(saveButton);
        controlPanel.add(rangeLabel);
        controlPanel.add(minField);
        controlPanel.add(maxField);
        controlPanel.add(rangeButton);

        // Output area
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setPreferredSize(new Dimension(400, 650));

        // Draw panel
        drawPanel = new DrawPanel();
        drawPanel.setPreferredSize(new Dimension(750, 650));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, drawPanel);
        splitPane.setDividerLocation(400);

        frame.add(controlPanel, BorderLayout.NORTH);
        frame.add(splitPane, BorderLayout.CENTER);

        // Button actions
        refreshButton.addActionListener(e -> refreshReport());

        bfsButton.addActionListener(e -> {
            try {
                int start = Integer.parseInt(startField.getText());
                runBFS(start);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Enter valid start node!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        copyButton.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(outputArea.getText()), null);
            JOptionPane.showMessageDialog(frame, "Output copied to clipboard!");
        });

        saveButton.addActionListener(e -> {
            try {
                JFileChooser chooser = new JFileChooser();
                if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    try (PrintWriter pw = new PrintWriter(file)) {
                        pw.write(outputArea.getText());
                    }
                    JOptionPane.showMessageDialog(frame, "Output saved to " + file.getAbsolutePath());
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Failed to save: " + ex.getMessage());
            }
        });

        rangeButton.addActionListener(e -> {
            try {
                int min = Integer.parseInt(minField.getText());
                int max = Integer.parseInt(maxField.getText());
                if (min > max) {
                    JOptionPane.showMessageDialog(frame, "Min cannot be greater than Max", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                applyRange(min, max);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Enter valid numbers!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void refreshReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Financial Transaction Network ===\n");
        sb.append("Accounts (Nodes): ").append(pmg.getNodeCount()).append("\n");
        sb.append("Transactions (Edges): ").append(pmg.getEdgeCount()).append("\n");
        sb.append(String.format("Network Density: %.3f\n", pmg.getDensity()));

        sb.append("\n--- Top 10 High-Volume Accounts ---\n");
        pmg.computeOutgoingAmounts().entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .forEach(e -> sb.append("Account ").append(e.getKey())
                        .append(" → Sent ₹").append(String.format("%.2f", e.getValue())).append("\n"));

        outputArea.setText(sb.toString());
        highlightedNodes.clear();
        drawPanel.repaint();
    }

    private void runBFS(int start) {
        highlightedNodes = pmg.bfs(start);
        StringBuilder sb = new StringBuilder(outputArea.getText());
        sb.append("\n--- BFS from Account ").append(start).append(" ---\n");
        sb.append("Reachable Accounts: ").append(highlightedNodes.size() - 1).append("\n");
        sb.append("Accounts: ").append(highlightedNodes).append("\n");
        outputArea.setText(sb.toString());
        drawPanel.repaint();
    }

    private void applyRange(int min, int max) {
        Set<Integer> filtered = new HashSet<>();
        for (int node : pmg.getNodes()) {
            if (node >= min && node <= max) filtered.add(node);
        }
        highlightedNodes = filtered;
        outputArea.append("\n--- Showing nodes in range " + min + " to " + max + " ---\n");
        drawPanel.repaint();
    }

    class DrawPanel extends JPanel {
        private Map<Integer, Point> positions = new HashMap<>();

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth() - 50, h = getHeight() - 50;
            Random rand = new Random(42);

            // Assign positions if empty
            if (positions.isEmpty()) {
                for (Integer acc : pmg.getNodes()) {
                    int x = 50 + rand.nextInt(w);
                    int y = 50 + rand.nextInt(h);
                    positions.put(acc, new Point(x, y));
                }
            }

            // Draw edges
            g.setColor(Color.LIGHT_GRAY);
            for (Integer from : pmg.getNodes()) {
                Point p1 = positions.get(from);
                for (TruePackedMemoryGraph.Edge e : pmg.getEdges(from)) {
                    Point p2 = positions.get(e.to);
                    if (p1 != null && p2 != null) g.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }

            // Draw nodes
            for (Map.Entry<Integer, Point> entry : positions.entrySet()) {
                int nodeId = entry.getKey();
                if (!highlightedNodes.isEmpty() && !highlightedNodes.contains(nodeId)) continue;
                Point p = entry.getValue();
                g.setColor(highlightedNodes.contains(nodeId) ? Color.ORANGE : Color.CYAN);
                g.fillOval(p.x - 10, p.y - 10, 20, 20);
                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(nodeId), p.x - 7, p.y - 15);
            }
        }
    }
}
