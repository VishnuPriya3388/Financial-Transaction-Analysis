import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class PMGAppFinancial {
    private JFrame frame;
    private JTextArea outputArea;
    private DrawPanel drawPanel;
    private TruePackedMemoryGraph pmg;
    private Set<Integer> highlightedNodes = new HashSet<>();

    public static void show(TruePackedMemoryGraph pmg) {
        SwingUtilities.invokeLater(() -> new PMGAppFinancial(pmg).frame.setVisible(true));
    }

    private PMGAppFinancial(TruePackedMemoryGraph pmg) {
        this.pmg = pmg;
        buildUI();
        refreshReport();
    }

    private void buildUI() {
        frame = new JFrame("PMG Financial Transaction Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 700);
        frame.setLayout(new BorderLayout());

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("Start Account:");
        JTextField startField = new JTextField(6);
        JButton bfsButton = new JButton("Run BFS");
        JButton refreshButton = new JButton("Refresh Summary");
        JButton copyButton = new JButton("Copy Output");
        JButton saveButton = new JButton("Save Output");

        controlPanel.add(label);
        controlPanel.add(startField);
        controlPanel.add(bfsButton);
        controlPanel.add(refreshButton);
        controlPanel.add(copyButton);
        controlPanel.add(saveButton);

        // Output area
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setPreferredSize(new Dimension(400, 700));

        // Draw panel
        drawPanel = new DrawPanel();
        drawPanel.setPreferredSize(new Dimension(750, 700));

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
                JOptionPane.showMessageDialog(frame, "Enter a valid account number!", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        copyButton.addActionListener(e -> {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
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
    }

    private void refreshReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Financial Transaction Network ===\n");
        sb.append("Accounts: ").append(pmg.getNodeCount()).append("\n");
        sb.append("Transactions: ").append(pmg.getEdgeCount()).append("\n");
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
                Point p = entry.getValue();
                if (highlightedNodes.contains(entry.getKey())) g.setColor(Color.ORANGE);
                else g.setColor(Color.CYAN);
                g.fillOval(p.x - 10, p.y - 10, 20, 20);
                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(entry.getKey()), p.x - 7, p.y - 15);
            }
        }
    }
}
