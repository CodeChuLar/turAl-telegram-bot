package az.code.turaltelegrambot.controller;

import az.code.turaltelegrambot.entity.Offer;
import az.code.turaltelegrambot.service.OfferService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/offers")
@CrossOrigin
public class OfferController {
    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping("/new")
    public Offer addStudent(@RequestBody Offer offer){
        return offerService.addStudent(offer);
    }

}