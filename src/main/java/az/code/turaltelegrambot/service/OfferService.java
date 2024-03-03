package az.code.turaltelegrambot.service;


import az.code.turaltelegrambot.dto.OfferDto;
import az.code.turaltelegrambot.entity.Offer;
import az.code.turaltelegrambot.repository.OfferRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j

public class OfferService extends DefaultAbsSender {

    private final OfferRepository offerRepository;

    public OfferService(@Value("${bot.token}") String telegramBotToken, OfferRepository offerRepository) {
        super(new DefaultBotOptions(), telegramBotToken);
        this.offerRepository = offerRepository;
    }

    public void saveOffer(Offer offer) {
        offerRepository.save(offer);
    }

    public synchronized void sendButtonToChat(long chatId, String buttonText, String callbackData, int messageId) throws TelegramApiException {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(buttonText);
        button.setCallbackData(callbackData);

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(java.util.List.of(List.of(button)));

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Click to see all offers");
        message.setReplyMarkup(markupKeyboard);
        message.setReplyToMessageId(messageId);

        execute(message);

    }

    public void generateImageWithText(OfferDto offerDto) {
        int width = 400;
        int height = 200;

        String backgroundImagePath = "background.jpg";
        BufferedImage backgroundImage = null;
        try {
            backgroundImage = ImageIO.read(new File("src/main/resources/offer_send.png"));
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        if (backgroundImage == null) {
            return;
        }

        BufferedImage image = new BufferedImage(backgroundImage.getWidth(), backgroundImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();
        graphics.drawImage(backgroundImage, 0, 0, backgroundImage.getWidth(), backgroundImage.getHeight(), null);

        // Set color to #F7CE25
        graphics.setColor(new Color(247, 206, 37)); // RGB values for #F7CE25

        graphics.setFont(new Font("Montserrat", Font.BOLD, 65));

        List<String> text1 = List.of(offerDto.getDateRange().split(","));
        String text3 = offerDto.getPrice();
        String text4 = offerDto.getCompanyName();
        graphics.drawString(text4, 800, 390);
        graphics.drawString("$" + text3, 800, 780);
        graphics.drawString(text1.get(0), 800, 1125);
        graphics.drawString(text1.get(1), 800, 1225);

        // Split additional info into lines with max 24 words each
        String text2 = offerDto.getAdditionalInfo();
        List<String> lines = splitAdditionalInfo(text2, 22);
        int startY = 1600;
        for (String line : lines) {
            graphics.drawString(line, 300, startY);
            startY += graphics.getFontMetrics().getHeight();
        }

        graphics.dispose();

        try {
            File output = new File("image_with_text.jpg");
            ImageIO.write(image, "jpg", output);
            System.out.println("Image successfully saved.");
        } catch (IOException e) {
            System.out.println("An error occurred while saving the image: " + e);
        }
    }

    private List<String> splitAdditionalInfo(String additionalInfo, int maxCharsPerLine) {
        List<String> lines = new ArrayList<>();
        StringBuilder lineBuilder = new StringBuilder();
        int charCount = 0;

        for (char c : additionalInfo.toCharArray()) {
            if (charCount >= maxCharsPerLine && c != ' ') {
                lines.add(lineBuilder.toString().trim());
                lineBuilder = new StringBuilder();
                charCount = 0;
            }
            lineBuilder.append(c);
            charCount++;
        }

        if (lineBuilder.length() > 0) {
            lines.add(lineBuilder.toString().trim());
        }

        return lines;
    }


    public int sendPhotoToChat(Long chatId, byte[] image, String caption, InlineKeyboardMarkup acceptButton) {
        try {
            // Convert byte array to file
            File imageFile = File.createTempFile("temp", ".jpg");
            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(image);
            fos.close();

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            InputFile inputFile = new InputFile();
            inputFile.setMedia(imageFile);
            sendPhoto.setPhoto(inputFile);
            sendPhoto.setCaption(caption);
            sendPhoto.setReplyMarkup(acceptButton);

            Message message = this.execute(sendPhoto);

            return message.getMessageId();
        } catch (IOException | TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}


