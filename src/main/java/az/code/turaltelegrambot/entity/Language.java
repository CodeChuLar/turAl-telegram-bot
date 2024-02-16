package az.code.turaltelegrambot.entity;

public enum Language {
    AZ,
    EN,
    RU;

    public static Language getByText(String text) {
        for(Language language : values()) {
            if(language.toString().equals(text)) return language;
        }
        return null;
    }
}
