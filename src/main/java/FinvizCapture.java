import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import java.nio.file.Paths;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FinvizCapture {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {

            // ✅ 파일명용 현재 시간 생성 (공백 제거)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
            String now = LocalDateTime.now().format(formatter);

            String finvizFile = "screenshots/finviz_" + now + ".jpg";
            String tradingviewFile = "screenshots/tradingview_" + now + ".jpg";

            // 1. 브라우저 실행
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false)
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
                """
                Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
                Object.defineProperty(navigator, 'languages', { get: () => ['en-US','en'] });
                Object.defineProperty(navigator, 'plugins', { get: () => [1,2,3] });
                """
            );

            // ✅ 페이지 분리 (중요)
            Page finvizPage = context.newPage();
            Page tradingViewPage = context.newPage();

            // 📁 폴더 생성
            new File("screenshots").mkdirs();

            // ==============================
            // 1️⃣ Finviz 캡쳐
            // ==============================
            System.out.println("Finviz 접속 중...");
            try {
                finvizPage.navigate(
                    "https://finviz.com/map.ashx?t=sec",
                    new Page.NavigateOptions()
                        .setWaitUntil(LoadState.NETWORKIDLE)
                        .setTimeout(60000)
                );

                finvizPage.waitForSelector("#map",
                    new Page.WaitForSelectorOptions().setTimeout(30000));

                finvizPage.waitForTimeout(5000);
                finvizPage.mouse().move(500, 500);
                finvizPage.waitForTimeout(1000);
                finvizPage.mouse().wheel(0, 300);
                finvizPage.waitForTimeout(9000);

                finvizPage.mouse().move(-100, -100);
                finvizPage.keyboard().press("Escape");
                finvizPage.waitForTimeout(500);

                finvizPage.addStyleTag(new Page.AddStyleTagOptions()
                    .setContent("*[class*='tooltip'], *[id*='tooltip'] { display: none !important; }"));

                finvizPage.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(finvizFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(100)
                    .setFullPage(true));

                System.out.println("Finviz 캡쳐 완료: " + finvizFile);

            } catch (Exception e) {
                System.out.println("Finviz 접속 실패, 에러 스샷 저장");

                finvizPage.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get("screenshots/finviz_error.jpg"))
                    .setFullPage(true));
            }

            // ==============================
            // 2️⃣ TradingView 캡쳐
            // ==============================
            System.out.println("트레이딩뷰 접속 중...");
            try {
                // ✅ URL로 1분봉 + 1D 범위 지정
                tradingViewPage.navigate(
                    "https://www.tradingview.com/chart/?symbol=NASDAQ:NDX&interval=1&range=1D",
                    new Page.NavigateOptions().setTimeout(120000)
                );

                tradingViewPage.waitForTimeout(15000);

                // 팝업 제거
                tradingViewPage.addStyleTag(new Page.AddStyleTagOptions()
                    .setContent("""
                        .tv-dialog__close,
                        .js-dialog__close,
                        div[class*='overlap-manager'],
                        [class*='dialog'],
                        [class*='overlay'] {
                            display: none !important;
                        }
                    """));

                tradingViewPage.keyboard().press("Escape");
                tradingViewPage.waitForTimeout(1000);

                // 1D 버튼 클릭 (보조 시도)
                try {
                    Locator btn1D = tradingViewPage
                        .locator("button[data-value='1D'], [data-name='1D']").first();

                    if (btn1D.isVisible()) {
                        btn1D.click(new Locator.ClickOptions().setForce(true));
                        System.out.println("1D 버튼 클릭 성공");
                    }
                } catch (Exception ignore) {}

                tradingViewPage.waitForTimeout(5000);

                tradingViewPage.mouse().move(0, 0);

                // ❗ TradingView는 fullPage 사용 안 함
                tradingViewPage.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(tradingviewFile))
                    .setType(ScreenshotType.JPEG)
                    .setQuality(100));

                System.out.println("트레이딩뷰 캡쳐 완료: " + tradingviewFile);

            } catch (Exception e) {
                System.out.println("트레이딩뷰 캡쳐 중 오류 발생");
                e.printStackTrace();
            }

            browser.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
