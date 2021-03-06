package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedThumbnail;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;
import su.dkzde.genki.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@Component
@BotCommand("caption")
public class AddCaptionCommandExecutor implements CommandExecutor {

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        byte[] Mimg = new byte[0];

        Optional<CompletableFuture<byte[]>> attachment = getbytearray(event);
        if (attachment.isPresent())
            Mimg = attachment.get().join();

        if (arguments == null || Arrays.equals(Mimg, new byte[0])) {
            if (arguments != null)
                new MessageBuilder().append("Here is your caption bro")
                        .addAttachment(captiononly(500, 500, arguments), "Captionedimage.jpg")
                        .send(event.getChannel());
            return;
        }

        if (Arrays.equals(Arrays.copyOf(Mimg, 3), new byte[]{71, 73, 70})) {
            try {
                byte[] caption = captionGif(Mimg, arguments);
                new MessageBuilder()
                        .addAttachment(caption, "caption.gif")
                        .send(event.getChannel());
            } catch (IOException e) {e.printStackTrace();}
        }
        else
            new MessageBuilder().addAttachment(captionimage(Mimg, arguments), "Captionedimage.jpg")
                    .send(event.getChannel());
    }

    private Optional<CompletableFuture<byte[]>> getbytearray (MessageCreateEvent event) {
        Optional<CompletableFuture<byte[]>> leBytes = Optional.empty();

        if (!event.getMessage().getAttachments().isEmpty() && event.getMessage().getAttachments().get(0).isImage())
            leBytes = Optional.ofNullable(event.getMessage().getAttachments().get(0).downloadAsByteArray());

        if (leBytes.isEmpty())
            leBytes = event.getMessage().getReferencedMessage()
                    .map(Message::getAttachments)
                    .filter(list -> !list.isEmpty())
                    .map(messageAttachments -> messageAttachments.get(0))
                    .filter(MessageAttachment::isImage)
                    .map(MessageAttachment::downloadAsByteArray);

        if (leBytes.isEmpty())
            leBytes = getembed(event);

        if (leBytes.isEmpty())
            leBytes = event.getMessage().getMessagesBefore(2).join()
                    .getNewestMessage()
                    .map(Message::getAttachments)
                    .filter(list -> !list.isEmpty())
                    .map(messageAttachments -> messageAttachments.get(0))
                    .filter(MessageAttachment::isImage)
                    .map(MessageAttachment::downloadAsByteArray);

        return leBytes;
    }

    private Optional<CompletableFuture<byte[]>> getembed (MessageCreateEvent event) {
        Optional<Embed> embed;

        embed = event.getMessage().getReferencedMessage()
                .map(Message::getEmbeds)
                .filter(embeds -> !embeds.isEmpty())
                .map(embeds -> embeds.get(0));

        if (embed.isEmpty())
            embed = event.getMessage().getMessagesBefore(2).join().getNewestMessage()
                    .map(Message::getEmbeds)
                    .filter(embeds -> !embeds.isEmpty())
                    .map(embeds -> embeds.get(0));

        if (embed.map(Embed::getType).isPresent() && embed.map(Embed::getType).get().equals("image")) {
            return embed.flatMap(Embed::getThumbnail)
                    .map(EmbedThumbnail::getProxyUrl)
                    .map(URL -> {
                        try (InputStream is = URL.openStream()){
                            return CompletableFuture.completedFuture(is.readAllBytes());}
                        catch (IOException e) {e.printStackTrace();}
                        return null;
                    });
        }

        else if (embed.flatMap(Embed::getProvider).isPresent() && embed.flatMap(Embed::getProvider).get().getName().equals("Tenor")) {
            return embed.flatMap(Embed::getThumbnail)
                    .map(EmbedThumbnail::getProxyUrl)
                    .map(URL -> {
                        String tenor = URL.toString().replaceAll("(.*)com/", "");
                        tenor = tenor.replaceAll("/(.*)", "");
                        tenor = tenor.substring(0, tenor.length() - 1).concat("d"); //"d" for lower res, "C" for higher res
                        tenor = "https://media.tenor.com/".concat(tenor);
                        return tenor;})
                    .map(String -> {
                        try (InputStream is = new URL(String).openStream()){
                            return CompletableFuture.completedFuture(is.readAllBytes());
                        } catch (IOException e) {e.printStackTrace();}
                        return null;
                    });
        }
        return Optional.empty();
    }

    private BufferedImage captionimage(byte[] imageinput, String arguments) {
        BufferedImage bufferedImage = null;
        try {bufferedImage = ImageIO.read(new ByteArrayInputStream(imageinput));} catch (IOException e) {e.printStackTrace();}

        assert bufferedImage != null;
        int imgWidth = bufferedImage.getWidth(), imgHeight = bufferedImage.getHeight(), Lines = 0, emojicount = 0;
        Graphics2D g = bufferedImage.createGraphics();
        g.setFont(new Font("Futura Extra Black Condensed", Font.PLAIN, (int) (imgHeight * .15)));

        ArrayList<String> wlines = new ArrayList<>();
        ArrayList<Image> emojis = new ArrayList<>();
        StringBuilder TempLine = new StringBuilder();

        for (String word : arguments.split(" ")) {
            if (word.endsWith(">") && word.startsWith("<")) {
                word = word.replaceAll("(.*):", "");
                word = word.replaceAll(">", "");
                emojicount++;
                try {
                    emojis.add(ImageIO.read(new URL("https://cdn.discordapp.com/emojis/<EmojiID>.png".replace("<EmojiID>", word))));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                word = "$l";
            }

            if (g.getFontMetrics().stringWidth(String.valueOf(TempLine)) + g.getFontMetrics().stringWidth(word) > imgWidth * 1.01) {
                if (TempLine.length() > 0)
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
        g = sentimage.createGraphics();
        g.setFont(new Font("Futura Extra Black Condensed", Font.PLAIN, (int) (imgHeight * .15)));
        g.drawImage(bufferedImage, 0, (int) (imgHeight * .18 + Lines * g.getFontMetrics().getHeight()), imgWidth, imgHeight, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.fillRect(0, 0, imgWidth, (int) (imgHeight * .18 + Lines * g.getFontMetrics().getHeight()));
        g.setColor(Color.black);
        int liney = (int) (-g.getFontMetrics().getHeight() * 0.25), wwidths, emoji = -1;

        for (String line : wlines) {
            wwidths = (imgWidth - g.getFontMetrics().stringWidth(line)) / 2;
            for (String word : line.split(" ")) {
                if (word.equals("$l") && emoji + 1 != emojicount) {
                    g.drawImage(emojis.get(emoji += 1), wwidths, liney + g.getFontMetrics().getHeight() / 3, (int) (g.getFontMetrics().getHeight() * 0.7), (int) (g.getFontMetrics().getHeight() * 0.7), null);
                } else
                    g.drawString(word, (wwidths), liney + g.getFontMetrics().getHeight());
                wwidths += g.getFontMetrics().stringWidth(word + " ");
            }
            liney += g.getFontMetrics().getHeight();
        }

        return sentimage;
    }

    private BufferedImage captiononly(int imgHeight,int imgWidth, String arguments){
        int Lines = 1, emojicount = 0;
        BufferedImage temp = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = temp.createGraphics();
        g.setFont(new Font("Futura Extra Black Condensed", Font.PLAIN, (int) (imgHeight * .15)));

        ArrayList<String> wlines = new ArrayList<>();
        ArrayList<Image> emojis = new ArrayList<>();
        StringBuilder TempLine = new StringBuilder();

        for (String word : arguments.split(" ")) {
            if (word.endsWith(">") && word.startsWith("<")) {
                word = word.replaceAll("(.*):", "");
                word = word.replaceAll(">", "");
                emojicount++;
                try {
                    emojis.add(ImageIO.read(new URL("https://cdn.discordapp.com/emojis/<EmojiID>.png".replace("<EmojiID>", word))));
                } catch (IOException e) {e.printStackTrace();}
                word = "$l";
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
        BufferedImage argcaption = new BufferedImage(imgWidth,Lines * g.getFontMetrics().getHeight(), BufferedImage.TYPE_INT_RGB);
        g = argcaption.createGraphics();
        g.setFont(new Font("Futura Extra Black Condensed", Font.PLAIN, (int) (imgHeight * .15)));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.fillRect(0, 0, imgWidth, (int) (imgHeight * .18 + Lines * g.getFontMetrics().getHeight()));
        g.setColor(Color.black);
        int liney = (int) (-g.getFontMetrics().getHeight() * 0.25), wwidths, emoji = -1;

        for (String line : wlines) {
            wwidths = (imgWidth - g.getFontMetrics().stringWidth(line)) / 2;
            for (String word : line.split(" ")) {
                if (word.equals("$l") && emoji + 1 != emojicount) {
                    g.drawImage(emojis.get(emoji += 1), wwidths, liney + g.getFontMetrics().getHeight() / 3, (int) (g.getFontMetrics().getHeight() * 0.7), (int) (g.getFontMetrics().getHeight() * 0.7), null);
                } else
                    g.drawString(word, (wwidths), liney + g.getFontMetrics().getHeight());
                wwidths += g.getFontMetrics().stringWidth(word + " ");
            }
            liney += g.getFontMetrics().getHeight();
        }
        return argcaption;
    }

    private byte[] captionGif(byte[] data, String arguments) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteStream ibs = ByteStream.from(data);
        ByteSink obs = ByteSink.from(bos);
        new ProtoDecoder(ibs)
                .accept(new ProtoVisitorDecorator(new ProtoEncoder(obs)) {

                    LogicalScreenDescriptor lsd;
                    BufferedImage caption;
                    int imageWidth;
                    int imageHeight;

                    boolean colorTableOverride = false;
                    byte[] colors;

                    @Override
                    public void visitLogicalScreenDescriptor(LogicalScreenDescriptor descriptor) {
                        imageHeight = descriptor.logicalScreenHeight();
                        imageWidth = descriptor.logicalScreenWidth();
                        caption = captiononly(imageHeight, imageWidth, arguments);
                        super.visitLogicalScreenDescriptor(lsd = descriptor.copy()
                                .setLogicalScreenHeight(imageHeight + caption.getHeight())
                                .build());
                        colors = new byte[3 * lsd.colorTableSize()];
                    }

                    @Override
                    public void visitGlobalColorTable(int index, byte r, byte g, byte b) {
                        colors[3 * index] = r;
                        colors[3 * index + 1] = g;
                        colors[3 * index + 2] = b;
                        if (!colorTableOverride && Arrays.equals(new int[]{Byte.toUnsignedInt(r), Byte.toUnsignedInt(g), Byte.toUnsignedInt(b)}, new int[]{0, 255, 0})) {
                            colors[3 * index] = -1;
                            colors[3 * index + 1] = -1;
                            colors[3 * index + 2] = -1;
                            colorTableOverride = true;
                        }
                        super.visitGlobalColorTable(index, r, g, b);
                    }

                    int frame = 0, gceframe = 0;

                    @Override
                    public void visitGraphicsControlExtension(GraphicsControlExtension extension) {
                        gceframe++;
                        if (gceframe != 1)
                            super.visitGraphicsControlExtension(extension);
                        else {
                            super.visitGraphicsControlExtension(
                                    extension.copy()
                                            .setDisposalMethod((byte) 1)
                                            .setTransparencyFlag(!colorTableOverride && extension.transparencyFlag())
                                            .setTransparencyIndex(colorTableOverride ? 0 : extension.transparencyIndex())
                                            .build());
                        }
                    }

                    @Override
                    public ProtoImageVisitor visitImage(ImageDescriptor descriptor) {
                        ++frame;
                        if (frame != 1) {
                            return super.visitImage(descriptor.copy()
                                    .setImageTopPosition(descriptor.imageTopPosition() + caption.getHeight())
                                    .build());
                        } else {
                            ImageDescriptor id = descriptor.copy()
                                    .setImageHeight(descriptor.imageHeight() + caption.getHeight())
                                    .setLocalColorTableUsed(colorTableOverride)
                                    .setColorTableSizeBits(colorTableOverride ? lsd.colorTableSizeBits() : 0)
                                    .build();

                            int[][] line = CalculateLineIndexes(colors, caption);
                            ImageEncoder encoder = new ImageEncoder(DataEncoder::makeEncoder, lsd, id, super.visitImage(id));
                            if (colorTableOverride)
                                for (int index = 0; index < colors.length / 3; index++)
                                    encoder.visitColorTable(index, colors[3 * index], colors[3 * index + 1], colors[3 * index + 2]);
                            return new ImageDecoder(DataDecoder::makeDecoder, new ImageVisitorDecorator(encoder) {
                                @Override
                                public void visitDataStart() {
                                    super.visitDataStart();
                                    for (int i = 0; i < caption.getHeight(); i++) {
                                        super.visitData(line[i]);
                                    }
                                }
                            });
                        }
                    }
                });
        obs.close();
        return bos.toByteArray();
    }

    private int[][] CalculateLineIndexes(byte[] colors, BufferedImage image) {
        int[][] line = new int[image.getHeight()][image.getWidth()];

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int color = image.getRGB(x, y), index = 0;
                int blue = color & 0xff, green = (color & 0xff00) >> 8, red = (color & 0xff0000) >> 16;
                int iblue = Byte.toUnsignedInt(colors[2]), igreen = Byte.toUnsignedInt(colors[1]), ired = Byte.toUnsignedInt(colors[0]);
                double d = Math.pow((red - ired * 0.30), 2) + Math.pow(((green - igreen) * 0.59), 2) + Math.pow(((blue - iblue) * 0.11), 2);
                for (int i = 1; i < colors.length / 3; i++) {
                    iblue = Byte.toUnsignedInt(colors[3 * i + 2]);
                    igreen = Byte.toUnsignedInt(colors[3 * i + 1]);
                    ired = Byte.toUnsignedInt(colors[3 * i]);
                    double id = Math.pow((red - ired * 0.30), 2) + Math.pow(((green - igreen) * 0.59), 2) + Math.pow(((blue - iblue) * 0.11), 2);
                    if (d > id) {
                        d = id;
                        index = i;
                    }
                }
                line[y][x] = index;
            }
        }
        return line;
    }
}