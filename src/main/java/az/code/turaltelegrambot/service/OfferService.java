package az.code.turaltelegrambot.service;


import az.code.turaltelegrambot.dto.OfferDto;
import az.code.turaltelegrambot.telegram.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.common.message.DescribeUserScramCredentialsRequestData;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Service
@Slf4j

public class OfferService extends DefaultAbsSender {
    public OfferService(@Value("${bot.token}") String telegramBotToken) {
        super(new DefaultBotOptions(), telegramBotToken);
    }

    public void generateImageWithText(OfferDto offerDto) {
        int width = 400;
        int height = 200;

        String backgroundImagePath = "background.jpg";
        BufferedImage backgroundImage = null;
        try {
            backgroundImage = ImageIO.read(new File("src/main/resources/img_2.png"));
        } catch (IOException e) {
            System.out.println(e);
        }

        if (backgroundImage == null) {
            return;
        }

        BufferedImage image = new BufferedImage(backgroundImage.getWidth(), backgroundImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();
        graphics.drawImage(backgroundImage, 0, 0, backgroundImage.getWidth(), backgroundImage.getHeight(), null);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.BOLD, 30));

        String text1 = offerDto.getDateRange();
        String text2 = offerDto.getAdditionalInfo();
        String text3 = offerDto.getPrice();
        graphics.drawString(text1, 100, 70);
        graphics.drawString(text2, 100, 120);
        graphics.drawString(text3, 100, 170);

        graphics.dispose();

        try {
            File output = new File("image_with_text.jpg");
            ImageIO.write(image, "jpg", output);
            System.out.println("Image successfully saved.");
        } catch (IOException e) {
            System.out.println("An error occurred while saving the image: " + e);
        }
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


