import com.microsoft.playwright.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.io.File;

public class FinvizCapture {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {

            // 🔥 Actions 환경은 반드시 headless true
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
            );

            // 🔥 화질만 개선 (안전)
            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(2560, 1440)
                    .setDeviceScaleFactor(2.0)
            );

            Page page = context.newPage();
            
            new File("screenshots").mkdirs();
            String date = LocalDate.now().toString();

            // --- Finviz ---
            System.out.println("Finviz 접속 중...");
            page.navigate("https://finviz.com/map.ashx?t=sec");
            page.waitForTimeout(10000);

            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("screenshots/finviz_" + date + ".jpg"))
                .setQuality(100));

            System.out.println("Finviz 캡쳐 완료.");

            // --- TradingView ---
            System.out.println("트레이딩뷰 접속 중...");
            page.navigate("https://www.tradingview.com/chart/?symbol=NASDAQ%3ANDX");
            page.waitForTimeout(15000);

            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("screenshots/tradingview_ndx_" + date + ".jpg"))
                .setQuality(100));

            System.out.println("트레이딩뷰 캡쳐 완료.");

            browser.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
