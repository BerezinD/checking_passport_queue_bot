package com.tommy.AnkaraPassportBot.service.selenium;

import com.tommy.AnkaraPassportBot.model.UserForAnkara;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tommy.AnkaraPassportBot.AppConstants.ANKARA_BASE_ASPX_URL;
import static com.tommy.AnkaraPassportBot.AppConstants.ANKARA_CHECK_PASSPORT_URL;

@Component
public class SiteAccessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteAccessService.class);

    public void parseAnkaraPage(UserForAnkara user, WebDriver driver) {
        driver.get(ANKARA_BASE_ASPX_URL);
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(2000));

        WebElement inputSurname = driver.findElement(By.name("ctl00$MainContent$txtFam"));
        WebElement inputName = driver.findElement(By.name("ctl00$MainContent$txtIm"));
        WebElement inputPatronymic = driver.findElement(By.name("ctl00$MainContent$txtOt"));
        WebElement inputPhone = driver.findElement(By.name("ctl00$MainContent$txtTel"));
        WebElement inputEmail = driver.findElement(By.name("ctl00$MainContent$txtEmail"));
        WebElement inputBirthYear = driver.findElement(By.name("ctl00$MainContent$TextBox_Year"));
        WebElement selectBirthDay = driver.findElement(By.name("ctl00$MainContent$DDL_Day"));
        WebElement selectBirthMonth = driver.findElement(By.name("ctl00$MainContent$DDL_Month"));
        WebElement inputCaptchaImg = driver.findElement(By.id("ctl00_MainContent_imgSecNum"));
        WebElement inputCaptcha = driver.findElement(By.name("ctl00$MainContent$txtCode"));
        WebElement submitButton = driver.findElement(By.name("ctl00$MainContent$ButtonA"));

        try {
            long userThreshold = new Random().nextLong() % 1000;
            Thread.sleep(5000 + userThreshold);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        inputName.sendKeys(user.getName());
        inputSurname.sendKeys(user.getSurname());
        inputPatronymic.sendKeys(user.getPatronymicName());
        inputPhone.sendKeys(user.getPhone());
        inputEmail.sendKeys(user.getEmail());
        inputBirthYear.sendKeys(String.valueOf(user.getBirthday().getYear()));
        selectBirthMonth.sendKeys(addLeadingZero(user.getBirthday().getMonthValue()));
        selectBirthDay.sendKeys(String.valueOf(user.getBirthday().getDayOfMonth()));
        File captchaScreenshot = inputCaptchaImg.getScreenshotAs(OutputType.FILE);
        String nowTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String solvedCaptcha = null;
        try {
            solvedCaptcha = solveCaptcha(captchaScreenshot);
        } catch (URISyntaxException | TesseractException e) {
            LOGGER.error("Cannot parse captcha.", e);
            throw new RuntimeException(e);
        }
        inputCaptcha.sendKeys(solvedCaptcha);
        LOGGER.info("Captcha solved as " + solvedCaptcha + ". At " + nowTime);
        submitButton.click();
        // TODO this method should be completed as Ankara site has been changed.
        driver.findElement(By.linkText("https://ankara.kdmid.ru/queue/pssp.aspx?nm=PASSPORT"));
    }

    /**
     * Searches for the surname of the user in the Ankara MID passports page.
     * Assumed that only one surname could appear in search - returns a first match.
     *
     * @param currentUser user that needed to be found on a page by surname
     * @param driver      web driver to connect to
     * @return a row with the surname from the site, a null if nothing was found
     */
    public String findNameInThePassportPage(UserForAnkara currentUser, WebDriver driver) {
        driver.get(ANKARA_CHECK_PASSPORT_URL);
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(webDriver -> webDriver.findElement(By.className("tab-accordion__block")));
        clickAndWaitForHeader(driver);
        if (driver.getPageSource().contains(currentUser.getSurname())) {
            WebElement foundElement = driver.findElement(By.xpath("//p[contains(text(), \"" + currentUser.getSurname() + "\")]"));
            return foundElement.getText();
        }
        return null;
    }

    public String findNameInThePassportPage(UserForAnkara currentUser) {
        ArrayList<String> readyPassports = getReadyPassports();
        return readyPassports.stream().filter(passport -> passport.contains(currentUser.getSurname())).findFirst().orElse(null);
    }

    public String findAll5YearsPassports(WebDriver driver) {
        driver.get(ANKARA_CHECK_PASSPORT_URL);
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(webDriver -> webDriver.findElement(By.className("tab-accordion__block")));
        clickAndWaitForHeader(driver);
        return driver.findElement(By.className("tab-accordion__content")).getText();
    }

    /**
     * Find 5 years passports based on regular expression
     *
     * @return surnames who can get passport along with row numbers
     */
    public String findAll5YearsPassports() {
        return String.join("\n", getReadyPassports());
    }

    private ArrayList<String> getReadyPassports() {
        InputStream response;
        try {
            response = getInputStreamFromAnkaraPage();
        } catch (IOException e) {
            LOGGER.error("Cannot get the Ankara page", e);
            throw new RuntimeException(e);
        }
        ArrayList<String> results = new ArrayList<>();
        try (Scanner scanner = new Scanner(response)) {
            String responseBody = scanner.useDelimiter("\\A").next();
            Pattern pattern =
                    Pattern.compile("<div class=\"tab-accordion__content\">(.+?)</div></div></div></div>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(responseBody);
            if (matcher.find()) {
                String all5YearsPassports = matcher.group(1);
                pattern = Pattern.compile("<p>(.+?)</p>", Pattern.DOTALL);
                matcher = pattern.matcher(all5YearsPassports);
                while (matcher.find()) {
                    String parseResult = matcher.group(1);
                    if (isNumeric(parseResult)) {
                        if (matcher.find()) {
                            results.add(parseResult + "  " + matcher.group(1));
                        }
                    }
                }
            }
        }
        return results;
    }

    private static InputStream getInputStreamFromAnkaraPage() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(ANKARA_CHECK_PASSPORT_URL).openConnection();
        connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "PostmanRuntime/7.32.3");
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setUseCaches(false);
        return connection.getInputStream();
    }

    private static void clickAndWaitForHeader(WebDriver driver) {
        WebElement header = driver.findElement(By.className("tab-accordion__title"));
        header.click();
        synchronized (header) {
            try {
                header.wait(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String addLeadingZero(int monthNumber) {
        if (monthNumber >= 10) {
            return String.valueOf(monthNumber);
        }
        return "0" + monthNumber;
    }

    private String solveCaptcha(File captchaPic) throws URISyntaxException, TesseractException {
        Tesseract tesseract = new Tesseract();
        File tessDataFolder = LoadLibs.extractTessResources("tessdata");
        tesseract.setVariable("tessedit_char_whitelist", "0123456789");
        tesseract.setVariable("user_defined_dpi", "200");
        tesseract.setVariable("textord_heavy_nr", "1");
        tesseract.setDatapath(tessDataFolder.getAbsolutePath());
        return tesseract.doOCR(captchaPic);
    }

    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    private String buildStringTableFromMap(Map<Integer, String> resultMap) {
        StringBuilder result = new StringBuilder();
        resultMap.keySet().forEach(key -> {
            result.append(key);
            result.append("  ");
            result.append(resultMap.get(key));
            result.append("/n");
        });
        return result.toString();
    }
}
