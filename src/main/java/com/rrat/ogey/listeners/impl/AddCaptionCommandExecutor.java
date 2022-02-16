package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
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
        else if (event.getMessageAttachments().isEmpty() && event.getMessage().getMessagesBefore(1).join().getNewestMessage().isPresent() && event.getMessage().getMessagesBefore(1).join().getNewestMessage().get().getAttachments().get(0).isImage() )
            Mimg = event.getMessage().getMessagesBefore(1).join().getNewestMessage().get().getAttachments().get(0).downloadAsImage().join();
        else
            return;

        if (Mimg == null || arguments == null)
            return;
        new MessageBuilder().addAttachment(captionimage(Mimg,arguments),
                "Captionedimage.jpg").send(event.getChannel());
    }

    private BufferedImage captionimage(BufferedImage bufferedImage,String arguments) {

        int imgWidth = bufferedImage.getWidth(), imgHeight = bufferedImage.getHeight(), Lines = 0,emojicount = 0;
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        g.setFont(new Font("Futura Extra Black Condensed", Font.PLAIN, (int) (imgHeight * .15)));

        ArrayList<String> wlines = new ArrayList<>();
        ArrayList<Image> emojis = new ArrayList<>();
        StringBuilder TempLine = new StringBuilder();
        for (String word : arguments.split(" ")) {
            if (word.startsWith("<:")) {
                word = word.replaceAll("\\D+", "");
                emojicount++;
                try {
                    emojis.add(ImageIO.read(new URL("https://cdn.discordapp.com/emojis/<EmojiID>.png".replace("<EmojiID>", word))));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                word = "&";
            }
            if (g.getFontMetrics().stringWidth(String.valueOf(TempLine)) + g.getFontMetrics().stringWidth(word) > imgWidth * 1.01) {
                TempLine.setLength(TempLine.length() - 1);
                wlines.add(String.valueOf(TempLine));
                TempLine.delete(0, TempLine.length());
                Lines++;
            }
            TempLine.append(word);
            TempLine.append(" ");
        }
        TempLine.setLength(TempLine.length() - 1);
        wlines.add(String.valueOf(TempLine));
        g.dispose();
        BufferedImage sentimage = new BufferedImage(imgWidth, (int) (imgHeight * 1.18 + Lines * g.getFontMetrics().getHeight()), BufferedImage.TYPE_INT_RGB);
        g = (Graphics2D) sentimage.getGraphics();
        g.setFont(new Font("Futura Extra Black Condensed", Font.PLAIN, (int) (imgHeight * .15)));
        g.drawImage(bufferedImage, 0, (int) (imgHeight * .18 + Lines * g.getFontMetrics().getHeight()), imgWidth, imgHeight, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.fillRect(0, 0, imgWidth, (int) (imgHeight * .18 + Lines * g.getFontMetrics().getHeight()));
        g.setColor(Color.black);
        int liney = (int) (-g.getFontMetrics().getHeight() * 0.25),wwidths,emoji = -1;
        for (String line : wlines) {
            wwidths=(imgWidth - g.getFontMetrics().stringWidth(line)) / 2;
            for (String word : line.split(" ")) {
                if (word.equals("&") && emoji + 1 != emojicount) {
                    g.drawImage(emojis.get(emoji += 1), wwidths, liney + g.getFontMetrics().getHeight() / 3, (int) (g.getFontMetrics().getHeight() * 0.7), (int) (g.getFontMetrics().getHeight() * 0.7), null);
                }
                else
                    g.drawString(word, (wwidths), liney + g.getFontMetrics().getHeight());
                wwidths += g.getFontMetrics().stringWidth(word + " ");

            }
            liney += g.getFontMetrics().getHeight();
        }
        return sentimage;
    }
}