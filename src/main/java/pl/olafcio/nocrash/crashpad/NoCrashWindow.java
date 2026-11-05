package pl.olafcio.nocrash.crashpad;

import org.jetbrains.annotations.NotNull;
import pl.olafcio.commons.hastebin.BinFile;
import pl.olafcio.commons.hastebin.Hastebin;
import pl.olafcio.nocrash.crashpad.logcensor.LogCensor;
import pl.olafcio.nocrash.crashpad.message.FixType;
import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicPanelUI;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

public final class NoCrashWindow {
    private final Thread thread;
    private final File   logFile;
    private final String logs;

    public NoCrashWindow(File logFile, String logs) {
        this.logs    = new LogCensor(logs).censor();
        this.logFile = logFile;

        this.thread = new Thread(this::show, "NoCrash/GUI");
        this.thread.setDaemon(false);
        this.thread.start();
    }

    private final JLabel status
            = new JLabel("Initializing");

    private final JPanel progress
            = new JPanel();

    private final JPanel messages
            = new JPanel();

    private Font font;

    {
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, readToStream("fonts/Inter-VariableFont_slnt,wght.ttf"))
                       .deriveFont(Font.BOLD, 13F);
        } catch (FontFormatException | IOException e) {
            font = null;
            IO.println("[NoCrashWindow/WARN] Failed to load font");
        }
    }

    public void setState(String state) {
        this.status.setText(state);
        this.progress.setBounds(0, 0, 0, 30);
    }

    public void setProgress(float percentage) {
        this.progress.setBounds(0, 0, (int) (500 * percentage), 30);
    }

    private final ArrayBlockingQueue<Runnable> callback
            = new ArrayBlockingQueue<>(36);

    public void exec(Runnable runnable) {
        if (Thread.currentThread() == thread) {
            runnable.run();
        } else {
            this.callback.add(runnable);
        }
    }

    private final AtomicReference<Runnable> newestTask
            = new AtomicReference<>();

    public void race(Runnable runnable) {
        if (Thread.currentThread() == thread) {
            runnable.run();
        } else {
            synchronized (this) {
                newestTask.set(runnable);

                this.notify();
            }
        }
    }

    private void show() {
        var frame = new JFrame();
        var child = new JPanel();

        child.setBounds(0, 0, 500, 600);
        child.setBackground(new Color(0x333333));
        child.setLayout(null);

        status.setBounds(5, 7, 500 - 5*2, 15);
        status.setOpaque(false);
        status.setFont(font);

        var hsb = new float[3];
        Color.RGBtoHSB(58, 94, 161, hsb);

        progress.setBackground(new Color(0x3A5EA1));
        progress.setUI(new BasicPanelUI() {
            @Override
            public void update(Graphics g, JComponent c) {
                if (c.isOpaque()) {
                    for (int y = 0, height = c.getHeight(); y < height; y++) {
                        float t = (float)y / height;

                        g.setColor(Color.getHSBColor(hsb[0], hsb[1], 0.5F + hsb[2] * t/2));
                        g.fillRect(0, y, c.getWidth(), y+1);
                    }
                }

                paint(g, c);
            }
        });

        messages.setLayout(new VerticalLayout(FlowLayout.LEADING));
        messages.setBackground(new Color(0x3c3c3c));

        var pane = new JScrollPane(messages);

        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setBounds(0, 30, 500, 300 - 30);

        child.add(status);
        child.add(progress);
        child.add(pane);

        frame.add(child);

        child.add(button_openLogs(frame));
        child.add(button_uploadLogs(frame));

        frame.setTitle("Minecraft crashed! | NoCrash");
        frame.setSize(500, 600);
        frame.setLayout(null);
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                IO.println("[NoCrashWindow/INFO] Closing");

                e.getWindow().dispose();
                thread.interrupt();
            }
        });

        var thread = new Thread(this::raceProcessing, "NoCrash/Race");
        thread.setDaemon(true);
        thread.start();

        queueProcessing(frame);
    }

    private synchronized void raceProcessing() {
        while (true) {
            for (int i = 0; i < 2; i++) {
                try {
                    this.wait(80L);
                } catch (InterruptedException e) {
                    IO.println("[NoCrashWindow/INFO] [RaceProcessing] Interrupting");
                    return;
                }
            }

            if (newestTask.get() == null)
                continue;

            callback.add(newestTask.getAndSet(null));
        }
    }

    private void queueProcessing(JFrame frame) {
        while (true) {
            try {
                var fun = callback.take();

                // Swing, WHAT THE FUCK
                SwingUtilities.invokeLater(() -> {
                    fun.run();

                    var val = frame.getSize();

                    frame.setSize(0, 0);
                    frame.setSize(val);
                });
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @NotNull
    private JButton button_openLogs(JFrame frame) {
        var button = new JButton("Open logs");

        button.setFont(font);
        button.setBounds(10, 310, 140, 40);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (logFile == null) {
                    JOptionPane.showMessageDialog(
                            frame,
                            "Failed to open logs: no log available",
                            "Unable to open logs",
                            JOptionPane.ERROR_MESSAGE
                    );

                    return;
                }

                try {
                    Desktop.getDesktop().open(logFile);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(
                            frame,
                            "Failed to open logs\nTechnical information:\n\n" + ex.getMessage() + "\n(" + ex.getClass().getSimpleName() + ")",
                            "Unable to open logs",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        return button;
    }

    @NotNull
    private JButton button_uploadLogs(JFrame frame) {
        var button = new JButton("<html><table><td align=\"center\" border=\"none\">Create Discord message asking for help</td></table></html>");

        button.setFont(font);
        button.setBounds(155, 310, 205, 40);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (logFile == null) {
                    JOptionPane.showMessageDialog(
                            frame,
                            "Failed to create a Discord message: no log available",
                            "Unable to create a Discord message",
                            JOptionPane.ERROR_MESSAGE
                    );

                    return;
                }

                var res = JOptionPane.showConfirmDialog(
                        frame,
                        "This will upload your logs without any sensitive data (e.g. access tokens will be censored). Do you consent?",
                        "Creating a Discord message",
                        JOptionPane.YES_NO_CANCEL_OPTION
                );

                if (res == JOptionPane.NO_OPTION) {
                    JOptionPane.showMessageDialog(
                            frame,
                            "Cancelled creating message.",
                            "Cancelled",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }

                if (res != JOptionPane.YES_OPTION)
                    return;

                var files = new ArrayList<>();
                var filesmsg = new ArrayList<String>();

                files.add("logs/latest.log");
                files.add("logs/debug.log");
                files.add("logs/nocrash.log");

                var noReport = new boolean[]{ true };
                var reportFolder = Path.of("crash-reports");

                if (Files.isDirectory(reportFolder)) {
                    try (var reports = Files.list(reportFolder)) {
                        reports.forEach(report -> {
                            try {
                                if (!Files.isDirectory(report) && System.currentTimeMillis() - Files.getLastModifiedTime(report).toMillis() < 13_000) {
                                    files.add(report.toString());
                                    noReport[0] = false;
                                }
                            } catch (IOException ex) {
                                JOptionPane.showMessageDialog(
                                        frame,
                                        "Failed to upload file '%s' (I/O error); skipping\nTechnical information:\n\n".formatted(report.toString()) + ex.getMessage() + "\n(" + ex.getClass().getSimpleName() + ")",
                                        "Unable to create a Discord message",
                                        JOptionPane.WARNING_MESSAGE
                                );
                            }
                        });
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(
                                frame,
                                "Failed to find a crash report (I/O error); skipping\nTechnical information:\n\n" + ex.getMessage() + "\n(" + ex.getClass().getSimpleName() + ")",
                                "Unable to create a Discord message",
                                JOptionPane.WARNING_MESSAGE
                        );
                    }
                }

                try (var s = Files.list(Path.of("."))) {
                    s.forEach(path -> {
                        try {
                            if (path.getFileName().toString().matches("hs_err_pid[0-9]+\\.log") && System.currentTimeMillis() - Files.getLastModifiedTime(path).toMillis() < 1000*16) {
                                files.add(new Object[]{
                                        path.toString(),
                                        (UnaryOperator<String>) data -> data.split("------")[0]
                                });
                            }
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(
                                    frame,
                                    "Failed to upload file '%s' (I/O error); skipping\nTechnical information:\n\n".formatted(path.toString()) + ex.getMessage() + "\n(" + ex.getClass().getSimpleName() + ")",
                                    "Unable to create a Discord message",
                                    JOptionPane.WARNING_MESSAGE
                            );
                        }
                    });
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(
                            frame,
                            "Failed to find a hs_err_pid report (I/O error); skipping\nTechnical information:\n\n" + ex.getMessage() + "\n(" + ex.getClass().getSimpleName() + ")",
                            "Unable to create a Discord message",
                            JOptionPane.WARNING_MESSAGE
                    );
                }

                for (var object : files) {
                    final String file;
                    final UnaryOperator<String> transformer;

                    if (object instanceof String str) {
                        file = str;
                        transformer = UnaryOperator.identity();
                    } else {
                        file        = (String) ((Object[]) object)[0];
                        transformer = (UnaryOperator<String>) ((Object[]) object)[1];
                    }

                    var path = Path.of(file);

                    final String binID;

                    try {
                        binID = Hastebin.create(List.of(
                                new BinFile(file, transformer.apply(new LogCensor(Files.readString(path, StandardCharsets.UTF_8)).censor()))
                        ), false, System.currentTimeMillis()/1000 + 60*60*24*3);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(
                                frame,
                                "Failed to upload file '%s' (connection error)\nTechnical information:\n\n".formatted(file) + ex.getMessage() + "\n(" + ex.getClass().getSimpleName() + ")",
                                "Unable to create a Discord message",
                                JOptionPane.ERROR_MESSAGE
                        );

                        return;
                    }

                    filesmsg.add("**[%s](%s)**".formatted(file, "https://olafcio1.github.io/logviewer/?" + binID));
                }

                var message = "Minecraft crashed%s. Here are the relevant links:\n".formatted(noReport[0] ? " without a crash report" : "") +
                              "\n" +
                              String.join(" ∕ ", filesmsg) +
                              "\n\n" +
                              "-# *Running NC %s*".formatted(State.nocrash);

                try {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                            new StringSelection(message),
                            null
                    );
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            frame,
                            "Failed to copy message to clipboard; a Notepad window with it will be opened instead after clicking OK\nTechnical information:\n\n" + ex.getMessage() + "\n(" + ex.getClass().getSimpleName() + ")",
                            "Unable to copy Discord message",
                            JOptionPane.WARNING_MESSAGE
                    );

                    var msgPath = Path.of(System.getProperty("TEMP"))
                                      .resolve("nocrash_message.txt");

                    try {
                        Files.writeString(msgPath, message, StandardCharsets.UTF_8);

                        Desktop.getDesktop().open(msgPath.toFile());
                    } catch (IOException exc) {
                        JOptionPane.showMessageDialog(
                                frame,
                                "Failed to open message in notepad; aborting\nTechnical information:\n\n" + ex.getMessage() + "\n(" + ex.getClass().getSimpleName() + ")",
                                "Unable to open Discord message",
                                JOptionPane.ERROR_MESSAGE
                        );

                        return;
                    }
                }

                var discord = State.modloader == null            ? null :
                              State.modloader.equals("FORGE")    ? new Discord("Minecraft Forge Official", "https://discord.gg/UuM6bmAjXh") :
                              State.modloader.equals("NEOFORGE") ? new Discord("The NeoForged Project",    "https://discord.gg/UvedJ9m")    :
                              State.modloader.equals("FABRIC")   ? new Discord("The Fabric Project",       "https://discord.gg/v6v4pMv")    :
                              State.modloader.equals("SPONGE")   ? new Discord("SpongePowered",            "https://discord.gg/sponge")    :
                                                                   null;

                if (discord == null) {
                    JOptionPane.showMessageDialog(
                            frame,
                            "A Discord message has been created and saved to clipboard.\nYou can send it on a channel such as #player-support in a Discord for your platform (%s).".formatted(State.modloader),
                            "Successfully created a Discord message",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } else {
                    var displayName = discord.displayName();
                    if (!displayName.toLowerCase().startsWith("the"))
                        displayName = "the " + displayName;

                    JOptionPane.showMessageDialog(
                            frame,
                            "A Discord message has been created and saved to clipboard.\nYou can send it on a channel such as #player-support in e.g. %s Discord.\n\n(invite link: %s)".formatted(displayName, discord.inviteLink()),
                            "Successfully created a Discord message",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });

        return button;
    }

    public void addMessage(Message message) {
        IO.println("[NoCrashWindow/INFO] Showing message: \"%s\"".formatted(message.text()));

        exec(() -> {
            addMessage0(message, messages);
        });
    }

    private void addMessage0(Message message, JPanel parent) {
        addMessage1(message, parent);

        parent.revalidate();
        parent.repaint();
    }

    private void addMessage1(Message message, JPanel parent) {
        var flex = new JPanel(new FlowLayout(FlowLayout.CENTER));
        var box = new JPanel(new BorderLayout(0, 0));

        flex.setOpaque(false);
        box.setOpaque(false);

        // Icon
        var icon = (ImageIcon) UIManager.get(iconForMessage(message), parent.getLocale());
            icon = scale(icon, 17, 17);

        // Border
        box.setBackground(new Color(0x777777));
        box.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), new EmptyBorder(5, 6, 3, 3)));

        // Label
        var label = new JLabel(message.text(), icon, SwingConstants.LEFT);

        label.setToolTipText(message.technical());

        label.setForeground(new Color(0xBCBCBC));
        label.setFont(font.deriveFont(Font.PLAIN));

        box.add(label);

        AffineTransform affineTransform = new AffineTransform();
        affineTransform.translate(0, -1.4);
        affineTransform.scale(.84, 1);
        label.setFont(label.getFont().deriveFont(affineTransform));


        flex.add(box);

        var fixType = message.autoFixType();
        if (fixType != FixType.UNAVAILABLE) {
            final var btn = new JLabel[1];

            flex.add(btn[0] = addButton(() -> {
                if (
                        JOptionPane.showConfirmDialog(null,
                                fixType.getDescription(),
                                "Are you sure (%s)?".formatted(fixType.getTitle()),
                                JOptionPane.YES_NO_CANCEL_OPTION
                        ) == JOptionPane.YES_OPTION
                ) {
                    message.autoFix().run();
                    btn[0].setEnabled(false);

                    var checkLabel = new JLabel(ICON_CHECK);

                    checkLabel.setToolTipText("The quick fix has been applied for this issue.");

                    flex.add(checkLabel);
                    flex.revalidate();
                    flex.repaint();

                    //TODO cursor
                    // btn[0].setCursor(Cursor.getSystemCustomCursor("Invalid.32x32"));
                    // } catch (AWTException e) {
                    // new RuntimeException("Failed to set cursor", e).printStackTrace();
                }
            }, fixType.getTooltip(), getIconForFix(fixType)));
        }

        parent.add(flex);

        if (!message.children().isEmpty()) {
            var children = new JPanel();

            children.setOpaque(false);
            children.setLayout(new BoxLayout(children, BoxLayout.PAGE_AXIS));
            children.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(0, 4, 0, 0),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(0x858585)),
                            BorderFactory.createEmptyBorder(0, 4, 0, 0)
                    )
            ));

            for (var child : message.children())
                addMessage0(child, children);

            parent.add(children);
        }
    }

    private static Icon getIconForFix(FixType fixType) {
        if (fixType == FixType.DELETE)
            return ICON_DELETE;
        else if (fixType == FixType.DOWNLOAD)
            return ICON_DOWNLOAD;
        else
            throw new RuntimeException("Tried to get icon for non-available fix: %s".formatted(fixType));
    }

    @NotNull
    private static String iconForMessage(Message message) {
        if (message.type() == MessageType.ERROR)
            return "OptionPane.errorIcon";
        else if (message.type() == MessageType.CRITICAL)
            return "OptionPane.errorIcon";
        else if (message.type() == MessageType.WARNING)
            return "OptionPane.warningIcon";
        else if (message.type() == MessageType.INFO)
            return "OptionPane.informationIcon";
        else
            return "OptionPane.questionIcon";
    }

    private static JLabel addButton(Runnable fix, String tooltip, Icon icon) {
        var button = new JLabel(icon);

        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xB5B1B1)), BorderFactory.createRaisedSoftBevelBorder()));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (button.isEnabled())
                    fix.run();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (button.isEnabled())
                    button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xB5B1B1)), BorderFactory.createLoweredSoftBevelBorder()));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.isEnabled())
                    button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xB5B1B1)), BorderFactory.createRaisedSoftBevelBorder()));
            }
        });

        button.setToolTipText(tooltip);

        return button;
    }

    @NotNull
    private static ImageIcon scale(ImageIcon icon, int width, int height) {
        var image = icon.getImage(); // transform it
        var newimg = image.getScaledInstance(width, height, Image.SCALE_SMOOTH); // scale it the smooth way

        icon = new ImageIcon(newimg);  // transform it back

        return icon;
    }

    private static byte[] read(String path) {
        try (var stream = NoCrashWindow.class.getResourceAsStream("/" + path)) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: '%s'".formatted(path), e);
        }
    }

    private static ByteArrayInputStream readToStream(String path) {
        try (var stream = NoCrashWindow.class.getResourceAsStream("/" + path)) {
            return new ByteArrayInputStream(stream.readAllBytes()); // Yeah I know it's quite unnecessary
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: '%s'".formatted(path), e);
        }
    }

    private static final Icon ICON_DELETE = scale(new ImageIcon(read("icons/delete.png")), 20, 20);
    private static final Icon ICON_DOWNLOAD = scale(new ImageIcon(read("icons/download.png")), 20, 20);
    private static final Icon ICON_CHECK = scale(new ImageIcon(read("icons/checkmark.png")), 20, 20);

    private static class VerticalLayout extends FlowLayout {
        public VerticalLayout() {
        }

        public VerticalLayout(int align) {
            super(align);
        }

        public VerticalLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public void layoutContainer(Container target) {
            synchronized (target.getTreeLock()) {
                Insets insets = target.getInsets();
                int maxwidth = target.getWidth() - (insets.left + insets.right + getHgap()*2);
                int nmembers = target.getComponentCount();
                int x = 0, y = insets.top + getVgap();
                int rowh = 0, start = 0;

                boolean ltr = target.getComponentOrientation().isLeftToRight();

                boolean useBaseline = getAlignOnBaseline();
                int[] ascent = null;
                int[] descent = null;

                if (useBaseline) {
                    ascent = new int[nmembers];
                    descent = new int[nmembers];
                }

                for (int i = 0 ; i < nmembers ; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = m.getPreferredSize();
                        m.setSize(d.width, d.height);

                        if (useBaseline) {
                            int baseline = m.getBaseline(d.width, d.height);
                            if (baseline >= 0) {
                                ascent[i] = baseline;
                                descent[i] = d.height - baseline;
                            }
                            else {
                                ascent[i] = -1;
                            }
                        }
                        if ((x == 0)/* || ((x + d.width) <= maxwidth)*/) {
                            if (x > 0) {
                                x += getHgap();
                            }
                            x += d.width;
                            rowh = Math.max(rowh, d.height);
                        } else {
                            rowh = moveComponents(target, insets.left + getHgap(), y,
                                    maxwidth - x, rowh, start, i, ltr,
                                    useBaseline, ascent, descent);
                            x = d.width;
                            y += getVgap() + rowh;
                            rowh = d.height;
                            start = i;
                        }
                    }
                }

                moveComponents(target, insets.left + getHgap(), y, maxwidth - x, rowh,
                               start, nmembers, ltr, useBaseline, ascent, descent);
            }
        }

        private int moveComponents(Container target, int x, int y, int width, int height,
                                   int rowStart, int rowEnd, boolean ltr,
                                   boolean useBaseline, int[] ascent,
                                   int[] descent) {
            switch (getAlignment()) {
                case LEFT:
                    x += ltr ? 0 : width;
                    break;
                case CENTER:
                    x += width / 2;
                    break;
                case RIGHT:
                    x += ltr ? width : 0;
                    break;
                case LEADING:
                    break;
                case TRAILING:
                    x += width;
                    break;
            }
            int maxAscent = 0;
            int nonbaselineHeight = 0;
            int baselineOffset = 0;
            if (useBaseline) {
                int maxDescent = 0;
                for (int i = rowStart ; i < rowEnd ; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        if (ascent[i] >= 0) {
                            maxAscent = Math.max(maxAscent, ascent[i]);
                            maxDescent = Math.max(maxDescent, descent[i]);
                        }
                        else {
                            nonbaselineHeight = Math.max(m.getHeight(),
                                    nonbaselineHeight);
                        }
                    }
                }
                height = Math.max(maxAscent + maxDescent, nonbaselineHeight);
                baselineOffset = (height - maxAscent - maxDescent) / 2;
            }
            for (int i = rowStart ; i < rowEnd ; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    int cy;
                    if (useBaseline && ascent[i] >= 0) {
                        cy = y + baselineOffset + maxAscent - ascent[i];
                    }
                    else {
                        cy = y + (height - m.getHeight()) / 2;
                    }
                    if (ltr) {
                        m.setLocation(x, cy);
                    } else {
                        m.setLocation(target.getWidth() - x - m.getWidth(), cy);
                    }
                    x += m.getWidth() + getHgap();
                }
            }
            return height;
        }
    }
}
