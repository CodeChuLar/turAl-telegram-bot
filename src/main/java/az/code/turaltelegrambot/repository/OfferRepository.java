package az.code.turaltelegrambot.repository;


import az.code.turaltelegrambot.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer,Long> {
}
