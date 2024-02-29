package az.code.turaltelegrambot.service;


import az.code.turaltelegrambot.dto.OfferDto;
import org.apache.kafka.common.message.DescribeUserScramCredentialsRequestData;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class OfferService {

    public static void main(String[] args) {
        OfferDto offerDto = new OfferDto();
        offerDto.setPrice("21.21");
        offerDto.setDateRange("12.12.2012");
        offerDto.setAdditionalInfo("Lorem impumasfknsdfjkdhsgfklasdf");
        SendOffer(offerDto);

    }

    public static void SendOffer(OfferDto offerDto){

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
        int textX1 = backgroundImage.getWidth()/2;
        int textY1 = backgroundImage.getHeight()/2;
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
    }


