import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.LoadState;   // ⭐ 이 줄 추가
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
                    .setUserAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                    )
            );

            context.addInitScript("""
                Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
                Object.defineProperty(navigator, 'languages', { get: () => ['en-US','en'] });
                Object.defineProperty(navigator, 'plugins', { get: () => [1,2,3] });
            """);

            Page page = context.newPage();
            new File("screenshots").mkdirs();

            // ================= FINVIZ =================
            System.out.println("Finviz 접속 중...");
            try {
                page.navigate("https://finviz.com/map.ashx?t=sec",
                        new Page.NavigateOptions()
                                .setWaitUntil(LoadState.NETWORKIDLE)
                                .setTimeout(60000));

                page.waitForSelector("#map", new Page.WaitForSelectorOptions().setTimeout(30000));
                page.waitForTimeout(5000);

                page.mouse().move(500, 500);
                page.waitForTimeout(1000);
                page.mouse().wheel(0, 300);
                page.waitForTimeout(9000);

                page.mouse().move(-100, -100);
                page.keyboard().press("Escape");
                page.waitForTimeout(500);

                page.addStyleTag(new Page.AddStyleTagOptions()
                        .setContent("*[class*='tooltip'], *[id*='tooltip'] { display: none !important; }"));

                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(Paths.get(finvizFile))
                        .setType(ScreenshotType.JPEG)
                        .setQuality(100)
                        .setFullPage(true));

                System.out.println("Finviz 캡쳐 완료");

            } catch (Exception e) {
                System.out.println("Finviz 실패 → 에러 스샷 저장");
                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(Paths.get("screenshots/finviz_error.jpg"))
                        .setFullPage(true));
            }

            // ================= TRADINGVIEW =================
            System.out.println("TradingView 접속 중...");
            try {
                page.navigate("https://www.tradingview.com/chart/?symbol=NASDAQ:NDX&interval=1",
                        new Page.NavigateOptions().setTimeout(120000));

                page.waitForTimeout(15000);

                page.addStyleTag(new Page.AddStyleTagOptions()
                        .setContent(".tv-dialog__close, .js-dialog__close, div[class*='overlap-manager'], [class*='dialog'], [class*='overlay'] { display: none !important; }"));

                page.keyboard().press("Escape");
                page.waitForTimeout(2000);

                page.mouse().move(0, 0);
                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(Paths.get(tradingviewFile))
                        .setType(ScreenshotType.JPEG)
                        .setQuality(100));

                System.out.println("TradingView 캡쳐 완료");

            } catch (Exception e) {
                e.printStackTrace();
            }

            browser.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
