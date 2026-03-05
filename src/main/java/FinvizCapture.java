import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;
import java.nio.file.Paths;
import java.io.File;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FinvizCapture {
    public static void main(String[] args) {
        Playwright playwright = null;
        Browser browser = null;
        Page page = null;

        try {
            // ==============================
            // 1️⃣ Playwright 생성
            // ==============================
            try {
                playwright = Playwright.create();
                System.out.println("[INFO] Playwright 생성 완료");
            } catch (Exception e) {
                System.out.println("[ERROR] Playwright 생성 실패: " + e.getMessage());
                return;
            }

            // ==============================
            // 2️⃣ 시간 설정 및 파일명 생성
            // ==============================
            String now = "";
            String finvizFile = "";
            String tradingviewFile = "";
            try {
                ZonedDateTime nowNY = ZonedDateTime.now(ZoneId.of("America/New_York"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm");
                now = nowNY.format(formatter);
                finvizFile = "screenshots/finviz_" + now + ".jpg";
                tradingviewFile = "screenshots/tradingview_" + now + ".jpg";
                System.out.println("[INFO] 파일명 생성 완료: " + finvizFile + ", " + tradingviewFile);
            } catch (Exception e) {
                System.out.println("[ERROR] 파일명 생성 실패: " + e.getMessage());
            }

            // ==============================
            // 3️⃣ 폴더 생성
            // ==============================
            try {
                new File("screenshots").mkdirs();
                System.out.println("[INFO] screenshots 폴더 생성 여부: " + new File("screenshots").exists());
            } catch (Exception e) {
                System.out.println("[ERROR] screenshots 폴더 생성 실패: " + e.getMessage());
            }

            // ==============================
            // 4️⃣ 브라우저 실행
            // ==============================
            try {
                browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setArgs(java.util.Arrays.asList(
                            "--disable-blink-features=AutomationControlled",
                            "--disable-web-security",
                            "--allow-running-insecure-content"
                        ))
                );
                System.out.println("[INFO] 브라우저 실행 완료");
            } catch (Exception e) {
                System.out.println("[ERROR] 브라우저 실행 실패: " + e.getMessage());
            }

            // ==============================
            // 5️⃣ 브라우저 컨텍스트 설정
            // ==============================
            BrowserContext context = null;
            try {
                context = browser.newContext(
                    new Browser.NewContextOptions()
                        .setViewportSize(1920, 1080)
                        .setDeviceScaleFactor(1.5)
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                        .setLocale("ko-KR")
                        .setTimezoneId("America/New_York")
                );
                context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined })");
                System.out.println("[INFO] 브라우저 컨텍스트 생성 완료");
            } catch (Exception e) {
                System.out.println("[ERROR] 브라우저 컨텍스트 생성 실패: " + e.getMessage());
            }

            // ==============================
            // 6️⃣ 페이지 생성
            // ==============================
            try {
                page = context.newPage();
                System.out.println("[INFO] 페이지 생성 완료");
            } catch (Exception e) {
                System.out.println("[ERROR] 페이지 생성 실패: " + e.getMessage());
            }

            // ==============================
            // 7️⃣ Finviz 캡쳐
            // ==============================
            try {
                System.out.println("[INFO] Finviz 접속 중...");
                page.navigate("https://finviz.com/map.ashx?t=sec&st=",
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));

                page.waitForTimeout(5000);
                page.evaluate("() => {" +
                    "  const style = document.createElement('style');" +
                    "  style.innerHTML = ` .modal-container, [class*='interstitial'], [id*='pro-popup'], .overlay { display: none !important; } " +
                    "  body, .map-container { filter: none !important; opacity: 1 !important; } `;" +
                    "  document.head.appendChild(style);" +
                    "}");

                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(finvizFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(90)
                    .setFullPage(false));
                System.out.println("[INFO] Finviz 캡쳐 완료: " + finvizFile);
            } catch (Exception e) {
                System.out.println("[ERROR] Finviz 캡쳐 실패: " + e.getMessage());
                try { page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshots/debug_finviz.jpg"))); } catch (Exception ex) { }
            }

            // ==============================
            // 8️⃣ TradingView 캡쳐
            // ==============================
            try {
                System.out.println("[INFO] TradingView 접속 중...");
                page.navigate("https://www.tradingview.com/chart/?symbol=NASDAQ:NDX&interval=1",
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));
                page.waitForTimeout(15000);

                try {
                    Locator btn1D = page.locator("button[data-value='1D'], [data-name='1D']").first();
                    if (btn1D.isVisible()) btn1D.click(new Locator.ClickOptions().setForce(true));
                    else page.locator("span:has-text('1D'), div:has-text('1D')").last().click(new Locator.ClickOptions().setForce(true));
                } catch (Exception e) {
                    for (int i = 0; i < 10; i++) { page.keyboard().press("Control+ArrowDown"); page.waitForTimeout(200); }
                }

                page.waitForTimeout(5000);
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(tradingviewFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(90)
                    .setFullPage(false));
                System.out.println("[INFO] TradingView 캡쳐 완료: " + tradingviewFile);
            } catch (Exception e) {
                System.out.println("[ERROR] TradingView 캡쳐 실패: " + e.getMessage());
                try { page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshots/debug_tradingview.jpg"))); } catch (Exception ex) { }
            }

        } catch (Exception e) {
            System.out.println("[ERROR] 전체 실행 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (browser != null) browser.close(); } catch (Exception e) { }
            try { if (playwright != null) playwright.close(); } catch (Exception e) { }
        }
    }
}
