package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;
import su.dkzde.genki.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;


@Component
public class AddCaptionCommandExecutor implements CommandExecutor {

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        byte[] Mimg = new byte[0];
        if (!event.getMessageAttachments().isEmpty() && event.getMessageAttachments().get(0).isImage())
            Mimg = event.getMessageAttachments().get(0).downloadAsByteArray().join();
        else
        if (event.getMessage().getReferencedMessage().isPresent() && !event.getMessage().getReferencedMessage().get().getAttachments().isEmpty()
                && event.getMessage().getReferencedMessage().get().getAttachments().get(0).isImage())
            Mimg = event.getMessage().getReferencedMessage().get().getAttachments().get(0).downloadAsByteArray().join();
        else if (event.getMessage().getMessagesBefore(1).join().getNewestMessage().isPresent() && !event.getMessage().getMessagesBefore(1).join().getNewestMessage().get().getAttachments().isEmpty()
                && event.getMessage().getMessagesBefore(1).join().getNewestMessage().get().getAttachments().get(0).isImage())
            Mimg = event.getMessage().getMessagesBefore(1).join().getNewestMessage().get().getAttachments().get(0).downloadAsByteArray().join();

        if (arguments == null || Arrays.equals(Mimg, new byte[0])) {
            if (arguments != null)
                new MessageBuilder().addAttachment(captiononly(2000, 1000, "Here's your caption bro" + arguments), "Captionedimage.jpg").send(event.getChannel());
            return;
        }
        if (Arrays.equals(Arrays.copyOf(Mimg, 3), new byte[]{71, 73, 70})) {
            try {captionGif(Mimg,arguments);} catch (IOException e) {e.printStackTrace();}
            new MessageBuilder().addAttachment(new File("./src/main/resources/GifOutput.gif")).send(event.getChannel());
        }
        else
            new MessageBuilder().addAttachment(captionimage(Mimg, arguments), "Captionedimage.jpg").send(event.getChannel());
    }

    private BufferedImage captionimage(byte[] imageinput, String arguments) {
        BufferedImage bufferedImage = null;
        try {bufferedImage = ImageIO.read(new ByteArrayInputStream(imageinput));
        } catch (IOException e) {e.printStackTrace();}

        assert bufferedImage != null;
        int imgWidth = bufferedImage.getWidth(), imgHeight = bufferedImage.getHeight(), Lines = 0, emojicount = 0;
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
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
        g = (Graphics2D) sentimage.getGraphics();
        g.setFont(new Font("Futura Extra Black Condensed", Font.PLAIN, (int) (imgHeight * .15)));
        g.drawImage(bufferedImage, 0, (int) (imgHeight * .18 + Lines * g.getFontMetrics().getHeight()), imgWidth, imgHeight, null);
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

    private void captionGif(byte[] gifinput, String arguments) throws IOException {
        try {
            FileOutputStream outputStream = new FileOutputStream("./src/main/resources/GifInput.gif");
            outputStream.write(gifinput);
            outputStream.close();
        } catch (IOException e) {e.printStackTrace();}
        ByteSink obs = new ChannelByteSink(Files.newByteChannel(Path.of("./src/main/resources/GifOutput.gif"), StandardOpenOption.CREATE,StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING), ByteBuffer.allocate(1 << 10));
        final ProtoEncoder encoder = new ProtoEncoder(obs);
        new ProtoDecoder(new ChannelByteStream(
                Files.newByteChannel(Path.of("./src/main/resources/GifInput.gif"),StandardOpenOption.READ),
                ByteBuffer.allocate(1 << 10)))
                .accept(new ProtoVisitorDecorator(encoder) {
                    private LogicalScreenDescriptor lsd;
                    int width,height,stripeHeight;
                    byte[] colors;
                    @Override
                    public void visitLogicalScreenDescriptor(LogicalScreenDescriptor descriptor) {
                        super.visitLogicalScreenDescriptor(lsd = new LogicalScreenDescriptor(
                                descriptor.logicalScreenWidth(),
                                captiononly(descriptor.logicalScreenHeight(),descriptor.logicalScreenWidth(),arguments).getHeight()
                                        + descriptor.logicalScreenHeight(),
                                descriptor.globalColorTableUsed(),
                                descriptor.colorResolution(),
                                descriptor.colorTableSizeBits(),
                                descriptor.backgroundColorIndex(),
                                descriptor.pixelAspectRatio()));
                        width = descriptor.logicalScreenWidth();
                        height = descriptor.logicalScreenHeight();
                        stripeHeight = captiononly(descriptor.logicalScreenHeight(),descriptor.logicalScreenWidth(),arguments).getHeight();
                        colors = new byte[3*lsd.colorTableSize()];
                    }

                    @Override
                    public void visitGlobalColorTable(int index, byte r, byte g, byte b) {
                        colors[3*index] = r;
                        colors[3*index+1] = g;
                        colors[3*index+2] = b;
                        super.visitGlobalColorTable(index, r, g, b);
                    }

                    int frame = 0,gceframe = 0;

                    @Override
                    public void visitGraphicsControlExtension(GraphicsControlExtension extension) {
                        gceframe++;
                        if (gceframe != 1)
                            super.visitGraphicsControlExtension(extension);
                        else{
                            super.visitGraphicsControlExtension(new GraphicsControlExtension(
                                    (byte) 1,
                                    extension.inputFlag(),
                                    extension.transparencyFlag(),
                                    extension.delayTime(),
                                    extension.transparencyIndex()));
                        }
                    }

                    @Override
                    public ProtoImageVisitor visitImage(ImageDescriptor descriptor) {
                        ++frame;
                        if (frame != 1) {
                            return super.visitImage(new ImageDescriptor(
                                    descriptor.imageLeftPosition(),
                                    stripeHeight + descriptor.imageTopPosition(),
                                    descriptor.imageWidth(),
                                    descriptor.imageHeight(),
                                    descriptor.localColorTableUsed(),
                                    descriptor.interlacingUsed(),
                                    descriptor.colorTableSizeBits()));
                        }   else {

                            ImageDescriptor id = new ImageDescriptor(
                                    descriptor.imageLeftPosition(),
                                    descriptor.imageTopPosition(),
                                    descriptor.imageWidth(),
                                    stripeHeight + descriptor.imageHeight(),
                                    descriptor.localColorTableUsed(),
                                    descriptor.interlacingUsed(),
                                    descriptor.colorTableSizeBits());

                            int[][] line = CalculateLineIndexes(colors,captiononly(height,width,arguments));

                            ImageEncoder encoder = new ImageEncoder(LZW::makeEncoder ,lsd,id,super.visitImage(id));
                            return new ImageDecoder(LZW::makeDecoder,new ImageVisitorDecorator(encoder){
                                @Override public void visitDataStart() {
                                    super.visitDataStart();
                                    for (int i = 0;i < stripeHeight; i++) {
                                        super.visitData(line[i]);
                                    }
                                }
                            });
                        }
                    }
                });
        obs.close();
    }


    private int[][] CalculateLineIndexes(byte[] colors,BufferedImage image){
        int[][] line = new int[image.getHeight()][image.getWidth()];

        for (int y = 0; y < image.getHeight(); y++){
            for (int x = 0; x < image.getWidth(); x++){
                int color = image.getRGB(x,y), index=0;
                int blue = color & 0xff, green = (color & 0xff00) >> 8, red = (color & 0xff0000) >> 16;
                int iblue = Byte.toUnsignedInt(colors[2]), igreen = Byte.toUnsignedInt(colors[1]),ired = Byte.toUnsignedInt(colors[0]);
                double d = Math.pow((red-ired*0.30),2) + Math.pow(((green-igreen)*0.59),2) + Math.pow(((blue-iblue)*0.11),2);
                for (int i = 1; i < colors.length/3; i++){
                    iblue = Byte.toUnsignedInt(colors[3*i+2]);
                    igreen = Byte.toUnsignedInt(colors[3*i+1]);
                    ired = Byte.toUnsignedInt(colors[3*i]);
                    double id = Math.pow((red-ired*0.30),2) + Math.pow(((green-igreen)*0.59),2) + Math.pow(((blue-iblue)*0.11),2);
                    if (d > id){
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