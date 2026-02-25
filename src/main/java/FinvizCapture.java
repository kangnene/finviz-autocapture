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

            Page page = context.newPage();

            // 📁 폴더 생성
            new File("screenshots").mkdirs();

            // ==============================
            // 1️⃣ Finviz 캡쳐
            // ==============================
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

                System.out.println("Finviz 캡쳐 완료: " + finvizFile);

            } catch (Exception e) {
                System.out.println("Finviz 접속 실패, 그래도 스크린샷 저장 시도");
            
                page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get("screenshots/finviz_error.jpg"))
                    .setFullPage(true));
            }

            // ==============================
            // 2️⃣ TradingView 캡쳐
            // ==============================
            System.out.println("트레이딩뷰 접속 중...");
            try {
                // 1분 봉 차트 접속
                page.navigate("https://www.tradingview.com/chart/?symbol=NASDAQ:NDX&interval=1",
                        new Page.NavigateOptions().setTimeout(120000));
                
                // 1. 초기 로딩 대기
                page.waitForTimeout(15000);

                // 2. 팝업 및 방해 요소 제거 (강력하게 삭제)
                page.addStyleTag(new Page.AddStyleTagOptions()
                    .setContent(".tv-dialog__close, .js-dialog__close, div[class*='overlap-manager'], [class*='dialog'], [class*='overlay'] { display: none !important; }"));
                page.keyboard().press("Escape");
                page.waitForTimeout(1000);

                // 3. '1일' 범위(1D) 클릭 시도 - 3단계 방어
                try {
                    // 전략 A: data-value 속성으로 클릭
                    Locator btn1D = page.locator("button[data-value='1D'], [data-name='1D']").first();
                    if (btn1D.isVisible()) {
                        btn1D.click(new Locator.ClickOptions().setForce(true));
                        System.out.println("1D 버튼 클릭 성공");
                    } else {
                        // 전략 B: 텍스트로 찾아서 클릭
                        page.locator("span:has-text('1D'), div:has-text('1D')").last().click(new Locator.ClickOptions().setForce(true));
                    }
                } catch (Exception e) {
                    System.out.println("버튼 클릭 실패, 단축키 및 축소 모드 실행");
                    // 전략 C: 키보드 단축키 활용 (차트에서 Alt + S + 1 등을 시뮬레이션하거나 축소)
                    for (int i = 0; i < 10; i++) {
                        page.keyboard().press("Control+ArrowDown");
                        page.waitForTimeout(200);
                    }
                }

                // 4. 차트가 정렬될 때까지 대기
                page.waitForTimeout(5000);

                // 5. 마우스 치우기 및 캡쳐
                page.mouse().move(0, 0);
                page.screenshot(new Page.ScreenshotOptions()
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
