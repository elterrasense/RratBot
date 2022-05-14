package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

@Component
public class AddSpeechBubbleCommandExecutor implements CommandExecutor {

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        byte[] Mimg = new byte[0];
        if (!event.getMessageAttachments().isEmpty() && event.getMessageAttachments().get(0).isImage())
            Mimg = event.getMessageAttachments().get(0).downloadAsByteArray().join();
        else if (event.getMessage().getReferencedMessage().isPresent() && !event.getMessage().getReferencedMessage().get().getAttachments().isEmpty()
                && event.getMessage().getReferencedMessage().get().getAttachments().get(0).isImage())
            Mimg = event.getMessage().getReferencedMessage().get().getAttachments().get(0).downloadAsByteArray().join();
        else if (event.getMessage().getMessagesBefore(1).join().getNewestMessage().isPresent() && !event.getMessage().getMessagesBefore(1).join().getNewestMessage().get().getAttachments().isEmpty()
                && event.getMessage().getMessagesBefore(1).join().getNewestMessage().get().getAttachments().get(0).isImage())
            Mimg = event.getMessage().getMessagesBefore(1).join().getNewestMessage().get().getAttachments().get(0).downloadAsByteArray().join();

        if (Arrays.equals(Mimg, new byte[0]))
            return;

        new MessageBuilder().addAttachment(speechbubble(Mimg, arguments), "Captionedimage.jpg").send(event.getChannel());
    }

    private BufferedImage speechbubble(byte[] imageinput, String arguments) {
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(imageinput));
        } catch (IOException e) {e.printStackTrace();}
        int pass = 0;
        double xoffset = 0.05, yoffset = 0.45;
        if (arguments != null) {
            for (String arg : arguments.split(" ")) {
                if (!arg.matches("[\\w&&[^1-9]]+")) {
                    if (pass == 0) {
                        xoffset = Integer.parseInt(arg) * 0.01;
                        pass++;
                    } else if (pass == 1) {
                        yoffset = Integer.parseInt(arg) * 0.01;
                        pass++;
                    } else break;
                }
            }
        }

        assert bufferedImage != null;
        BufferedImage sentimage = new BufferedImage(bufferedImage.getWidth(),bufferedImage.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sentimage.createGraphics();
        g.drawImage(bufferedImage,0,0,null);
        g.setColor(new Color(54,57,62));
        g.fillOval((int) (-bufferedImage.getWidth() * 0.15), (int) (-bufferedImage.getHeight() * 0.05), (int) (bufferedImage.getWidth() * 1.5), bufferedImage.getHeight() / 5);
        int[] xPoints;
        if (arguments != null && arguments.contains("right"))
            xPoints = new int[]{(int) (bufferedImage.getWidth() * (xoffset)), (int) (bufferedImage.getWidth() * (0.2+xoffset)), (int) (bufferedImage.getWidth() * (xoffset))};
        else
            xPoints = new int[]{(int) (bufferedImage.getWidth() * (xoffset)), (int) (bufferedImage.getWidth() * (0.2+xoffset)), (int) (bufferedImage.getWidth() * (0.2+xoffset))};

        int[] yPoints = {(int) (bufferedImage.getHeight() * (0.1)), (int) (bufferedImage.getHeight() * (0.1)), (int) (bufferedImage.getHeight() * (0.1+yoffset))};
        g.fillPolygon(xPoints, yPoints, 3);

        return sentimage;
        }
    }
