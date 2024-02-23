package az.code.turaltelegrambot.dto;

import az.code.turaltelegrambot.entity.Client;
import lombok.Data;

import java.util.UUID;

@Data
public class SessionDTO {
    private UUID id;
    private Client client;
    private String answers;

}