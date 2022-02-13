package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

@Component
public class AddCaptionCommandExecutor implements CommandExecutor {

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (event.getMessageAttachments().isEmpty() || !event.getMessageAttachments().get(0).isImage())
            return;

        BufferedImage bufferedImage = event.getMessageAttachments().get(0).downloadAsImage().join();
        int imgWidth = bufferedImage.getWidth(), imgHeight = bufferedImage.getHeight(), Lines = 0;
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        g.setFont(new Font("Arial", Font.PLAIN, (int) (imgHeight * .15)));

        ArrayList<String> wlines = new ArrayList<>();
        StringBuilder TempLine = new StringBuilder();
        for (String word : arguments.split(" ")) {
            if (g.getFontMetrics().stringWidth(String.valueOf(TempLine)) + g.getFontMetrics().stringWidth(word) > imgWidth * 1.01) {
                wlines.add(String.valueOf(TempLine));
                TempLine.delete(0, TempLine.length());
                Lines++;
            }
            TempLine.append(word);
            TempLine.append(" ");
        }
        wlines.add(String.valueOf(TempLine));
        g.dispose();
        BufferedImage sentimage = new BufferedImage(imgWidth, (int) (imgHeight * 1.15 + Lines * g.getFontMetrics().getHeight()), BufferedImage.TYPE_INT_RGB);
        g = (Graphics2D) sentimage.getGraphics();
        g.setFont(new Font("Arial", Font.PLAIN, (int) (imgHeight * .15)));
        g.drawImage(bufferedImage, 0, (int) (imgHeight * .15 + Lines * g.getFontMetrics().getHeight()), imgWidth, imgHeight, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.white);
        int liney = (int) (-g.getFontMetrics().getHeight() * 0.3);
        for (String line : wlines)
            g.drawString(line, 0, liney += g.getFontMetrics().getHeight());
        g.dispose();
        new MessageBuilder().addAttachment(sentimage, "Captionedimage.jpg").send(event.getChannel());
    }
}