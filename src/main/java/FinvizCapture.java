import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import java.nio.file.Paths;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FinvizCapture {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {

            // ✅ 파일명용 현재 시간 생성
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm");
            String now = LocalDateTime.now().format(formatter);

            String finvizFile = "screenshots/finviz_" + now + ".jpg";
            String tradingviewFile = "screenshots/tradingview_" + now + ".jpg";

            // 1. 브라우저 실행
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
            );

            // 2. 브라우저 컨텍스트 설정
            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(2560, 1440)
                    .setDeviceScaleFactor(2.0)
                    .setUserAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                    )
            );

            // 자동화 흔적 제거
            context.addInitScript(
                "Object.defineProperty(navigator, 'webdriver', { get: () -> undefined })"
            );

            Page page = context.newPage();

            // 📁 폴더 생성
            new File("screenshots").mkdirs();

            // ==============================
            // 1️⃣ Finviz 캡쳐
            // ==============================
            System.out.println("Finviz 접속 중...");
            try {
                page.navigate("https://finviz.com/map.ashx?t=sec",
                        new Page.NavigateOptions().setTimeout(60000));

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

                System.out.println("Finviz 캡쳐 완료.");

            } catch (Exception e) {
                System.out.println("Finviz 접속 실패(타임아웃), 하지만 계속 진행합니다.");
            }

            // ==============================
            // 2️⃣ TradingView 캡쳐
            // ==============================
            System.out.println("트레이딩뷰 접속 중...");
            page.navigate("https://www.tradingview.com/chart/ZZfo1QcX/?symbol=NASDAQ%3ANDX");

            page.waitForTimeout(15000);

            // ✅ debug 캡쳐 제거됨
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get(tradingviewFile))
                .setType(ScreenshotType.JPEG)
                .setQuality(100));

            System.out.println("트레이딩뷰 캡쳐 완료.");

            browser.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
