import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;
import java.nio.file.Paths;
import java.io.File;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class FinvizCapture {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {

            // 1. 시간 설정 및 파일명 생성
            ZonedDateTime nowNY = ZonedDateTime.now(ZoneId.of("America/New_York"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm");
            String now = nowNY.format(formatter);

            String finvizFile = "screenshots/finviz_" + now + ".jpg";
            String tradingviewFile = "screenshots/tradingview_" + now + ".jpg";

            new File("screenshots").mkdirs();

            // 2. 브라우저 실행
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
            );

            // 3. 우회 설정 (헤더 및 언어 설정 추가)
            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .setLocale("ko-KR")
                    .setTimezoneId("America/New_York")
            );

            // 자동화 흔적 제거
            context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined })");

            Page page = context.newPage();

            // ==============================
            // 1️⃣ Finviz 캡쳐 (우회 로직 적용)
            // ==============================
            System.out.println("Finviz 접속 중 (NY Time: " + now + ")...");
            try {
                // LOAD 대신 DOMCONTENTLOADED 사용 (폰트/광고 대기 방지)
                page.navigate("https://finviz.com/map.ashx?t=sec&st=", 
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));
                
                // 최소한의 데이터 로딩을 위해 10초 강제 대기
                page.waitForTimeout(10000); 

                // 팝업 제거 스크립트
                page.evaluate("() => {" +
                    "  const targets = ['.modal-container', '[class*=\"interstitial\"]', '[id*=\"pro-popup\"]', '.overlay'];" +
                    "  targets.forEach(selector => {" +
                    "    document.querySelectorAll(selector).forEach(el => el.remove());" +
                    "  });" +
                    "  document.body.style.filter = 'none';" +
                    "  document.body.style.overflow = 'auto';" +
                    "}");

                // 캡처 실행 (setFullPage를 false로 설정하여 무한 대기 방지)
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(finvizFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(80)
                    .setFullPage(false));
                
                System.out.println("Finviz 캡쳐 완료: " + finvizFile);
            } catch (Exception e) {
                System.out.println("Finviz 실패: " + e.getMessage());
                try {
                    page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshots/error_finviz.jpg")));
                } catch (Exception ignored) {}
            }

            // ==============================
            // 2️⃣ TradingView 캡쳐 (우회 로직 적용)
            // ==============================
            System.out.println("트레이딩뷰 접속 중...");
            try {
                page.navigate("https://www.tradingview.com/chart/?symbol=NASDAQ:NDX&interval=1",
                        new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));
                
                // 차트 로딩을 위해 15초 강제 대기
                page.waitForTimeout(15000);

                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(tradingviewFile))
                    .setType(ScreenshotType.JPEG)
                    .setFullPage(false)); 
                
                System.out.println("트레이딩뷰 캡쳐 완료: " + tradingviewFile);
            } catch (Exception e) {
                System.out.println("트레이딩뷰 실패: " + e.getMessage());
            }

            browser.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
