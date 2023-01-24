package iss.workshop.ca;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class Scraper {
    private String[] imageUrls;

    public Scraper(){}

    public String[] getImageUrls(String url){
        try{
            Document document = Jsoup.connect(url).get();
            Elements images = document.select("img[src~=(?i)\\.(png|jpe?g|gif)]");

            imageUrls = images.stream()
                    .map(image -> image.attr("src"))
                    .toArray(String[]::new);
        }
        catch (IOException ioException){
            ioException.printStackTrace();
            return imageUrls = null;
        }

        return imageUrls;
    }
}
