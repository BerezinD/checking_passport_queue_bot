package com.tommy.AnkaraPassportBot.service.selenium;

import com.tommy.AnkaraPassportBot.model.UserForAnkara;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

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

    public boolean isNameInThePassportPage(UserForAnkara currentUser, WebDriver driver) {
        driver.get(ANKARA_CHECK_PASSPORT_URL);
        return driver.getPageSource().contains(currentUser.getSurname());
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
}
