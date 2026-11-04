package pl.olafcio.nocrash.crashpad;

import org.jetbrains.annotations.NotNull;
import pl.olafcio.nocrash.crashpad.message.FixType;
import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicPanelUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

public final class NoCrashWindow {
    private final Thread thread;
    private final File logFile;

    public NoCrashWindow(File logFile) {
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

    private synchronized void show() {
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

        var button = new JButton("Open logs");

        button.setFont(font);
        button.setBounds(10, 310, 160, 40);
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

        child.add(button);

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

        while (true) {
            try {
                callback.take().run();
            } catch (InterruptedException e) {
                return;
            }
        }
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
            }, fixType.getTooltip(), ICON_DELETE));
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
