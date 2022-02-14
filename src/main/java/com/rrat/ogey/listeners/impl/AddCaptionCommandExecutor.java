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
        BufferedImage Mimg;
        if (event.getMessageAttachments().isEmpty() && event.getMessage().getReferencedMessage().isPresent() && event.getMessage().getReferencedMessage().get().getAttachments().get(0).isImage())
            Mimg = event.getMessage().getReferencedMessage().get().getAttachments().get(0).downloadAsImage().join();
        else if (!event.getMessageAttachments().isEmpty() && event.getMessageAttachments().get(0).isImage())
            Mimg = event.getMessageAttachments().get(0).downloadAsImage().join();
        else
            return;

        if (Mimg == null || arguments == null)
            return;
        new MessageBuilder().addAttachment(captionimage(Mimg,arguments),
                "Captionedimage.png").send(event.getChannel());
    }

    private BufferedImage captionimage(BufferedImage bufferedImage,String arguments) {

        int imgWidth = bufferedImage.getWidth(), imgHeight = bufferedImage.getHeight(), Lines = 0;
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        g.setFont(new Font("Futura XBlk", Font.PLAIN, (int) (imgHeight * .15)));

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
        BufferedImage sentimage = new BufferedImage(imgWidth, (int) (imgHeight * 1.18 + Lines * g.getFontMetrics().getHeight()), BufferedImage.TYPE_INT_ARGB);
        g = (Graphics2D) sentimage.getGraphics();
        g.setFont(new Font("Futura XBlk", Font.PLAIN, (int) (imgHeight * .15)));
        g.drawImage(bufferedImage, 0, (int) (imgHeight * .18 + Lines * g.getFontMetrics().getHeight()), imgWidth, imgHeight, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.fillRect(0, 0, imgWidth, (int) (imgHeight * .18 + Lines * g.getFontMetrics().getHeight()));
        g.setColor(Color.black);
        int liney = (int) (-g.getFontMetrics().getHeight() * 0.25);
        for (String line : wlines)
            g.drawString(line, (imgWidth-g.getFontMetrics().stringWidth(line))/2, liney += g.getFontMetrics().getHeight());
        g.dispose();
        return sentimage;
    }
}