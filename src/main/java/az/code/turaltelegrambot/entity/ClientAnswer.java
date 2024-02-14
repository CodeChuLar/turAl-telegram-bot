package az.code.turaltelegrambot.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    String answer;
    String question;
    boolean nextQuestion;
    @ManyToOne
    @JoinColumn(name = "session_id")
    Session session;
}
