import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import java.nio.file.Paths;
import java.io.File;

public class FinvizCapture {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {

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
                "Object.defineProperty(navigator, 'webdriver', { get: () => undefined })"
            );

            Page page = context.newPage();

            // 📁 폴더 생성
            new File("screenshots").mkdirs();

            // 🔥 기존 파일 삭제 (항상 새 파일 생성되게)
            new File("screenshots/finviz.jpg").delete();
            new File("screenshots/tradingview.jpg").delete();
            new File("screenshots/debug_tradingview.jpg").delete();

            // ==============================
            // 1️⃣ Finviz 캡쳐
            // ==============================
            System.out.println("Finviz 접속 중...");
            try {
                // 타임아웃을 60초로 늘리고, 페이지가 완전히 로드될 때까지 기다리지 않도록 옵션 추가
                page.navigate("https://finviz.com/map.ashx?t=sec", new Page.NavigateOptions().setTimeout(60000));
                
                page.waitForTimeout(5000);
                page.mouse().move(500, 500);
                page.waitForTimeout(1000);
                page.mouse().wheel(0, 300);
                page.waitForTimeout(9000);
    
                // 마우스를 화면 밖으로 이동
                page.mouse().move(-100, -100);
                page.keyboard().press("Escape");
                page.waitForTimeout(500);
    
                // tooltip 강제 제거
                page.addStyleTag(new Page.AddStyleTagOptions()
                    .setContent("*[class*='tooltip'], *[id*='tooltip'] { display: none !important; }"));
    
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get("screenshots/finviz.jpg"))
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

            // 디버그 전체 캡쳐
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("screenshots/debug_tradingview.jpg"))
                .setFullPage(true));

            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("screenshots/tradingview.jpg"))
                .setType(ScreenshotType.JPEG)
                .setQuality(100));

            System.out.println("트레이딩뷰 캡쳐 완료.");

            browser.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
