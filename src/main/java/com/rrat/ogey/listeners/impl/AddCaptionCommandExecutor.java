package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedProvider;
import org.javacord.api.entity.message.embed.EmbedThumbnail;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;
import su.dkzde.genki.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
@BotCommand("caption")
public class AddCaptionCommandExecutor implements CommandExecutor {

    private static final Pattern pt_vidextensions = Pattern.compile(".*(?<extension>(\\.mp4)|(\\.mov)|(\\.webm))$");
    private static final Pattern pt_emoji = Pattern.compile("<(a)?:(?<name>\\w+):(?<id>\\d+)>");
    private int vidwidth;
    private int vidheight;

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
        boolean isvideo = Arrays.equals(Arrays.copyOfRange(Mimg, 4, 7), new byte[]{-97, 66, -122}) | Arrays.equals(Arrays.copyOfRange(Mimg, 4, 7), new byte[]{102, 116, 121});
        boolean iswebm = Arrays.equals(Arrays.copyOfRange(Mimg, 4, 7), new byte[]{-97, 66, -122});


        if (isvideo) {
            try {
                File inputfile;
                if (iswebm)
                    inputfile = File.createTempFile("download", ".webm");
                else
                    inputfile = File.createTempFile("download", ".mp4");
                File outputfile = File.createTempFile("finished", ".mp4");
                File captionfile = File.createTempFile("caption", ".jpg");
                FileOutputStream fos = new FileOutputStream(inputfile);
                fos.write(Mimg);
                fos.close();
                BufferedImage caption = captiononly(vidheight, vidwidth, arguments);
                ImageIO.write(caption, "jpg", captionfile);
                ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-y", "-v", "error", "-i", inputfile.getAbsolutePath().replaceAll("\\\\", "/"), "-i", captionfile.getAbsolutePath().replaceAll("\\\\", "/"), "-filter_complex", "[0:v] pad=width=iw:height=ih+" + caption.getHeight() + ":x=0:y=" + caption.getHeight() + "[padded];[padded][1:v]overlay=0.0", outputfile.getAbsolutePath().replaceAll("\\\\", "/"));
                Process process;
                process = processBuilder.start();
                process.waitFor();
                captionfile.delete();
                inputfile.delete();
                if (process.exitValue() == 0)
                    new MessageBuilder().addAttachment(outputfile)
                            .send(event.getChannel()).thenRun(outputfile::delete);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        else if (Arrays.equals(Arrays.copyOf(Mimg, 3), new byte[]{71, 73, 70})) {
            try {
                byte[] caption = captionGif(Mimg, arguments);
                new MessageBuilder()
                        .addAttachment(caption, "caption.gif")
                        .send(event.getChannel());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (Arrays.equals(Arrays.copyOf(Mimg,4),new byte[]{82,73,70,70})) { //Checks if it's a .webp file
            try {
                File webpfile = File.createTempFile("webp-dl", ".webp");
                FileOutputStream fos = new FileOutputStream(webpfile);
                fos.write(Mimg);
                fos.close();
                File jpgfile = File.createTempFile("to-jpg",".jpg");
                ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg","-y","-v","error","-i", webpfile.getAbsolutePath().replaceAll("\\\\","/"),"-crf 28","-threads 2",jpgfile.getAbsolutePath().replaceAll("\\\\","/"));
                Process process;
                process = processBuilder.start();
                process.waitFor();
                webpfile.delete();
                FileInputStream fin = new FileInputStream(jpgfile);
                if (process.exitValue() == 0)
                    new MessageBuilder().addAttachment(captionimage(fin.readAllBytes(),arguments),"CaptionedImage.jpg")
                            .send(event.getChannel()).thenRun(jpgfile::delete);
            } catch (IOException | InterruptedException e) {e.printStackTrace();}
        }
        else
            new MessageBuilder().addAttachment(captionimage(Mimg, arguments), "Captionedimage.jpg")
                    .send(event.getChannel());
    }

    private Optional<CompletableFuture<byte[]>> getbytearray (MessageCreateEvent event) {
        Optional<CompletableFuture<byte[]>> leBytes = Optional.empty();
        Message message = event.getMessage();

        if (!message.getAttachments().isEmpty()) {
            MessageAttachment messageAttachment = message.getAttachments().get(0);
            if (messageAttachment.isImage() || messageAttachment.getFileName().matches(pt_vidextensions.pattern())) {
                if (messageAttachment.getSize() < 10000000) {
                    vidheight = messageAttachment.getHeight().orElse(200);
                    vidwidth = messageAttachment.getWidth().orElse(200);
                     leBytes = Optional.ofNullable(messageAttachment.asByteArray());
                }
            }
        }

        if (leBytes.isEmpty())
            leBytes = message.getReferencedMessage()
                    .map(Message::getAttachments)
                    .filter(list -> !list.isEmpty())
                    .map(messageAttachments -> messageAttachments.get(0))
                    .filter(messageAttachment -> messageAttachment.isImage() || messageAttachment.getFileName().matches(pt_vidextensions.pattern()))
                    .filter(messageAttachment -> messageAttachment.getSize() < 10000000)
                    .map(messageAttachment -> {
                        vidheight = messageAttachment.getHeight().orElse(200);
                        vidwidth = messageAttachment.getWidth().orElse(200);
                        return messageAttachment.asByteArray();});

        if (leBytes.isEmpty())
            leBytes = getembed(event);

        if (leBytes.isEmpty())
            leBytes = message.getMessagesBefore(2).join()
                    .getNewestMessage()
                    .map(Message::getAttachments)
                    .filter(list -> !list.isEmpty())
                    .map(messageAttachments -> messageAttachments.get(0))
                    .filter(messageAttachment -> messageAttachment.isImage() || messageAttachment.getFileName().matches(pt_vidextensions.pattern()))
                    .filter(messageAttachment -> messageAttachment.getSize() < 10000000)
                    .map(messageAttachment -> {
                        vidheight = messageAttachment.getHeight().orElse(200);
                        vidwidth = messageAttachment.getWidth().orElse(200);
                        return messageAttachment.asByteArray();});

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

        if (embed.map(Embed::getType).orElse("nothing").equals("image"))
            return embed.flatMap(Embed::getThumbnail)
                    .map(EmbedThumbnail::getProxyUrl)
                    .map(URL -> {
                        try (InputStream is = URL.openStream()){
                            return CompletableFuture.completedFuture(is.readAllBytes());}
                        catch (IOException e) {e.printStackTrace();}
                        return null;});

        if (embed.map(Embed::getType).orElse("nothing").equals("video") && embed.flatMap(Embed::getUrl).map(URL::toString).orElse("nothing").matches("(https://)(cdn.discordapp.com|media.discordapp.net)/attachments/.*"))
            return embed.flatMap(Embed::getVideo)
                    .map(embedVideo -> {
                        vidheight = embedVideo.getHeight();
                        vidwidth = embedVideo.getWidth();
                        return embedVideo.getUrl();})
                    .map(URL -> {
                        try {
                            URLConnection connection = URL.openConnection();
                            if (connection.getContentLength() < 10000000)
                                return CompletableFuture.completedFuture(connection.getInputStream().readAllBytes());} catch (IOException e) {
                            e.printStackTrace();}
                        return null;});

        else if (embed.flatMap(Embed::getProvider).map(EmbedProvider::getName).orElse("noprovider").equals("Tenor")) {
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
        int imgWidth = bufferedImage.getWidth(), imgHeight = bufferedImage.getHeight();
        BufferedImage caption = captiononly(imgHeight,imgWidth,arguments);
        BufferedImage sentimage = new BufferedImage(imgWidth, imgHeight + caption.getHeight() , BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sentimage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.drawImage(caption,0,0,null);
        g.drawImage(bufferedImage, 0,caption.getHeight(), null);

        return sentimage;
    }

    private BufferedImage captiononly(int imgHeight,int imgWidth, String arguments){
        int Lines = 1, emojicount = 0,lettersize;
        double lettersizeoffset = 0.15;
        lettersize = Math.min(imgHeight, imgWidth);
        BufferedImage temp = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = temp.createGraphics();
        g.setFont(new Font("Futura Extra Black Condensed", Font.PLAIN, (int) (lettersize * lettersizeoffset)));

        ArrayList<String> wlines = new ArrayList<>();
        ArrayList<Image> emojis = new ArrayList<>();
        StringBuilder TempLine = new StringBuilder();

        for (String word : arguments.split(" ")) {
            Matcher matcher = pt_emoji.matcher(word);
            if (matcher.matches()) {
                emojicount++;
                try {
                    emojis.add(ImageIO.read(new URL("https://cdn.discordapp.com/emojis/<EmojiID>.png".replace("<EmojiID>", matcher.group("id")))));
                } catch (IOException e) {e.printStackTrace();}
                word = "$l";
            }

            if (g.getFontMetrics().stringWidth(String.valueOf(TempLine)) + g.getFontMetrics().stringWidth(word) > imgWidth * 1.01) {
                if (g.getFontMetrics().stringWidth(word) > imgWidth *1.02){
                    String wordpart1 = word;
                    do{
                        wordpart1=wordpart1.substring(0,wordpart1.length()-1);
                    }while (g.getFontMetrics().stringWidth(wordpart1) > imgWidth * 1.01);
                    TempLine.append(wordpart1).append(" ");
                    word = word.substring(wordpart1.length());
                }
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
        g.setFont(new Font("Futura Extra Black Condensed", Font.PLAIN, (int) (lettersize * lettersizeoffset)));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.fillRect(0, 0, imgWidth, (int) ((lettersize * lettersizeoffset) + Lines * g.getFontMetrics().getHeight()));
        g.setColor(Color.black);
        int liney = (int) (-g.getFontMetrics().getHeight() * 0.21), wwidths, emoji = -1;

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