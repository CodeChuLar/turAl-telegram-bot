package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Offer;
import az.code.turaltelegrambot.repository.OfferRepository;
import org.springframework.stereotype.Service;

@Service
public class OfferService {
    private final OfferRepository offerRepository;

    public OfferService(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    //TODO request response ucun dto yazib ele elemek lazimdir.
    public Offer addStudent(Offer offer) {
        return offerRepository.save(offer);
    }
}
