package pl.olafcio.nocrash.crashpad;

import org.jetbrains.annotations.NotNull;
import pl.olafcio.nocrash.crashpad.message.FixType;
import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
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

    public void setState(String state) {
        this.status.setText(state);
        this.progress.setBounds(0, 0, 0, 30);
    }

    public void setProgress(float percentage) {
        this.progress.setBounds(0, 0, (int) (500 * percentage), 30);
    }

    private final ArrayBlockingQueue<Runnable> callback
            = new ArrayBlockingQueue<>(16);

    public void exec(Runnable runnable) {
        if (Thread.currentThread() == thread) {
            runnable.run();
        } else {
            this.callback.add(runnable);
        }
    }

    private synchronized void show() {
        var frame = new JFrame();

        status.setBounds(5, 5, 500 - 5*2, 15);
        status.setOpaque(false);

        progress.setBackground(new Color(0x3A5EA1));

        messages.setLayout(new FlowLayout(FlowLayout.LEADING));
        messages.setBounds(5, 30, 500 - 5, 300 - 30);

        frame.add(status);
        frame.add(progress);
        frame.add(messages);

        var button = new JButton("Open logs");

        button.setBounds(10, 300, 160, 40);
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

        frame.add(button);

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


        // Icon
        var icon = (ImageIcon) UIManager.get(iconForMessage(message), parent.getLocale());
            icon = scale(icon, 17, 17);

        // Border
        box.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), new EmptyBorder(5, 6, 3, 3)));

        // Label
        var label = new JLabel(message.text(), icon, SwingConstants.LEFT);
        box.add(label);

        AffineTransform affineTransform = new AffineTransform();
        affineTransform.translate(0, -1.4);
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

    private static final Icon ICON_DELETE = scale(new ImageIcon(read("icons/delete.png")), 20, 20);
    private static final Icon ICON_CHECK = scale(new ImageIcon(read("icons/checkmark.png")), 20, 20);
}
