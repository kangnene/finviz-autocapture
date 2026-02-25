import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import java.nio.file.Paths;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FinvizCapture {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm");
            String now = LocalDateTime.now().format(formatter);

            String finvizFile = "screenshots/finviz_" + now + ".jpg";
            String tradingviewFile = "screenshots/tradingview_" + now + ".jpg";

            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
            );

            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(2560, 1440)
                    .setDeviceScaleFactor(2.0)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/122.0")
            );

            context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined })");

            Page page = context.newPage();
            new File("screenshots").mkdirs();

            // ================= FINVIZ =================
            System.out.println("Finviz 접속 중...");
            try {
                page.navigate("https://finviz.com/map.ashx?t=sec");

                // ⭐ 핵심: 요소 뜨면 바로 진행
                page.waitForSelector("#map", new Page.WaitForSelectorOptions().setTimeout(15000));

                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(Paths.get(finvizFile))
                        .setType(ScreenshotType.JPEG)
                        .setQuality(100)
                        .setFullPage(true));

                System.out.println("Finviz 완료");

            } catch (Exception e) {
                System.out.println("Finviz 실패");
            }

            // ================= TRADINGVIEW =================
            System.out.println("TradingView 접속 중...");
            try {
                page.navigate("https://www.tradingview.com/chart/?symbol=NASDAQ:NDX&interval=1");

                // ⭐ 핵심: 차트 canvas 뜨는 순간
                page.waitForSelector("canvas", new Page.WaitForSelectorOptions().setTimeout(20000));

                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(Paths.get(tradingviewFile))
                        .setType(ScreenshotType.JPEG)
                        .setQuality(100));

                System.out.println("TradingView 완료");

            } catch (Exception e) {
                e.printStackTrace();
            }

            browser.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
