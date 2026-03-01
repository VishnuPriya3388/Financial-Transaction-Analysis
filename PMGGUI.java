import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;

/**
 * PMGGUI - Visualization and Interaction for Packed Memory Graph (Financial Transactions)
 */
public class PMGGUI {
    private JFrame frame;
    private JTextArea outputArea;
    private DrawPanel drawPanel;
    private TruePackedMemoryGraph pmg;

    public PMGGUI(TruePackedMemoryGraph pmg) {
        this.pmg = pmg;
        buildUI();
        buildInitialReport();
    }

    public static void showOutput(String text) {
        JFrame frame = new JFrame("PMG Output");
        JTextArea area = new JTextArea(text, 20, 50);
        area.setEditable(false);
        JScrollPane scroll = new JScrollPane(area);
        frame.add(scroll);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void buildUI() {
        frame = new JFrame("PMG Financial Transactions");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());

        // Controls panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JLabel label = new JLabel("Start Account:");
        JTextField startField = new JTextField(8);
        JButton bfsButton = new JButton("Run BFS");
        JButton refreshButton = new JButton("Refresh");
        JButton copyButton = new JButton("Copy Output");

        controlPanel.add(label);
        controlPanel.add(startField);
        controlPanel.add(bfsButton);
        controlPanel.add(refreshButton);
        controlPanel.add(copyButton);

        // Output area
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Transaction Report"));

        // Draw panel
        drawPanel = new DrawPanel();
        drawPanel.setBorder(BorderFactory.createTitledBorder("Transaction Visualization"));
        drawPanel.setPreferredSize(new Dimension(400, 600));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, drawPanel);
        splitPane.setDividerLocation(600);

        frame.add(controlPanel, BorderLayout.NORTH);
        frame.add(splitPane, BorderLayout.CENTER);

        // Button actions
        refreshButton.addActionListener(e -> buildInitialReport());

        bfsButton.addActionListener(e -> {
            try {
                int start = Integer.parseInt(startField.getText());
                performBFS(start);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Enter a valid account number!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        copyButton.addActionListener(e -> copyToClipboard(outputArea.getText()));
    }

    private void buildInitialReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Financial Transaction Network Summary ===\n");
        sb.append("Total Accounts: ").append(pmg.getNodeCount()).append("\n");
        sb.append("Total Transactions: ").append(pmg.getEdgeCount()).append("\n");
        sb.append(String.format("Network Density: %.4f\n", pmg.getDensity()));

        sb.append("\n--- Top 10 High-Volume Accounts ---\n");
        Map<Integer, Double> sums = pmg.computeOutgoingAmounts();
        sums.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .forEach(e -> sb.append("Account ").append(e.getKey())
                        .append(" → Sent ₹").append(String.format("%.2f", e.getValue())).append("\n"));

        outputArea.setText(sb.toString());
        drawPanel.repaint();
    }

    private void performBFS(int start) {
        Set<Integer> visited = pmg.bfs(start);
        StringBuilder sb = new StringBuilder(outputArea.getText());
        sb.append("\n--- BFS Money Flow from Account ").append(start).append(" ---\n");
        sb.append("Reachable Accounts: ").append(visited.size() - 1).append("\n");
        sb.append("Accounts: ").append(visited.toString()).append("\n");
        outputArea.setText(sb.toString());

        drawPanel.highlight(visited);
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);
        JOptionPane.showMessageDialog(frame, "Output copied to clipboard!");
    }

    // === Inner Draw Panel ===
    class DrawPanel extends JPanel {
        private Set<Integer> highlighted = new HashSet<>();

        void highlight(Set<Integer> visited) {
            highlighted = visited;
            repaint();
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.DARK_GRAY);
            g.drawString("Financial Transaction Network", 50, 20);

            Random rand = new Random(0);
            List<Integer> nodes = new ArrayList<>(pmg.getNodes());
            Map<Integer, Point> positions = new HashMap<>();

            for (int i = 0; i < nodes.size(); i++) {
                int x = 50 + rand.nextInt(350);
                int y = 50 + rand.nextInt(500);
                positions.put(nodes.get(i), new Point(x, y));
            }

            // Draw edges
            for (int from : positions.keySet()) {
                for (TruePackedMemoryGraph.Edge e : pmg.getEdges(from)) {
                    if (positions.containsKey(e.to)) {
                        Point a = positions.get(from);
                        Point b = positions.get(e.to);
                        g.setColor(Color.LIGHT_GRAY);
                        g.drawLine(a.x, a.y, b.x, b.y);
                    }
                }
            }

            // Draw nodes
            for (Map.Entry<Integer, Point> entry : positions.entrySet()) {
                int id = entry.getKey();
                Point p = entry.getValue();
                g.setColor(highlighted.contains(id) ? Color.ORANGE : Color.CYAN);
                g.fillOval(p.x - 10, p.y - 10, 20, 20);
                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(id), p.x - 5, p.y - 15);
            }
        }
    }

    // === Launch GUI ===
    public static void show(TruePackedMemoryGraph pmg) {
        SwingUtilities.invokeLater(() -> {
            PMGGUI app = new PMGGUI(pmg);
            app.frame.setVisible(true);
        });
    }
}
