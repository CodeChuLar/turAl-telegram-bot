//package az.code.turaltelegrambot.services;
//
//import az.code.turaltelegrambot.entity.Language;
//import az.code.turaltelegrambot.entity.Locale;
//import az.code.turaltelegrambot.repository.LocaleRepository;
//import az.code.turaltelegrambot.service.LocalizationServiceImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.mockito.Mockito.when;
//
//class LocalizationServiceImplTest {
//
//    @Mock
//    private LocaleRepository localeRepository;
//
//    @InjectMocks
//    private LocalizationServiceImpl localizationService;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void testTranslate() {
//        String key = "testKey";
//        Language language = Language.EN;
//        String expectedTranslation = "Test Translation";
//        Locale mockedLocale = new Locale(,language, key, expectedTranslation);
//
//        when(localeRepository.findByKeyAndLanguage(key, language)).thenReturn(Optional.of(mockedLocale));
//
//        String translation = localizationService.translate(key, language);
//
//        assertEquals(expectedTranslation, translation);
//    }
//
//    @Test
//    void testTranslate_NoTranslationFound() {
//        String key = "testKey";
//        Language language = Language.EN;
//
//        when(localeRepository.findByKeyAndLanguage(key, language)).thenReturn(Optional.empty());
//
//        String translation = localizationService.translate(key, language);
//
//        assertNull(translation);
//    }
//
//    @Test
//    void testFindByValue() {
//        String value = "Test Translation";
//        String expectedKey = "testKey";
//        Locale mockedLocale = new Locale(expectedKey, value, Language.EN);
//
//        when(localeRepository.findByValue(value)).thenReturn(Optional.of(mockedLocale));
//
//        String key = localizationService.findByValue(value);
//
//        assertEquals(expectedKey, key);
//    }
//
//    @Test
//    void testFindByValue_NoLocaleFound() {
//        String value = "Test Translation";
//
//        when(localeRepository.findByValue(value)).thenReturn(Optional.empty());
//
//        String key = localizationService.findByValue(value);
//
//        assertNull(key);
//    }
//}
//
