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
                new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(java.util.Arrays.asList(
                        "--disable-blink-features=AutomationControlled", // 자동화 흔적 제거
                        "--disable-web-security",
                        "--allow-running-insecure-content"
                    ))
            );

            // 3. 브라우저 컨텍스트 설정 (해상도 향상 및 기타 설정)
            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080)
                    .setDeviceScaleFactor(1.5)  // 해상도를 1.5배로 향상시킴
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .setLocale("ko-KR")
                    .setTimezoneId("America/New_York")
            );

            // 자동화 흔적 제거
            context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined })");

            Page page = context.newPage();

            // ==============================
            // 1️⃣ Finviz 캡쳐 (강화된 우회 로직)
            // ==============================
            System.out.println("Finviz 접속 중...");
            try {
                Response response = page.navigate("https://finviz.com/map.ashx?t=sec&st=", 
                    new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(60000));

                page.waitForTimeout(5000);
                System.out.println("데이터 로딩 대기 중 (20초)...");
                page.waitForTimeout(15000); 

                page.mouse().click(500, 500);
                page.keyboard().press("End");
                page.waitForTimeout(2000);
                page.keyboard().press("Home");

                page.evaluate("() => {" +
                    "  const style = document.createElement('style');" +
                    "  style.innerHTML = ` " +
                    "    .modal-container, [class*='interstitial'], [id*='pro-popup'], .overlay { display: none !important; } " +
                    "    body, .map-container { filter: none !important; opacity: 1 !important; } " +
                    "  `;" +
                    "  document.head.appendChild(style);" +
                    "}");

                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(finvizFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(90)
                    .setFullPage(false));
                
                System.out.println("Finviz 캡쳐 완료!");
            } catch (Exception e) {
                System.out.println("Finviz 실패 원인: " + e.getMessage());
                page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshots/debug_finviz.jpg")));
            }

            // ==============================
            // 2️⃣ TradingView 캡쳐 (우회 로직 적용)
            // ==============================
            System.out.println("트레이딩뷰 접속 중...");
            try {
                page.navigate("https://www.tradingview.com/chart/?symbol=NASDAQ:NDX&interval=1",
                        new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));
                
                page.waitForTimeout(15000);

                try {
                    Locator btn1D = page.locator("button[data-value='1D'], [data-name='1D']").first();
                    if (btn1D.isVisible()) {
                        btn1D.click(new Locator.ClickOptions().setForce(true));
                    } else {
                        page.locator("span:has-text('1D'), div:has-text('1D')").last().click(new Locator.ClickOptions().setForce(true));
                    }
                } catch (Exception e) {
                    for (int i = 0; i < 10; i++) {
                        page.keyboard().press("Control+ArrowDown");
                        page.waitForTimeout(200);
                    }
                }

                page.waitForTimeout(5000); // 1D 데이터가 렌더링되도록 잠시 대기

                // 스크린샷을 캡처 (해상도 1.5배 향상)
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(tradingviewFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(90) // 품질은 90으로 설정하여 고화질을 유지하면서 파일 크기는 줄임
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
