import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.tree.*;

class gui extends JPanel implements ActionListener {

  JTextField jtf;
  JTextArea jta;
  JTree tree;
  JButton refresh;
  JTable jtb;
  JScrollPane jsp;
  JScrollPane jspTable;

  String currDirectory = null;

  final String[] colHeads = { "File Name", "SIZE" };
  String[][] data = { { "", "", "" } };
  ExecutorService executorService = Executors.newFixedThreadPool(10);

  gui(String path) {
    jtf = new JTextField();
    jta = new JTextArea(5, 30);
    refresh = new JButton("Refresh");

    File temp = new File(path);
    DefaultMutableTreeNode top = createTree(temp);

    tree = new JTree(top);

    jsp = new JScrollPane(tree);

    final String[] colHeads = { "File Name", "SIZE" };
    String[][] data = { { "", "", "" } };
    jtb = new JTable(data, colHeads);
    jspTable = new JScrollPane(jtb);

    setLayout(new BorderLayout());
    add(jtf, BorderLayout.NORTH);
    add(jsp, BorderLayout.WEST);
    add(jspTable, BorderLayout.CENTER);
    add(refresh, BorderLayout.SOUTH);

    tree.addMouseListener(
      new MouseAdapter() {
        public void mouseClicked(MouseEvent me) {
          doMouseClicked(me);
        }
      }
    );
    jtf.addActionListener(this);
    refresh.addActionListener(this);
  }

  public void actionPerformed(ActionEvent ev) {
    File temp = new File(jtf.getText());
    DefaultMutableTreeNode newtop = createTree(temp);
    if (newtop != null) {
      DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
      DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();

      root.removeAllChildren();

      root.add(newtop);

      treeModel.reload();
    }
  }

  DefaultMutableTreeNode createTree(File temp) {
    DefaultMutableTreeNode top = new DefaultMutableTreeNode(temp.getPath());
    if (!(temp.exists() && temp.isDirectory())) return top;

    fillTree(top, temp.getPath());

    return top;
  }

  void fillTree(final DefaultMutableTreeNode root, String filename) {
    File temp = new File(filename);

    if (!(temp.exists() && temp.isDirectory())) return;
    File[] filelist = temp.listFiles();

    if (filelist == null) return;

    for (final File file : filelist) {
      if (file.isDirectory()) {
        // Submit a task to the thread pool to cache the subdirectory
        executorService.submit(() -> {
          DefaultMutableTreeNode tempDmtn = new DefaultMutableTreeNode(
            file.getName()
          );
          fillTree(tempDmtn, file.getAbsolutePath());
          SwingUtilities.invokeLater(() -> root.add(tempDmtn)); // Update the tree model on the Event Dispatch Thread
        });
      } else {
        // Cache file information (on the Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
          DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(
            file.getName()
          );
          root.add(fileNode);
        });
      }
    }
  }

  void doMouseClicked(MouseEvent me) {
    TreePath tp = tree.getPathForLocation(me.getX(), me.getY());
    if (tp == null) return;

    String s = tp.toString();
    s = s.replace("[", "");
    s = s.replace("]", "");
    s = s.replace(", ", "\\");

    File file = new File(s);
    if (file.isFile()) {
      openFile(file);
    }

    showFiles(s);
  }

  void openFile(File file) {
    try {
      Desktop.getDesktop().open(file);
    } catch (IOException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(
        this,
        "Error opening the file: " + e.getMessage(),
        "File Open Error",
        JOptionPane.ERROR_MESSAGE
      );
    }
  }

  void showFiles(String filename) {
    File temp = new File(filename);
    data = new String[][] { { "", "" } };
    remove(jspTable);
    jtb = new JTable(data, colHeads);
    jspTable = new JScrollPane(jtb);
    setVisible(false);
    add(jspTable, BorderLayout.CENTER);
    setVisible(true);

    if (!temp.exists()) return;
    if (!temp.isDirectory()) return;

    File[] filelist = temp.listFiles();
    int fileCounter = 0;
    data = new String[filelist.length][2];

    for (int i = 0; i < filelist.length; i++) {
      if (filelist[i].isDirectory()) continue;
      data[fileCounter][0] = new String(filelist[i].getName());

      // Use StringBuilder to construct the size string
      StringBuilder sizeBuilder = new StringBuilder();

      long fileSize = filelist[i].length();
      if (fileSize >= 1000) {
        sizeBuilder.append(fileSize / 1000).append(" kilobytes");
      } else if (fileSize >= 1000000) {
        sizeBuilder.append(fileSize / 1000000).append(" megabytes");
      } else if (fileSize >= 1000000000) {
        sizeBuilder.append(fileSize / 1000000000).append(" gigabytes");
      } else if (fileSize >= 1000000000000L) {
        sizeBuilder.append(fileSize / 1000000000000L).append(" terabytes");
      } else if (fileSize >= 1000000000000000L) {
        sizeBuilder.append(fileSize / 1000000000000000L).append(" petabytes");
      } else {
        // Add "0." to represent less than 1 kilobyte
        sizeBuilder.append("0.").append(fileSize / 100).append(" kilobytes");
      }

      data[fileCounter][1] = sizeBuilder.toString();
      fileCounter++;
    }

    String dataTemp[][] = new String[fileCounter][2];
    for (int k = 0; k < fileCounter; k++) dataTemp[k] = data[k];
    data = dataTemp;

    remove(jspTable);
    jtb = new JTable(data, colHeads);
    jspTable = new JScrollPane(jtb);
    setVisible(false);
    add(jspTable, BorderLayout.CENTER);
    setVisible(true);
  }
}

class ExplorerTest extends JFrame {

  ExecutorService executorService = Executors.newFixedThreadPool(10);

  ExplorerTest(String path) {
    super("File Explorer ");
    Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
    double double_width = (int) size.getWidth() * .7;
    double double_height = (int) size.getHeight() * .8;
    int width = (int) double_width;
    int height = (int) double_height;

    add(new gui(path), "Center");
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setSize(width, height);
    setVisible(true);
    Runtime
      .getRuntime()
      .addShutdownHook(
        new Thread(() -> {
          executorService.shutdown();
        })
      );
  }

  public static void main(String[] args) {
    new ExplorerTest("C:\\Users");
  }
}
